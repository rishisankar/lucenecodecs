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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class ParameterAnalyzer {
  public static final int EXTRA_SPACE_PERCENT = 50;

  int k;
  int capacity;
  int termCount;

  public ParameterAnalyzer(Terms terms, MapPostingsFormat postingsFormat) throws IOException {

    long startTime = System.nanoTime();
    System.out.println("Starting parameter analysis (may take a while)...");

    this.termCount = countTerms(terms);

    int minCapacity = (int) (termCount * ((100 + EXTRA_SPACE_PERCENT) / 100.0));
    int[] pChoices = Primes.next20Primes(minCapacity);
    int[] kChoices = { 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140 };

    int[] hash = new int[pChoices[pChoices.length - 1]];
    long[] fp = new long[termCount];

    TermsEnum te = terms.iterator();
    for (int i = 0; i < termCount; ++i) {
      fp[i] = postingsFormat.getFingerprint(te.next());
    }

    int bestP = 0;
    int bestK = 0;
    double bestAvgHistDist = 0;

    int count = 1;
    int totalPairs = pChoices.length * kChoices.length;
    for (int p : pChoices) {
      for (int k : kChoices) {
        double val = analyzeParameters(p, k, hash, fp, count, totalPairs);
        count++;
        if (val == -1) {
          continue;
        } else {
          if (bestP == 0) {
            bestP = p;
            bestK = k;
            bestAvgHistDist = val;
          } else {
            if (val < bestAvgHistDist) {
              bestP = p;
              bestK = k;
              bestAvgHistDist = val;
            }
          }
        }
      }
    }

    this.capacity = bestP;
    this.k = bestK;

    if (bestP == 0) {
      throw new RuntimeException("Error running parameter analyzer: no (p,k) pair found.");
    }
    long elapsed = System.nanoTime() - startTime;
    System.out.println("Parameter analysis finished in " + elapsed / 1000000 + "ms. k=" + k
        + ", capacity=" + capacity);

    /*
     * Potential parameters (1672501, 128) or (1500269,116);
     */
  }

  // Conditions: lastByteCollision = 0, maxHitDist < 15
  // If conditions not met, return -1, else return avgHitDist
  public double analyzeParameters(int p, int k, int[] hash, long[] fp, int count, int totalPairs) {
    double avgHitDist; // of steps for search on avg
    int maxHitDist;

    for (int i = 0; i < p; ++i) {
      hash[i] = 0;
    }
    for (int i = 0; i < fp.length; ++i) {
      int hashVal = getHashcode(fp[i], k, p);
      int index = hashVal;
      for (;;) {
        if (hash[index] == 0) {
          hash[index] = hashVal;
          break;
        }
        int dist = index - hashVal;
        if (dist < 0) {
          dist += p;
        }
        int existingDist = index - hash[index];
        if (existingDist < 0) {
          existingDist += p;
        }
        if (existingDist < dist) {
          int existingHashVal = hash[index];
          hash[index] = hashVal;
          hashVal = existingHashVal;
        }
        index = (index + 1) % p;
      }
    }
    double totalHitDist = 0.0;
    maxHitDist = 0;
    int hitCount = 0;
    for (int i = 0; i < p; ++i) {
      if (hash[i] != 0) {
        int hitDist = i - hash[i];
        if (hitDist < 0) {
          hitDist += p;
        }
        totalHitDist += hitDist;
        if (hitDist > maxHitDist) {
          maxHitDist = hitDist;
        }
        ++hitCount;
      }
    }
    avgHitDist = totalHitDist / hitCount;

    System.out.println(count + "/" + totalPairs + ": (" + p + "," + k + ") - maxHitDist: "
        + maxHitDist + ", avgHitDist: " + avgHitDist + ", lastByteCollisions: "
        + countLastByteCollisions(p, k, hash, fp));
    if (maxHitDist < 15 && countLastByteCollisions(p, k, hash, fp) == 0) {
      return avgHitDist;
    } else {
      return -1;
    }
  }

  public int countLastByteCollisions(int p, int k, int[] hash, long[] fp) {
    for (int i = 0; i < p; ++i) {
      hash[i] = 0;
    }
    Map<Integer, List<Long>> reverseHash = new HashMap<>();
    for (int i = 0; i < fp.length; ++i) {
      int hashVal = getHashcode(fp[i], k, p);
      ++hash[hashVal];
      List<Long> rhList = reverseHash.get(hashVal);
      if (rhList == null) {
        rhList = new ArrayList<>();
        reverseHash.put(hashVal, rhList);
      }
      rhList.add(fp[i]);
    }
    int lastByteCollisions = 0;
    for (int i = 0; i < p; ++i) {
      if (hash[i] >= 2) {
        // There is a collision
        List<Long> rhList = reverseHash.get(i);
        Set<Long> s = new HashSet<>();
        for (long l : rhList) {
          // Add last byte to a set
          s.add(l & 0xffL);
        }
        if (s.size() < rhList.size()) {
          // Set is smaller than list - so last byte was same for multiple entries
          ++lastByteCollisions;
        }
      }
    }
    return lastByteCollisions;
  }

  public static int getHashcode(long fingerprint, int k, int capacity) {
    return Math.abs((int) ((fingerprint * k) % capacity));
  }

  public int getHashcode(long fingerprint) {
    return getHashcode(fingerprint, k, capacity);
  }

  private static int countTerms(Terms t) throws IOException {
    TermsEnum te = t.iterator();
    int count = 0;
    while (true) {
      BytesRef term = te.next();
      if (term == null) {
        break;
      }
      ++count;
    }

    return count;
  }

}
