package org.rsankar.lucenecodecs.termmap;

import static org.hamcrest.core.Is.is;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class TestRobinHoodHashMap {
  final static int HASHCODE_SIZE_BYTES = 4;
  final static int FINGERPRINT_SIZE_BYTES = 8;
  final static int VALUE_SIZE_BYTES = 2;

  @Test
  public void testRHHM() {
    long keys[] = { 0, 32, 2, 64, 96, 128, 1, 33, 34, 1000, 1001, 1002, 1003 };
    int values[] = new int[keys.length];
    Random r = new Random();
    for (int i = 0; i < keys.length; ++i) {
      values[i] = r.nextInt(keys.length);
    }
    RobinHoodHashMap map = new RobinHoodHashMap();
    map.create(32, HASHCODE_SIZE_BYTES, FINGERPRINT_SIZE_BYTES, VALUE_SIZE_BYTES);

    RobinHoodHashMap fullmap = new RobinHoodHashMap();
    fullmap.create(keys.length, HASHCODE_SIZE_BYTES, FINGERPRINT_SIZE_BYTES, VALUE_SIZE_BYTES);

    testKeysAndValues(map, keys, values);
    Assert.assertThat(map.get(1, 3), is(-1));
    Assert.assertThat(map.get(2, 2), is(-1));
    Assert.assertThat(map.get(3, 0), is(-1));

    testKeysAndValues(fullmap, keys, values);
    Assert.assertThat(fullmap.get(1, 3), is(-1));
    Assert.assertThat(fullmap.get(2, 2), is(-1));
    Assert.assertThat(fullmap.get(3, 0), is(-1));

    testRHHMWithParams(255, 200);
    testRHHMWithParams(300, 250);
    testRHHMWithParams(256, 200);
    testRHHMWithParams(127, 90);
    testRHHMWithParams(128, 90);
    testRHHMWithParams(1000, 800);
  }

  public void testRHHMWithParams(int capacity, int numKeys) {
    long keys[] = new long[numKeys];
    int values[] = new int[numKeys];
    Random r = new Random();

    for (int i = 0; i < numKeys; ++i) {
      keys[i] = r.nextLong();
      values[i] = r.nextInt(capacity);
    }

    RobinHoodHashMap map = new RobinHoodHashMap();
    map.create(capacity, HASHCODE_SIZE_BYTES, FINGERPRINT_SIZE_BYTES, VALUE_SIZE_BYTES);

    testKeysAndValues(map, keys, values);
  }

  public void testKeysAndValues(RobinHoodHashMap map, long[] keys, int[] values) {
    int capacity = map.getCapacity();

    for (int i = 0; i < keys.length; ++i) {
      int hash = getHashcode(keys[i], capacity);
      long fingerprint = getFingerprint(keys[i], capacity);

      map.put(hash, fingerprint, values[i]);
    }

    int hashcode1 = getHashcode(keys[1], capacity);
    long fingerprint1 = getFingerprint(keys[1], capacity);
    Assert.assertThat(map.get(hashcode1, fingerprint1), is(values[1]));
    // Replace key at index 1's value with 12
    map.put(hashcode1, fingerprint1, 12);
    values[1] = 12;

    for (int i = 0; i < keys.length; ++i) {
      int hash = getHashcode(keys[i], capacity);
      long fingerprint = getFingerprint(keys[i], capacity);
      Assert.assertThat(map.get(hash, fingerprint), is(values[i]));
    }
  }

  static int getHashcode(long key, int capacity) {
    return (int) Math.abs(key % capacity);
  }

  static long getFingerprint(long key, int capacity) {
    return key / capacity + 1000000;
  }
}