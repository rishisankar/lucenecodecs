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

public abstract class MapPostingsFormat extends PostingsFormat {
  public static final String FIELD_MAP_EXTENSION = "fme";
  public static final String FIELD_DATA_EXTENSION = "fde";

  private int capacity;

  public MapPostingsFormat(String name) {
    super(name);
  }

  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    PostingsWriterBase pw = new Lucene50PostingsWriter(state);
    return new MapFieldsWriter(state, pw, this);
  }

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase pr = new Lucene50PostingsReader(state);
    return new MapFieldsReader(state, pr, this);
  }

  static String getFieldMapFileName(String segmentName, String segmentSuffix) {
    return segmentName + "_" + segmentSuffix + "." + FIELD_MAP_EXTENSION;
  }

  static String getFieldDataFileName(String segmentName, String segmentSuffix) {
    return segmentName + "_" + segmentSuffix + "." + FIELD_DATA_EXTENSION;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int capacity() {
    return this.capacity;
  }

  public abstract long getFingerprint(BytesRef term);
}