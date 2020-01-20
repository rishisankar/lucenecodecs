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

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.lucene50.BlockTermStateHelper;
import org.apache.lucene.store.IndexOutput;

public class MapFileWriter {
  IndexOutput out;
  int index;

  public MapFileWriter(IndexOutput out) {
    this.out = out;
    this.index = 0;
  }

  // Returns pointer to location in file where data is stored
  public int saveToFile(BlockTermState bts) throws IOException {
    int location = (int) out.getFilePointer();
    BlockTermStateHelper.writeToFile(out, bts);
    return location;
  }

  public void close() throws IOException {
    out.close();
  }
}
