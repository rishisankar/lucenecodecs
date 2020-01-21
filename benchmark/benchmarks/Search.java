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

package benchmarks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.rsankar.lucenecodecs.field.IndexedLongField;

public class Search {

  public static void main(String[] args) throws Exception {
    if (args.length != 1 || (!(args[0].equals("map") || args[0].equals("default")))) {
      System.out.println("USAGE: benchmarks.Search [map/default]");
      return;
    }

    String index = "benchmark/data/index/data1000000/" + args[0];
    String field = "longs";
    int numHits = 15;

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);

    BufferedReader in = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8));

    while (true) {
      System.out.println("Enter query: ");

      String line = in.readLine();

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
      Term t;
      try {
        t = new Term(field, IndexedLongField.longToBytesRef(Long.parseLong(line, 16)));
      } catch (NumberFormatException e) {
        System.out.println("Invalid query: must be a hexadecimal long value");
        continue;
      }

      Query query = new ConstantScoreQuery(new TermQuery(t));
      System.out.println("Searching field \"" + field + "\" for \"" + query.toString(field) + "\"");

      long startTime = System.currentTimeMillis();
      TopDocs results = searcher.search(query, numHits);
      System.out.println("Search completed in " + (System.currentTimeMillis() - startTime) + "ms.");
      ScoreDoc[] hits = results.scoreDocs;

      if (hits.length == 0) {
        System.out.println("No results found.");
        continue;
      }

      for (int i = 0; i < hits.length; ++i) {
        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
        if (path != null) {
          System.out.println((i + 1) + ". " + path);
        } else {
          System.out.println((i + 1) + ". " + "No path for this document");
        }
      }

    }
    reader.close();
  }
}