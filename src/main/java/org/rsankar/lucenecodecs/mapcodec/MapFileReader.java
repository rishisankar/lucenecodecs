package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.lucene50.BlockTermStateHelper;
import org.apache.lucene.store.IndexInput;

public class MapFileReader {
  IndexInput in;

  public MapFileReader(IndexInput in) {
    this.in = in;
  }

  // Returns BlockTermState object encoded at given index in the file
  public BlockTermState read(int index) throws IOException {
    return BlockTermStateHelper.readFromFile(in, index);
  }
}
