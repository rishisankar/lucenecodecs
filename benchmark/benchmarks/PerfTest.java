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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.rsankar.lucenecodecs.field.IndexedLongField;
import org.rsankar.lucenecodecs.termmap.RobinHoodHashMap;

public class PerfTest {

  static int count;

  public static void main(String[] args) throws IOException {
    if (args.length != 1 || (!(args[0].equals("map") || args[0].equals("default")))) {
      System.out.println("USAGE: benchmarks.PerfTest [map/default]");
      return;
    }

    String indexPath = "benchmark/data/index/data1000000/" + args[0];
    String docsPath = "benchmark/data/docs/data1000000";

    Set<Long> terms = new HashSet<>();
    BufferedReader br = new BufferedReader(new FileReader(docsPath));
    String line;
    while ((line = br.readLine()) != null) {
      StringTokenizer strtok = new StringTokenizer(line);
      while (strtok.hasMoreTokens()) {
        long term = Long.parseLong(strtok.nextToken(), 16);
        terms.add(term);
      }
    }
    br.close();

    List<Query> queries = new ArrayList<>();
    for (long term : terms) {
      Term t = new Term("longs", IndexedLongField.longToBytesRef(term));
      queries.add(new ConstantScoreQuery(new TermQuery(t)));
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs results;

    int searchCount = 0;
    long startTime = System.nanoTime();
    for (Query query : queries) {
      results = searcher.search(query, 10);
      count += results.scoreDocs.length;
      if (++searchCount % 10000 == 0)
        System.out.print(searchCount + ".");
      if (searchCount > 100000)
        break;
    }
    long elapsed = System.nanoTime() - startTime;

    System.out.println();
    System.out.println("elapsed: " + elapsed);
    System.out.println("iterations: " + RobinHoodHashMap.iterations);
    System.out.println("calls: " + RobinHoodHashMap.calls);
    System.out.println("ratio: " + RobinHoodHashMap.iterations * 1.0 / RobinHoodHashMap.calls);

    System.out.println("count: " + count);
  }
}
