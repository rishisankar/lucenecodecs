/**Copyright (c) 2020 Rishi Sankar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

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

  MapPostingsFormat postingsFormat;

  private final Map<String, MapTerms> termsCache = new HashMap<String, MapTerms>();

  public MapFieldsReader(SegmentReadState state, PostingsReaderBase reader,
      MapPostingsFormat postingsFormat) {
    this.state = state;
    this.reader = reader;
    this.segmentName = state.segmentInfo.name;
    this.postingsFormat = postingsFormat;
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

    private int docCount, termCount;

    private RobinHoodHashMap map;

    private int k, capacity;

    private IndexInput dataFile;

    private MapTerms(String field, IndexInput in) throws IOException {
      this.fieldInfo = state.fieldInfos.fieldInfo(field);

      this.docCount = in.readVInt();
      this.termCount = in.readVInt();
      int k = in.readVInt();

      int capacity = in.readVInt();
      this.k = k;
      this.capacity = capacity;
      postingsFormat.setCapacity(capacity);
      int distSizeBytes = in.readVInt();
      int fingerprintSizeBytes = in.readVInt();
      int valueSizeBytes = in.readVInt();

      // TODO: Switch to off-heap ByteBuffer - use RandomAccessSlice
      byte arr[] = new byte[capacity * (distSizeBytes + fingerprintSizeBytes + valueSizeBytes)];
      in.readBytes(arr, 0, arr.length);
      this.map = new RobinHoodHashMap();
      map.open(capacity, distSizeBytes, fingerprintSizeBytes, valueSizeBytes, arr);

      this.dataFile = state.directory.openInput(
          MapPostingsFormat.getFieldDataFileName(segmentName, state.segmentSuffix), state.context);
    }

    @Override
    public TermsEnum iterator() throws IOException {
      if (map != null) {
        return new MapTermsEnum(fieldInfo, map, k, capacity, dataFile);
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
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumDocFreq() throws IOException {
      throw new UnsupportedOperationException();
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
  }

  private class MapTermsEnum extends BaseTermsEnum {
    private FieldInfo fieldInfo;
    private final IndexOptions indexOptions;
    private final int k;
    private final int capacity;

    private RobinHoodHashMap map;
    private MapFileReader mfr;

    private BlockTermState currentState;
    private BytesRef currentTerm;

    private MapTermsEnum(FieldInfo fieldInfo, RobinHoodHashMap map, int k, int capacity,
        IndexInput in) throws IOException {
      this.fieldInfo = fieldInfo;
      this.indexOptions = fieldInfo.getIndexOptions();
      this.map = map;
      this.k = k;
      this.capacity = capacity;

      this.mfr = new MapFileReader(in);
    }

    @Override
    public boolean seekExact(BytesRef text) throws IOException {
      long fingerprint = postingsFormat.getFingerprint(text);
      int value = map.get(ParameterAnalyzer.getHashcode(fingerprint, k, capacity),
          fingerprint & 0xff);
      if (value != -1) {
        currentState = mfr.read(value);
        currentTerm = text;
        return true;
      } else {
        currentTerm = null;
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
      throw new UnsupportedOperationException();
    }

    @Override
    public SeekStatus seekCeil(BytesRef text) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long ord() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void seekExact(long ord) throws IOException {
      throw new UnsupportedOperationException();
    }
  }

}