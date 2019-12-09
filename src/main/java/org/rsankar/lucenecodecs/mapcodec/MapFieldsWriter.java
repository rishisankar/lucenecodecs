package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.rsankar.lucenecodecs.termmap.RobinHoodHashMap;

public class MapFieldsWriter extends FieldsConsumer {

  private SegmentWriteState state;
  private PostingsWriterBase writer;
  private String segmentName;

  public MapFieldsWriter(SegmentWriteState state, PostingsWriterBase writer) {
    this.state = state;
    this.writer = writer;
    this.segmentName = state.segmentInfo.name;
  }

  @Override
  public void write(Fields fields, NormsProducer norms) throws IOException {
    FieldInfos fieldInfos = state.fieldInfos;
    for (String field : fields) {
      FixedBitSet docsSeen = new FixedBitSet(state.segmentInfo.maxDoc());
      long sumTotalTermFreq = 0;
      long sumDocFreq = 0;
      Terms terms = fields.terms(field);
      if (terms == null) {
        continue;
      }
      int termCount = countTerms(terms);
      int capacity = termCount * (100 + MapPostingsFormat.EXTRA_SPACE_PERCENT) / 100;
      int hashcodeSizeBytes = MapPostingsFormat.deriveHashcodeSizeFromCapacity(capacity);
      int valueSizeBytes = MapPostingsFormat.deriveValueSizeFromCapacity(capacity);

      RobinHoodHashMap map = new RobinHoodHashMap();
      map.create(capacity, hashcodeSizeBytes, MapPostingsFormat.FINGERPRINT_SIZE_BYTES,
          valueSizeBytes);
      FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
      writer.setField(fieldInfo);
      String fieldDataFileName = MapPostingsFormat.getFieldDataFileName(segmentName,
          state.segmentSuffix);

      MapFileWriter mfw = new MapFileWriter(
          state.directory.createOutput(fieldDataFileName, state.context));

      TermsEnum termsEnum = terms.iterator();
      while (true) {
        BytesRef term = termsEnum.next();
        if (term == null) {
          break;
        }

        long key = MapPostingsFormat.getKey(term);

        BlockTermState bts = writer.writeTerm(term, termsEnum, docsSeen, norms);
        sumTotalTermFreq += bts.totalTermFreq;
        sumDocFreq += bts.docFreq;

        int value = mfw.saveToFile(bts);

        map.put(MapPostingsFormat.getHashcode(key, capacity),
            MapPostingsFormat.getFingerprint(key, capacity), value);
      }

      mfw.close();

      String fieldMapFileName = MapPostingsFormat.getFieldMapFileName(segmentName,
          state.segmentSuffix);
      IndexOutput out = state.directory.createOutput(fieldMapFileName, state.context);
      out.writeInt(docsSeen.cardinality());
      out.writeInt(termCount);
      out.writeLong(sumTotalTermFreq);
      out.writeLong(sumDocFreq);
      map.save(out);
      out.close();
    }

  }

  // For some reason, terms.size() throws UnsupportedOperationException
  // Not sure if there's a better way to do this, but temporarily this works..
  private static int countTerms(Terms t) throws IOException {
    TermsEnum te = t.iterator();
    int count = 0;
    while (true) {
      BytesRef term = te.next();
      if (term == null) {
        break;
      }
      ++count;
    }

    return count;
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
