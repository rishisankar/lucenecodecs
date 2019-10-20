package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.lucene50.BlockTermStateHelper;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.rsankar.lucenecodecs.robinhood.RobinHoodHashMap;

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
      RobinHoodHashMap map = new RobinHoodHashMap(termCount);
      FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
      writer.setField(fieldInfo);

      TermsEnum termsEnum = terms.iterator();
      while (true) {
        BytesRef term = termsEnum.next();
        if (term == null) {
          break;
        }

        long key = Long.valueOf(term.utf8ToString().hashCode());
        BlockTermState bts = writer.writeTerm(term, termsEnum, docsSeen, norms);
        sumTotalTermFreq += bts.totalTermFreq;
        sumDocFreq += bts.docFreq;
        ByteBuffer buffer = BlockTermStateHelper.buildBuffer(key, bts);
        byte[] arr = new byte[56];
        buffer.position(0);
        buffer.get(arr);

        map.put(key, arr);

      }

      String fileName = MapPostingsFormat.getFieldMapFileName(segmentName, state.segmentSuffix);
      IndexOutput out = state.directory.createOutput(fileName, state.context);
      out.writeInt(docsSeen.cardinality());
      out.writeInt(termCount);
      out.writeLong(sumTotalTermFreq);
      out.writeLong(sumDocFreq);
      byte[] arr = map.bufferToByteArray();
      out.writeBytes(arr, arr.length);
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
