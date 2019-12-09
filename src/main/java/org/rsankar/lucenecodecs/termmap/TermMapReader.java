package org.rsankar.lucenecodecs.termmap;

public interface TermMapReader {
  public void open(int capacity, int hashcodeSizeBytes, int fingerprintSizeBytes,
      int valueSizeBytes, byte[] array);

  int get(int hashcode, long fingerprint);
}