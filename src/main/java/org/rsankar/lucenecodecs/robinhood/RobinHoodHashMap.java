package org.rsankar.lucenecodecs.robinhood;

import java.nio.ByteBuffer;
import java.util.Random;

public class RobinHoodHashMap {

	static final int VALUE_SIZE = 56;
	static final int KEY_SIZE = 8;
	static final int TOTAL_SIZE = VALUE_SIZE + KEY_SIZE + 4;
	static final int DEFAULT_CAPACITY = 32;

	private ByteBuffer buffer;
	private int capacity;

	public RobinHoodHashMap() {
		this(DEFAULT_CAPACITY);
	}

	public RobinHoodHashMap(int capacity) {
		buffer = ByteBuffer.allocateDirect(capacity * TOTAL_SIZE);
		this.capacity = capacity;
	}
	
	public RobinHoodHashMap(byte[] array) {
		this.capacity = array.length / TOTAL_SIZE;
		buffer = ByteBuffer.wrap(array);
	}
	
	public byte[] bufferToByteArray() {
		byte[] arr = new byte[buffer.capacity()];
		buffer.position(0);
		buffer.get(arr);
		return arr;
	}

	public byte[] get(long key) {
		int index = indexFor(Long.hashCode(key), capacity);
		return get(key, index, 0);
	}

	//TODO: switch from recursion to iteration
	private byte[] get(long key, int index, int dist) {
		// Last bit represents empty at index (0) or not (1)
		int distVal = buffer.getInt(index * TOTAL_SIZE + KEY_SIZE + VALUE_SIZE);
		if ((distVal & 1) == 0 || dist > (distVal >> 1))
			return null;
		else if (buffer.getLong(index * TOTAL_SIZE) == key) {
			byte[] pointer = new byte[VALUE_SIZE];
			buffer.position(index * TOTAL_SIZE + KEY_SIZE);
			buffer.get(pointer, 0, VALUE_SIZE); // Copy value into pointer
			return pointer;
		} else {
			return get(key, ((index + 1) % capacity), dist + 1);
		}
	}

	public void put(long key, byte[] pointer) {
		if (pointer.length == VALUE_SIZE) {
			int index = indexFor(Long.hashCode(key), capacity);
			if (isEmpty(index)) {
				buffer.position(index * TOTAL_SIZE);
				buffer.putLong(key);
				buffer.put(pointer);
				buffer.putInt(1);
			} else { // collision - implement robin-hood hashing
				// wraps around when index reaches capacity
				shiftDown(((index + 1) % capacity), key, pointer, 1);
			}
		}
	}

	private void shiftDown(int index, long key, byte[] pointer, int dist) {
		if (isEmpty(index)) {
			buffer.position(index * TOTAL_SIZE);
			buffer.putLong(key);
			buffer.put(pointer);
			buffer.putInt((dist << 1) | 1);
			// last bit represents that there's a value at given index
		} else { // collision - implement robin-hood hashing
			int curDist = buffer
					.getInt(index * TOTAL_SIZE + KEY_SIZE + VALUE_SIZE) >> 1;
			if (curDist > dist) {
				// wraps around when index reaches capacity
				shiftDown(((index + 1) % capacity), key, pointer, dist + 1);
			} else {
				long tmpKey = buffer.getLong(index * TOTAL_SIZE);
				byte[] tmpPointer = new byte[VALUE_SIZE];
				buffer.position(index * TOTAL_SIZE + KEY_SIZE);
				buffer.get(tmpPointer, 0, VALUE_SIZE);

				buffer.position(index * TOTAL_SIZE);
				buffer.putLong(key);
				buffer.put(pointer);
				buffer.putInt((dist << 1) | 1);

				// wraps around when index reaches capacity
				shiftDown(((index + 1) % capacity), tmpKey, tmpPointer,
						curDist + 1);
			}
		}
	}

	/**
	 * Checks whether last bit of last byte of k-v pair of given index is set or
	 * not
	 * 
	 * @return true if last bit is 0, false if last bit is 1
	 */
	private boolean isEmpty(int index) {
		return (buffer.get((index + 1) * TOTAL_SIZE - 1) & 1) == 0;
	}

	/**
	 * 
	 * @param h
	 *            hash code of a key
	 * @param capacity
	 *            total capacity of buffer
	 * @return index based on h, scaled to be within capacity
	 */
	private static int indexFor(int h, int capacity) {
		return (h & (capacity - 1));
	}
	
	public static byte[] getEmptyByteArray(int capacity) {
		return new byte[capacity * TOTAL_SIZE];
	}

	// Unit test
	public static void main(String[] args) {
		RobinHoodHashMap map = new RobinHoodHashMap();
		Random r = new Random();

		byte[] pointer1 = new byte[56];
		r.nextBytes(pointer1);
		byte[] pointer2 = new byte[56];
		r.nextBytes(pointer2);
		byte[] pointer3 = new byte[56];
		r.nextBytes(pointer3);
		byte[] pointer4 = new byte[56];
		r.nextBytes(pointer4);

		map.put(32, pointer1);
		map.put(64, pointer2);
		map.put(0, pointer3);
		map.put(11235, pointer4);

		System.out.println(pointer2[33]);
		System.out.println(map.get(64)[33]);
		System.out.println(map.get(32));
		System.out.println(map.get(1));
	}
}
