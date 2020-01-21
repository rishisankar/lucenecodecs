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

package org.rsankar.lucenecodecs.mapcodec;

class Primes {

  // An iterator that returns a number that is nearly prime - will return all
  // prime numbers,
  // but will return a few additional numbers also.
  static class NearlyPrimeIterator {
    static final int N = 10;
    static final int[] FIRST_N_PRIMES = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29 };

    int firstNIndex = 0;
    int lastNearPrime = FIRST_N_PRIMES[N - 1];

    int nextNearlyPrime() {
      if (firstNIndex < N) {
        return FIRST_N_PRIMES[firstNIndex++];
      }
      OuterLoop: for (;;) {
        lastNearPrime += 2;
        for (int i = 1; i < N; ++i) {
          if (lastNearPrime % FIRST_N_PRIMES[i] == 0) {
            continue OuterLoop;
          }
        }
        return lastNearPrime;
      }
    }
  }

  static int[] next20Primes(int n) {
    // We look for primes from n to n + RANGE - 1. For n values less than 2^31, a
    // range of 1000
    // is more than sufficient.
    final int RANGE = 1000;
    // The prime status of numbers from n to n + RANGE - 1. If it remains false at
    // the end of
    // processing, it is a prime.
    boolean[] sieve = new boolean[RANGE];
    int maxDivisor = (int) (Math.sqrt(n + RANGE));
    NearlyPrimeIterator iter = new NearlyPrimeIterator();
    for (;;) {
      int divisor = iter.nextNearlyPrime();
      if (divisor > maxDivisor) {
        break;
      }
      // multiple starts off as the smallest number greater than or equal to n that is
      // divisible
      // by divisor
      int multiple = Math.max(n / divisor, 2) * divisor;
      if (multiple < n) {
        multiple += divisor;
      }
      while (multiple < n + RANGE) {
        sieve[multiple - n] = true;
        multiple += divisor;
      }
    }
    int[] result = new int[20];
    int index = -1;
    for (int i = 0; i < 20; ++i) {
      while (sieve[++index]) {
      }
      result[i] = n + index;
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    int[] primes = next20Primes(1000000);
    for (int p : primes) {
      System.out.println(p);
    }
  }
}
