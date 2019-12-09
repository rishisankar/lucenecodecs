package org.rsankar.lucenecodecs.termmap;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.lucene.store.IndexOutput;

public class RobinHoodHashMap implements TermMapReader, TermMapWriter {

  private int capacity;

  private int hashcodeSizeBytes;
  private int fingerprintSizeBytes;
  private int valueSizeBytes;
  private int totalSizeBytes;

  private ByteBuffer buffer;

  // Saved as hashcode + fingerprint + value ( last bit of value corresponds to
  // empty-0 or not-1)
  // All sizes in # of bytes
  // hashcode and value size must be less than 4, fingerprint size must be less
  // than 8
  // Hashcode + fingerprint together uniquely identify a key
  public void create(int capacity, int hashcodeSizeBytes, int fingerprintSizeBytes,
      int valueSizeBytes) {
    this.capacity = capacity;

    this.hashcodeSizeBytes = hashcodeSizeBytes;
    this.fingerprintSizeBytes = fingerprintSizeBytes;
    this.valueSizeBytes = valueSizeBytes;
    this.totalSizeBytes = hashcodeSizeBytes + fingerprintSizeBytes + valueSizeBytes;

    this.buffer = ByteBuffer.allocateDirect(this.capacity * this.totalSizeBytes);
  }

  // Returns int representation of value, -1 if not in map
  public int get(int hashcode, long fingerprint) {
    if (hashcode < 0 || hashcode >= capacity)
      throw new IllegalArgumentException("Hashcode must be between 0, capacity-1");
    int distVal;
    int dist = 0;
    int index = hashcode;
    while (!isEmpty(index)) {
      buffer.position(index * totalSizeBytes);
      int curHashcode = readIntoInt(hashcodeSizeBytes);
      long curFingerprint = readIntoLong(fingerprintSizeBytes);
      if (hashcode == curHashcode && fingerprint == curFingerprint)
        return (readIntoInt(valueSizeBytes) >>> 1);

      distVal = getDistAtIndex(index, curHashcode);
      if (dist > distVal)
        return -1;

      ++index;
      index %= capacity;
      ++dist;
    }
    return -1;
  }

  public void put(int hashcode, long fingerprint, int value) {
    if (hashcode < 0 || hashcode >= capacity)
      throw new IllegalArgumentException("Hashcode must be between 0, capacity-1");

    // wraps around when index reaches capacity
    shiftDown(hashcode, hashcode, fingerprint, value, 0);

  }

  private void shiftDown(int index, int hashcode, long fingerprint, int value, int dist) {
    if (isEmpty(index)) {
      putAllInfoIntoBuffer(index, hashcode, fingerprint, value);
    } else { // collision - implement robin-hood hashing
      buffer.position(index * totalSizeBytes);
      int curHashcode = readIntoInt(hashcodeSizeBytes);
      long curFingerprint = readIntoLong(fingerprintSizeBytes);
      int curDist = getDistAtIndex(index, curHashcode);
      if (curDist >= dist) {
        // If duplicate key, overwrite old value (but this shouldn't happen)
        if (hashcode == curHashcode && fingerprint == curFingerprint) {
          System.out.println("Warning: key already in map");
          putIntoBuffer((value << 1) | 1, valueSizeBytes);
          return;
        }

        // wraps around when index reaches capacity
        shiftDown(((index + 1) % capacity), hashcode, fingerprint, value, dist + 1);
      } else {
        int curValue = readIntoInt(valueSizeBytes) >>> 1;

        putAllInfoIntoBuffer(index, hashcode, fingerprint, value);

        // wraps around when index reaches capacity
        shiftDown(((index + 1) % capacity), curHashcode, curFingerprint, curValue, curDist + 1);
      }
    }
  }

  public int getCapacity() {
    return capacity;
  }

  private void putAllInfoIntoBuffer(int index, int hashcode, long fingerprint, int value) {
    buffer.position(index * totalSizeBytes);
    putIntoBuffer(hashcode, hashcodeSizeBytes);
    putIntoBuffer(fingerprint, fingerprintSizeBytes);
    putIntoBuffer((value << 1) | 1, valueSizeBytes);
    buffer.position((index * totalSizeBytes));
  }

  private void putIntoBuffer(int val, int numBytes) {
    for (int i = numBytes; --i >= 0;) {
      buffer.put((byte) (val >>> (i * 8)));
    }
  }

  private void putIntoBuffer(long val, int numBytes) {
    for (int i = numBytes; --i >= 0;) {
      buffer.put((byte) (val >>> (i * 8)));
    }
  }

  private int getDistAtIndex(int index, int curHashcode) {
    return index >= curHashcode ? index - curHashcode : index - curHashcode + capacity;
  }

  private int readIntoInt(int numBytes) {
    int result = (buffer.get() & 0xff);
    for (int i = 1; i < numBytes; ++i) {
      result <<= 8;
      result |= (buffer.get() & 0xff);
    }
    return result;
  }

  private long readIntoLong(int numBytes) {
    long result = (buffer.get() & 0xff);
    for (int i = 1; i < numBytes; ++i) {
      result <<= 8;
      result |= (buffer.get() & 0xff);
    }
    return result;
  }

  /**
   * Checks whether last bit of last byte of k-v pair of given index is set or not
   * 
   * @return true if last bit is 0, false if last bit is 1
   */
  private boolean isEmpty(int index) {
    return (buffer.get((index + 1) * this.totalSizeBytes - 1) & 1) == 0;
  }

  public void save(IndexOutput out) throws IOException {
    out.writeInt(capacity);
    out.writeInt(hashcodeSizeBytes);
    out.writeInt(fingerprintSizeBytes);
    out.writeInt(valueSizeBytes);
    buffer.position(0);
    for (int i = 0; i < buffer.capacity(); ++i) {
      out.writeByte(buffer.get());
    }
  }

  public void open(int capacity, int hashcodeSizeBytes, int fingerprintSizeBytes,
      int valueSizeBytes, byte[] array) {
    this.capacity = capacity;
    this.hashcodeSizeBytes = hashcodeSizeBytes;
    this.fingerprintSizeBytes = fingerprintSizeBytes;
    this.valueSizeBytes = valueSizeBytes;
    this.totalSizeBytes = hashcodeSizeBytes + fingerprintSizeBytes + valueSizeBytes;
    this.buffer = ByteBuffer.wrap(array);
  }

}
