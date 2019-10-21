package org.apache.lucene.codecs.lucene50;

import java.nio.ByteBuffer;

import org.apache.lucene.codecs.BlockTermState;

public class BlockTermStateHelper {
//test
  public static BlockTermState buildIntBlockTermState(byte[] pointer) {
    Lucene50PostingsFormat.IntBlockTermState ibts = new Lucene50PostingsFormat.IntBlockTermState();

    ByteBuffer b = ByteBuffer.wrap(pointer);

    ibts.docFreq = b.getInt();
    ibts.totalTermFreq = b.getLong();
    ibts.docStartFP = b.getLong();
    ibts.skipOffset = b.getLong();
    ibts.singletonDocID = b.getInt();
    ibts.posStartFP = b.getLong();
    ibts.payStartFP = b.getLong();
    ibts.lastPosBlockOffset = b.getLong();

    return ibts;
  }

  public static ByteBuffer buildBuffer(long key, BlockTermState bts) {
    ByteBuffer b = ByteBuffer.allocateDirect(56);
    Lucene50PostingsFormat.IntBlockTermState ibts = (Lucene50PostingsFormat.IntBlockTermState) bts;
    b.putInt(ibts.docFreq);
    b.putLong(ibts.totalTermFreq);
    b.putLong(ibts.docStartFP);
    b.putLong(ibts.skipOffset);
    b.putInt(ibts.singletonDocID);
    b.putLong(ibts.posStartFP);
    b.putLong(ibts.payStartFP);
    b.putLong(ibts.lastPosBlockOffset);
    return b;
  }
}
