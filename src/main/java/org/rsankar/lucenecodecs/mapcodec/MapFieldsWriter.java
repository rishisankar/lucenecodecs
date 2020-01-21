/*
 * Copyright (c) 2020 Rishi Sankar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

  private MapPostingsFormat postingsFormat;

  public MapFieldsWriter(SegmentWriteState state, PostingsWriterBase writer,
      MapPostingsFormat postingsFormat) {
    this.state = state;
    this.writer = writer;
    this.segmentName = state.segmentInfo.name;
    this.postingsFormat = postingsFormat;
  }

  @Override
  public void write(Fields fields, NormsProducer norms) throws IOException {
    FieldInfos fieldInfos = state.fieldInfos;
    for (String field : fields) {
      FixedBitSet docsSeen = new FixedBitSet(state.segmentInfo.maxDoc());
      Terms terms = fields.terms(field);
      if (terms == null) {
        continue;
      }
      ParameterAnalyzer analyzer = new ParameterAnalyzer(terms, postingsFormat);
      int termCount = analyzer.termCount;
      int capacity = analyzer.capacity;
      postingsFormat.setCapacity(capacity);
      int distSizeBytes = 1; // if not, ParameterAnalyzer will throw exception
      int fingerprintSizeBytes = 1; // if not, ParameterAnalyzer will throw exception
      int valueSizeBytes = 4; // TODO: optimize this
      RobinHoodHashMap map = new RobinHoodHashMap();
      map.create(capacity, distSizeBytes, fingerprintSizeBytes, valueSizeBytes);
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

        long fingerprint = postingsFormat.getFingerprint(term);

        BlockTermState bts = writer.writeTerm(term, termsEnum, docsSeen, norms);

        int value = mfw.saveToFile(bts);

        map.put(analyzer.getHashcode(fingerprint), fingerprint & 0xff, value);

        // System.out.println("Debug: " + map.get(analyzer.getHashcode(fingerprint),
        // fingerprint & 0xff) + " " + Long.toHexString(fingerprint));
      }

      mfw.close();

      String fieldMapFileName = MapPostingsFormat.getFieldMapFileName(segmentName,
          state.segmentSuffix);
      IndexOutput out = state.directory.createOutput(fieldMapFileName, state.context);
      out.writeVInt(docsSeen.cardinality());
      out.writeVInt(termCount);
      out.writeVInt(analyzer.k);
      map.save(out);
      out.close();
    }

  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}