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
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

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

public class VerifyCorrectness {
  public static void main(String[] args) throws IOException {
    Set<Long> terms = new HashSet<Long>();
    String mapIndexPath = "benchmark/data/index/data1000000/map";
    String defaultIndexPath = "benchmark/data/index/data1000000/default";
    String docsPath = "benchmark/data/docs/data1000000";
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
    Set<Integer> docs = new HashSet<Integer>();
    boolean passedAllTests = true;
    int termCount = 0;
    System.out.println(terms.size() + " total terms.");
    long startTime = System.currentTimeMillis();
    for (long term : terms) {
      if (++termCount % 500 == 0) {
        long timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("Finished analyzing " + termCount + "/" + terms.size() + " terms in "
            + timeTaken / 1000 + " seconds. Time remaining: "
            + ((int) timeTaken / 1000.0 * terms.size() / termCount) + " seconds.");
      }
      try {
        // System.out.println("Starting test for term " + Long.toHexString(term));
        Term t = new Term("longs", IndexedLongField.longToBytesRef(term));
        Query query = new ConstantScoreQuery(new TermQuery(t));

        IndexReader readerDefault = DirectoryReader
            .open(FSDirectory.open(Paths.get(defaultIndexPath)));
        IndexSearcher searcherDefault = new IndexSearcher(readerDefault);
        IndexReader readerMap = DirectoryReader.open(FSDirectory.open(Paths.get(mapIndexPath)));
        IndexSearcher searcherMap = new IndexSearcher(readerMap);

        TopDocs results = searcherDefault.search(query, 15);
        ScoreDoc[] hits = results.scoreDocs;

        for (ScoreDoc doc : hits) {
          docs.add(Integer.parseInt(searcherDefault.doc(doc.doc).get("id")));
        }

        results = searcherMap.search(query, 15);
        hits = results.scoreDocs;

        if (hits.length != docs.size()) {
          System.out.println("Failed size test for term " + Long.toHexString(term)
              + ": Default size " + docs.size() + ", map size " + hits.length);
          passedAllTests = false;
        }

        for (ScoreDoc doc : hits) {
          if (!(docs.contains(Integer.parseInt(searcherMap.doc(doc.doc).get("id"))))) {
            System.out.println("Doc " + Integer.parseInt(searcherMap.doc(doc.doc).get("id"))
                + " found in map search and not default for term " + Long.toHexString(term));
            passedAllTests = false;
          }
        }

        docs.clear();
      } catch (Exception e) {
        docs.clear();
        System.out.println("Error when testing term " + Long.toHexString(term));
        passedAllTests = false;
      }
    }
    if (passedAllTests) {
      System.out.println("MapPostingsFormat produced same result as Lucene50PostingsFormat!");
    }
  }
}
