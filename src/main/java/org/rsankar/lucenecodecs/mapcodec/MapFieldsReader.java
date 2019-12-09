package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.index.BaseTermsEnum;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.ImpactsEnum;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SlowImpactsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.BytesRef;
import org.rsankar.lucenecodecs.termmap.RobinHoodHashMap;

public class MapFieldsReader extends FieldsProducer {

  SegmentReadState state;
  PostingsReaderBase reader;
  String segmentName;

  private final Map<String, MapTerms> termsCache = new HashMap<String, MapTerms>();

  public MapFieldsReader(SegmentReadState state, PostingsReaderBase reader) {
    this.state = state;
    this.reader = reader;
    this.segmentName = state.segmentInfo.name;
  }

  @Override
  public Terms terms(String field) throws IOException {

    MapTerms terms = termsCache.get(field);
    if (terms == null) {
      String fileName = MapPostingsFormat.getFieldMapFileName(segmentName, state.segmentSuffix);
      try {
        IndexInput in = this.state.directory.openInput(fileName, state.context);
        terms = new MapTerms(field, in);
        termsCache.put(field, terms);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }
    return terms;

  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Override
  public Iterator<String> iterator() {
    // TODO
    return null;
  }

  @Override
  public long ramBytesUsed() {
    throw new RuntimeException("ramBytesUsed() called in MapFieldsReader");
  }

  @Override
  public void checkIntegrity() throws IOException {
    throw new RuntimeException("checkIntegrity() called in MapFieldsReader");
  }

  @Override
  public int size() {
    throw new RuntimeException("size() called in MapFieldsReader");
  }

  private class MapTerms extends Terms {
    private FieldInfo fieldInfo;

    private int docCount;
    private int termCount;
    private long sumTotalTermFreq;
    private long sumDocFreq;

    private RobinHoodHashMap map;

    private MapTerms(String field, IndexInput in) throws IOException {
      this.fieldInfo = state.fieldInfos.fieldInfo(field);

      this.docCount = in.readInt();
      this.termCount = in.readInt();
      this.sumTotalTermFreq = in.readLong();
      this.sumDocFreq = in.readLong();

      int capacity = in.readInt();
      int hashcodeSizeBytes = in.readInt();
      int fingerprintSizeBytes = in.readInt();
      int valueSizeBytes = in.readInt();

      // TODO: Switch to off-heap ByteBuffer
      byte arr[] = new byte[capacity * (hashcodeSizeBytes + fingerprintSizeBytes + valueSizeBytes)];
      in.readBytes(arr, 0, arr.length);
      this.map = new RobinHoodHashMap();
      map.open(capacity, hashcodeSizeBytes, fingerprintSizeBytes, valueSizeBytes, arr);

    }

    @Override
    public TermsEnum iterator() throws IOException {
      if (map != null) {
        return new MapTermsEnum(fieldInfo, map);
      } else {
        return TermsEnum.EMPTY;
      }
    }

    @Override
    public long size() throws IOException {
      return (long) termCount;
    }

    @Override
    public int getDocCount() throws IOException {
      return docCount;
    }

    @Override
    public long getSumTotalTermFreq() throws IOException {
      return sumTotalTermFreq;
    }

    @Override
    public long getSumDocFreq() throws IOException {
      return sumDocFreq;
    }

    @Override
    public boolean hasFreqs() {
      return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
    }

    @Override
    public boolean hasOffsets() {
      return fieldInfo.getIndexOptions()
          .compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    }

    @Override
    public boolean hasPositions() {
      return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    }

    @Override
    public boolean hasPayloads() {
      return fieldInfo.hasPayloads();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(terms=" + termCount + ",postings=" + sumDocFreq
          + ",positions=" + sumTotalTermFreq + ",docs=" + docCount + ")";
    }
  }

  private class MapTermsEnum extends BaseTermsEnum {
    private FieldInfo fieldInfo;
    private final IndexOptions indexOptions;

    private RobinHoodHashMap map;
    private MapFileReader mfr;

    private BlockTermState currentState;
    private BytesRef currentTerm;

    private MapTermsEnum(FieldInfo fieldInfo, RobinHoodHashMap map) throws IOException {
      this.fieldInfo = fieldInfo;
      this.indexOptions = fieldInfo.getIndexOptions();
      this.map = map;

      String fieldDataFileName = MapPostingsFormat.getFieldDataFileName(segmentName,
          state.segmentSuffix);
      this.mfr = new MapFileReader(state.directory.openInput(fieldDataFileName, state.context));
    }

    @Override
    public boolean seekExact(BytesRef text) throws IOException {
      long key = MapPostingsFormat.getKey(text);
      int value = map.get(MapPostingsFormat.getHashcode(key, map.getCapacity()),
          MapPostingsFormat.getFingerprint(key, map.getCapacity()));
      if (value != -1) {
        currentState = mfr.read(value);
        currentTerm = text;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public BytesRef term() throws IOException {
      return currentTerm;
    }

    @Override
    public int docFreq() throws IOException {
      if (currentState != null)
        return currentState.docFreq;
      else
        return -1;
    }

    @Override
    public long totalTermFreq() throws IOException {
      if (currentState != null) {
        if (indexOptions == IndexOptions.DOCS)
          return currentState.docFreq;
        else
          return currentState.totalTermFreq;
      } else
        return -1;
    }

    @Override
    public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
      return reader.postings(fieldInfo, currentState, reuse, flags);
    }

    @Override
    public ImpactsEnum impacts(int flags) throws IOException {
      return new SlowImpactsEnum(postings(null, flags));
    }

    @Override
    public BytesRef next() throws IOException {
      throw new RuntimeException("next() called in MapFieldsReader.MapTermsEnum");
    }

    @Override
    public SeekStatus seekCeil(BytesRef text) throws IOException {
      throw new RuntimeException("seekCeil() called in MapFieldsReader.MapTermsEnum");
    }

    @Override
    public long ord() throws IOException {
      throw new RuntimeException("ord() called in MapFieldsReader.MapTermsEnum");
    }

    @Override
    public void seekExact(long ord) throws IOException {
      throw new RuntimeException("seexExact(long ord) called in MapFieldsReader.MapTermsEnum");
    }
  }

}
