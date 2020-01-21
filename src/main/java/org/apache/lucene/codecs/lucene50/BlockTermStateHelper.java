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

package org.apache.lucene.codecs.lucene50;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsFormat.IntBlockTermState;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

public class BlockTermStateHelper {
  public static void writeToFile(IndexOutput out, BlockTermState bts) throws IOException {
    Lucene50PostingsFormat.IntBlockTermState ibts = (Lucene50PostingsFormat.IntBlockTermState) bts;

    /*
     * If docFreq=1: 1-5 bytes: [singletonDocIDSizeBytes] [singletonDocID]... Else:
     * First byte 0 0 0 [docFreqSizeBytes] [docStartFPSizeBytes]
     */

    if (ibts.docFreq == 1) {
      int singletonDocIDSizeBytes = numBytes(ibts.singletonDocID << 2); // 2 bits
      byte first = (byte) (singletonDocIDSizeBytes << 6);
      first |= (ibts.singletonDocID >> ((singletonDocIDSizeBytes - 1) * 8)) & 0x3f;
      out.writeByte(first);
      for (int i = (singletonDocIDSizeBytes - 2) * 8; i >= 0; i -= 8) {
        out.writeByte((byte) (ibts.singletonDocID >> i));
      }
    } else {
      int docFreqSizeBytes = numBytes(ibts.docFreq); // max 2 bits
      int docStartFPSizeBytes = numBytes(ibts.docStartFP); // max 3 bits
      // singletonDocID will be -1 here

      byte first = (byte) docFreqSizeBytes;
      first <<= 3;
      first |= (byte) docStartFPSizeBytes;
      out.writeByte(first);

      for (int i = docFreqSizeBytes - 1; i >= 0; --i) {
        out.writeByte((byte) (ibts.docFreq >> (8 * i)));
      }
      for (int i = docStartFPSizeBytes - 1; i >= 0; --i) {
        out.writeByte((byte) (ibts.docStartFP >> (8 * i)));
      }
    }
  }

  public static BlockTermState readFromFile(IndexInput in, int index) throws IOException {
    Lucene50PostingsFormat.IntBlockTermState ibts = new Lucene50PostingsFormat.IntBlockTermState();

    in.seek(index);
    byte first = in.readByte();
    int singletonDocIDSizeBytes;

    if (first == 0) {
      ibts.docFreq = 1;
      ibts.docStartFP = -1;
      ibts.singletonDocID = 0;
    } else if ((singletonDocIDSizeBytes = (first & 0xff) >> 6) != 0) {
      ibts.docFreq = 1;
      ibts.docStartFP = -1;
      ibts.singletonDocID = first & 0x3f;
      for (int i = 0; i < singletonDocIDSizeBytes - 1; ++i) {
        ibts.singletonDocID <<= 8;
        ibts.singletonDocID |= (in.readByte() & 0xff);
      }
    } else {
      ibts.singletonDocID = -1;
      int docFreqSizeBytes = first >> 3;
      int docStartFPSizeBytes = first & 0x07;
      ibts.docFreq = 0;
      for (int i = 0; i < docFreqSizeBytes; ++i) {
        ibts.docFreq <<= 8;
        ibts.docFreq |= (in.readByte() & 0xff);
      }
      ibts.docStartFP = 0;
      for (int i = 0; i < docStartFPSizeBytes; ++i) {
        ibts.docStartFP <<= 8;
        ibts.docStartFP |= (in.readByte() & 0xff);
      }
    }

    ibts.totalTermFreq = -1;
    ibts.skipOffset = -1;
    ibts.posStartFP = 0;
    ibts.payStartFP = 0;
    ibts.lastPosBlockOffset = -1;

    return ibts;
  }

  private static int numBytes(int i) {
    if (i == 0)
      return 0;
    else
      return 1 + numBytes(i >> 8);
  }

  private static int numBytes(long l) {
    if (l == 0)
      return 0;
    else
      return 1 + numBytes(l >> 8);
  }

  public static void printBTS(BlockTermState bts) {
    Lucene50PostingsFormat.IntBlockTermState ibts = (IntBlockTermState) bts;
    System.out.println("BTS: docFreq: " + ibts.docFreq + ", totalTermFreq: " + ibts.totalTermFreq
        + ", docStartFP: " + ibts.docStartFP + ", skipOffset: " + ibts.skipOffset
        + ", singletonDocID: " + ibts.singletonDocID + ", posStartFP: " + ibts.posStartFP
        + ", payStartFP: " + ibts.payStartFP + ", lastPosBlockOffset: " + ibts.lastPosBlockOffset);
  }

}
