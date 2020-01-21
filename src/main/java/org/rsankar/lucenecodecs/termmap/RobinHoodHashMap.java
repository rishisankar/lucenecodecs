/*
 * Copyright (c) 2020 Rishi Sankar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rsankar.lucenecodecs.termmap;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.lucene.store.IndexOutput;

public class RobinHoodHashMap implements TermMapReader, TermMapWriter {

  private int capacity;

  private int distSizeBytes;
  private int fingerprintSizeBytes;
  private int valueSizeBytes;
  private int totalSizeBytes;

  private ByteBuffer buffer;

  // Saved as dist + fingerprint + value ( last bit of value corresponds to
  // empty-0 or not-1)
  // All sizes in # of bytes
  // hashcode and value size must be less than 4, fingerprint size must be less
  // than 8
  // Hashcode + fingerprint together uniquely identify a key
  public void create(int capacity, int distSizeBytes, int fingerprintSizeBytes,
      int valueSizeBytes) {
    this.capacity = capacity;

    this.distSizeBytes = distSizeBytes;
    this.fingerprintSizeBytes = fingerprintSizeBytes;
    this.valueSizeBytes = valueSizeBytes;
    this.totalSizeBytes = distSizeBytes + fingerprintSizeBytes + valueSizeBytes;

    this.buffer = ByteBuffer.allocateDirect(this.capacity * this.totalSizeBytes);
  }

  public static long iterations = 0;
  public static long calls = 0;

  // Returns int representation of value, -1 if not in map
  public int get(int hashcode, long fingerprint) {
    ++calls;
    if (hashcode < 0 || hashcode >= capacity)
      throw new IllegalArgumentException("Hashcode must be between 0, capacity-1");
    int distVal;
    int dist = 0;
    int index = hashcode;
    while (!isEmpty(index)) {
      ++iterations;
      buffer.position(index * totalSizeBytes);
      distVal = readIntoInt(distSizeBytes);
      int curHashcode = getHashcodeAtIndex(index, distVal);
      long curFingerprint = readIntoLong(fingerprintSizeBytes);
      if (hashcode == curHashcode && fingerprint == curFingerprint) {
        return readIntoInt(valueSizeBytes) >>> 1;
      }
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

  public double[] getAvgDist() {
    double totalDist = 0;
    double minDist = 10000;
    int totalTerms = 0;
    for (int i = 0; i < capacity; ++i) {
      if (isEmpty(i))
        continue;
      totalTerms++;
      buffer.position(i * totalSizeBytes);
      int dist = readIntoInt(distSizeBytes);
      totalDist += dist;
      if (dist < minDist) {
        minDist = dist;
      }
    }
    double[] arr = new double[2];

    arr[0] = totalDist / totalTerms;
    arr[1] = minDist;
    return arr;
  }

  public static int clashcount = 0;

  private void shiftDown(int index, int hashcode, long fingerprint, int value, int dist) {
    for (;;) {
      if (isEmpty(index)) {
        putAllInfoIntoBuffer(index, dist, fingerprint, value);
        return;
      } else { // collision - implement robin-hood hashing
        buffer.position(index * totalSizeBytes);
        int curDist = readIntoInt(distSizeBytes);
        long curFingerprint = readIntoLong(fingerprintSizeBytes);
        if (curDist >= dist) {
          // If duplicate key, overwrite old value (but this shouldn't happen)
          if (hashcode == getHashcodeAtIndex(index, curDist) && fingerprint == curFingerprint) {
            System.out.println("Warning: key already in map");
            putIntoBuffer((value << 1) | 1, valueSizeBytes);
            ++clashcount;
            return;
          }

          // wraps around when index reaches capacity
          index = (index + 1) % capacity;
          dist += 1;
        } else {
          int curValue = readIntoInt(valueSizeBytes) >>> 1;

          putAllInfoIntoBuffer(index, dist, fingerprint, value);

          index = (index + 1) % capacity;
          hashcode = getHashcodeAtIndex(index, curDist);
          fingerprint = curFingerprint;
          value = curValue;
          dist = curDist + 1;
        }
      }
    }
  }

  public int getCapacity() {
    return capacity;
  }

  private void putAllInfoIntoBuffer(int index, int hashcode, long fingerprint, int value) {
    buffer.position(index * totalSizeBytes);
    putIntoBuffer(hashcode, distSizeBytes);
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

  private int getHashcodeAtIndex(int index, int dist) {
    return index >= dist ? index - dist : index - dist + capacity;
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
    out.writeVInt(capacity);
    out.writeVInt(distSizeBytes);
    out.writeVInt(fingerprintSizeBytes);
    out.writeVInt(valueSizeBytes);
    buffer.position(0);
    for (int i = 0; i < buffer.capacity(); ++i) {
      out.writeByte(buffer.get());
    }
  }

  public void open(int capacity, int distSizeBytes, int fingerprintSizeBytes, int valueSizeBytes,
      byte[] array) {
    this.capacity = capacity;
    this.distSizeBytes = distSizeBytes;
    this.fingerprintSizeBytes = fingerprintSizeBytes;
    this.valueSizeBytes = valueSizeBytes;
    this.totalSizeBytes = distSizeBytes + fingerprintSizeBytes + valueSizeBytes;
    this.buffer = ByteBuffer.wrap(array);
  }

}
