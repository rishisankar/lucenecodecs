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

package examples.formats;

import org.apache.lucene.util.BytesRef;
import org.rsankar.lucenecodecs.field.IndexedLongField;
import org.rsankar.lucenecodecs.mapcodec.MapPostingsFormat;

public class SpecializedLongFormat extends MapPostingsFormat {
  /*
   * For this format, all longs have the format 309d __ __ __ __ 2f This allows
   * the hashcode/fingerprint functions to require less saved space
   */

  public SpecializedLongFormat() {
    super("SpecializedLongFormat");
  }

  @Override
  public long getFingerprint(BytesRef text) {
    return IndexedLongField.BytesRefToLong(text, 3, 1);
  }
}
