package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;

import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsReader;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsWriter;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.BytesRef;

public class MapPostingsFormat extends PostingsFormat {
  public static final String FIELD_MAP_EXTENSION = "fme";
  public static final String FIELD_DATA_EXTENSION = "fde";

  public static final int EXTRA_SPACE_PERCENT = 20;
  public static final int FINGERPRINT_SIZE_BYTES = 6;

  public MapPostingsFormat() {
    super("MapPostingsFormat");
  }

  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    PostingsWriterBase pw = new Lucene50PostingsWriter(state);
    return new MapFieldsWriter(state, pw);
  }

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase pr = new Lucene50PostingsReader(state);
    return new MapFieldsReader(state, pr);
  }

  static String getFieldMapFileName(String segmentName, String segmentSuffix) {
    return segmentName + "_" + segmentSuffix + "." + FIELD_MAP_EXTENSION;
  }

  static String getFieldDataFileName(String segmentName, String segmentSuffix) {
    return segmentName + "_" + segmentSuffix + "." + FIELD_DATA_EXTENSION;
  }

  static int deriveHashcodeSizeFromCapacity(int capacity) {
    if (capacity == 0)
      return 1;
    else
      return 1 + deriveHashcodeSizeFromCapacity(capacity >> 8);
  }

  static int deriveValueSizeFromCapacity(int capacity) {
    return deriveHashcodeSizeFromCapacity(capacity << 1);
  }

  static int getHashcode(long key, int capacity) {
    return (int) (key >= 0 ? key % capacity : -key % capacity);
  }

  static long getFingerprint(long key, int capacity) {
    if (key >= 0)
      return (key / capacity) << 1;
    else
      return (Math.abs(key) / capacity) << 1 | 1;
  }
  
  static long getKey(BytesRef text) {
    return Long.valueOf(text.utf8ToString().hashCode());
  }
}
