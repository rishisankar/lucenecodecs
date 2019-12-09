package org.apache.lucene.codecs.lucene50;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RandomAccessInput;

public class BlockTermStateHelper {

  public static void writeToFile(IndexOutput out, BlockTermState bts) throws IOException {
    Lucene50PostingsFormat.IntBlockTermState ibts = (Lucene50PostingsFormat.IntBlockTermState) bts;
    out.writeInt(ibts.docFreq);
    out.writeLong(ibts.totalTermFreq);
    out.writeLong(ibts.docStartFP);
    out.writeLong(ibts.skipOffset);
    out.writeInt(ibts.singletonDocID);
    out.writeLong(ibts.posStartFP);
    out.writeLong(ibts.payStartFP);
    out.writeLong(ibts.lastPosBlockOffset);
  }

  public static BlockTermState readFromFile(IndexInput in, int index) throws IOException {
    RandomAccessInput slice = in.randomAccessSlice(index * 56, 56);
    Lucene50PostingsFormat.IntBlockTermState ibts = new Lucene50PostingsFormat.IntBlockTermState();

    ibts.docFreq = slice.readInt(0);
    ibts.totalTermFreq = slice.readLong(4);
    ibts.docStartFP = slice.readLong(12);
    ibts.skipOffset = slice.readLong(20);
    ibts.singletonDocID = slice.readInt(28);
    ibts.posStartFP = slice.readLong(32);
    ibts.payStartFP = slice.readLong(40);
    ibts.lastPosBlockOffset = slice.readLong(48);

    return ibts;
  }
}
