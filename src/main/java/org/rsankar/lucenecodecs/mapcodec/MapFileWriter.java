package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.lucene50.BlockTermStateHelper;
import org.apache.lucene.store.IndexOutput;

public class MapFileWriter {
  IndexOutput out;
  int index;

  public MapFileWriter(IndexOutput out) {
    this.out = out;
    this.index = 0;
  }

  // Returns pointer to location in file where data is stored
  public int saveToFile(BlockTermState bts) throws IOException {
    BlockTermStateHelper.writeToFile(out, bts);
    return index++;
  }

  public void close() throws IOException {
    out.close();
  }
}
