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

package benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsFormat;
import org.apache.lucene.codecs.lucene80.Lucene80Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.rsankar.lucenecodecs.field.IndexedLongField;

public final class Index {

  public static void main(String[] args) {
    if (args.length != 1 || (!(args[0].equals("map") || args[0].equals("default")))) {
      System.out.println("USAGE: benchmarks.Index [map/default]");
      return;
    }

    String indexPath = "benchmark/data/index/data1000000/" + args[0];
    String docsPath = "benchmark/data/docs/data1000000";

    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      // FSDirectory.open() chooses the best directory implementation - which is
      // MMapDirectory for MacOS
      Directory dir = FSDirectory.open(Paths.get(indexPath));
      IndexWriterConfig iwc = new IndexWriterConfig();
      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      iwc.setUseCompoundFile(false);
      iwc.setRAMBufferSizeMB(2000.0);
      iwc.setCodec(new Lucene80Codec() {
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {
          if (field.equals("path") || field.equals("id")) {
            return new Lucene50PostingsFormat();
          } else {
            if (args[0].equals("default")) {
              return new Lucene50PostingsFormat();
            } else {
              return new BenchmarkFormat();
            }
          }
        }
      });

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexFile(writer, docsPath);
      // Merge all segments into one
      // writer.forceMerge(1);
      writer.close();
    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
    }
  }

  static void indexFile(IndexWriter writer, String docspath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(docspath));

    String line;
    int docID = 0;
    while ((line = reader.readLine()) != null) {

      Document doc = new Document();

      // Add the path of the file to the index
      Field pathField = new StringField("path", docspath + "/Doc" + docID, Field.Store.YES);
      Field idField = new StringField("id", Integer.toString(docID), Field.Store.YES);
      doc.add(pathField);
      doc.add(idField);

      for (String s : line.split(" ")) {
        Long l = Long.parseLong(s, 16);
        doc.add(new IndexedLongField("longs", l, Field.Store.NO));

      }

      System.out.println("Adding Doc" + docID + " . . .");
      writer.addDocument(doc);
      docID++;
    }
    reader.close();
  }
}
