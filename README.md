This library implements a specialized PostingsFormat for Lucene indices that improves query time performance. It works under the following assumptions:

- The terms can be encoded as 64 bit numbers (long values)
- The query has to specify the exact term (so iteration through the terms/wild card searches are not supported) 
- The term map data structure is tuned for performance and therefore takes time to build (so this is primarily meant for offline index builds)

This PostingsFormat can be applied to specific fields in an index - so you can still have other fields that use the default PostingsFormat.

Example:

The examples directory contains a few simple indexing and searching examples to demonstrate usage. Follow the instructions in BUILD.txt to run the examples.

Usage:

To use this with terms in your index, you need to subclass MapPostingsFormat and implement the following method:

getFingerprint - returns a portion of a BytesRef term that uniquely identifies that term (in most situations, most bits of the long value end up being unnecessary)
  - see org.rsankar.lucenecodecs.field.IndexedLongField.BytesRefToLong(BytesRef term) to convert a BytesRef to long if using the IndexedLongField

You also need to include the subclass in the src/main/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat file

Then use the examples as a guide to integrate this library with your Lucene indexing and search code.

Benchmarks:

Current benchmarks show that this library can lookup terms more than twice as fast as Lucene's standard PostingsFormat implementation. The benchmark directory measures the performance of an index containing 1 million hex ids from Uber's H3 geospatial indexing system (https://uber.github.io/h3/#/). Resolution 9 ids were used for the benchmark.

Follow the instructions in BUILD.txt to run the benchmarks.

Ongoing work:

This library is still in active development, but it is ready for use. Please contact me at rishisankar@ucla.edu if you need help in getting this library set up with a project.

Additional work that needs to be done include:
- Coming up with a more robust tuning algorithm to derive the best possible hash table size and hash function
- There is a list of simple optimizations still to be done that should improve performance even more
- I will also be working on strategies to reduce space usage (while this library improves search performance, it does make the index larger in size)
