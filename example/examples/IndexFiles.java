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

package examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

import examples.formats.MapPostingsFormats;
import examples.formats.SimpleLongFormat;
import examples.formats.SimpleTextFormat;
import examples.formats.SpecializedLongFormat;

public final class IndexFiles {

  public static void main(String[] args) {

    MapPostingsFormats format;
    if (args.length == 0) {
      format = MapPostingsFormats.SIMPLE_LONG_FORMAT;
    } else {
      if (args[0].equalsIgnoreCase("simple")) {
        format = MapPostingsFormats.SIMPLE_LONG_FORMAT;
      } else if (args[0].equalsIgnoreCase("specialized")) {
        format = MapPostingsFormats.SPECIALIZED_LONG_FORMAT;
      } else if (args[0].equalsIgnoreCase("text")) {
        format = MapPostingsFormats.SIMPLE_TEXT_FORMAT;
      } else {
        System.err.println("Usage: examples.IndexFiles [simple/specialized/text]");
        return;
      }
    }

    String indexPath = "example/data/index/index_" + format;
    String docsPath = "example/data/docs/docs_" + format;

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" + docDir.toAbsolutePath()
          + "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      // FSDirectory.open() chooses the best directory implementation - which is
      // MMapDirectory for MacOS
      Directory dir = FSDirectory.open(Paths.get(indexPath));
      IndexWriterConfig iwc = new IndexWriterConfig();
      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      iwc.setUseCompoundFile(false);
      iwc.setCodec(new Lucene80Codec() {
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {
          if (field.equals("path")) {
            return new Lucene50PostingsFormat();
          } else {
            switch (format) {
            case SIMPLE_LONG_FORMAT:
              return new SimpleLongFormat();
            case SPECIALIZED_LONG_FORMAT:
              return new SpecializedLongFormat();
            case SIMPLE_TEXT_FORMAT:
              return new SimpleTextFormat();
            default:
              throw new RuntimeException(); // should never reach here...
            }
          }
        }
      });

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir, format);

      writer.close();
    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
    }
  }

  static void indexDocs(IndexWriter writer, Path path, MapPostingsFormats format)
      throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, format);
          } catch (IOException ignore) {
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, format);
    }
  }

  /**
   * Indexes a single document.
   */
  static void indexDoc(IndexWriter writer, Path file, MapPostingsFormats format)
      throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      Document doc = new Document();

      // Add the path of the file to the index
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);

      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      String line;
      while ((line = reader.readLine()) != null) {
        if (format == MapPostingsFormats.SIMPLE_TEXT_FORMAT) {
          doc.add(new StringField("longs", line, Field.Store.NO));
        } else {
          Long l = Long.parseLong(line, 16);
          doc.add(new IndexedLongField("longs", l, Field.Store.NO));
        }
      }

      System.out.println("Adding " + file + " . . .");
      writer.addDocument(doc);
    }
  }
}
