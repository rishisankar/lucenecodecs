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

package org.rsankar.lucenecodecs.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;

public class IndexedLongField extends Field {
  /**
   * Indexed, not tokenized, omits norms, indexes docs only, not stored
   */
  public static final FieldType TYPE_NOT_STORED = new FieldType();

  /**
   * Indexed, not tokenized, omits norms, indexes docs only, stored
   */
  public static final FieldType TYPE_STORED = new FieldType();

  static {
    TYPE_NOT_STORED.setOmitNorms(true);
    TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS);
    TYPE_NOT_STORED.setTokenized(false);
    TYPE_NOT_STORED.freeze();

    TYPE_STORED.setOmitNorms(true);
    TYPE_STORED.setIndexOptions(IndexOptions.DOCS);
    TYPE_STORED.setStored(true);
    TYPE_STORED.setTokenized(false);
    TYPE_STORED.freeze();
  }

  public IndexedLongField(String name, long value, Store stored) {
    super(name, longToBytesRef(value), stored == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
  }

  // TODO: Switch to org.apache.lucene.store.ByteArrayDataOutput
  public static BytesRef longToBytesRef(long l) {
    byte[] bytes = new byte[8];
    for (int i = 7; i >= 0; --i) {
      bytes[i] = (byte) (l & 0xff);
      l >>= 8;
    }
    return new BytesRef(bytes);
  }

  public static long BytesRefToLong(BytesRef term) {
    return BytesRefToLong(term, 0, 0);
  }

  public static long BytesRefToLong(BytesRef term, int leftOffset, int rightOffset) {
    long val = 0;
    for (int i = term.offset + leftOffset; i < term.offset + term.length - rightOffset; ++i) {
      val <<= 8;
      val |= (term.bytes[i] & 0xff);
    }
    return val;
  }
}
