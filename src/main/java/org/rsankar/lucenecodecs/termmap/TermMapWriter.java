package org.rsankar.lucenecodecs.termmap;

import java.io.IOException;

import org.apache.lucene.store.IndexOutput;

public interface TermMapWriter {
  void create(int capacity, int hashcodeSizeBytes, int fingerprintSizeBytes, int valueSizeBytes);

  void put(int hashcode, long fingerprint, int value);

  void save(IndexOutput out) throws IOException;
}
