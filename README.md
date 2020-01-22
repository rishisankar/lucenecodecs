## Lucene MapPostingsFormat

This library implements a specialized PostingsFormat for Lucene indices that can improve query time performance given the following criteria:
* The terms can be encoded as 64 bit numbers (i.e. as a `long`)
* Any query made must specify exact terms - wild card searches are not supported
* The term map data structure is tuned for performance, but can take a long time to initially build
* By using this format within a `PerFieldPostingsFormat`, it can be applied only to specific fields of an index - and other fields can still use the default PostingsFormat

### Example:

The examples directory contains a few simple indexing and searching examples to demonstrate usage. Instructions to run the examples can be found in `BUILD.txt`.

### Usage:

MapPostingsFormat is an abstract class. To use the MapPostingsFormat, subclass it and implement the following method:

`getFingerprint` - returns a portion of a BytesRef term that uniquely identifies that term (in most situations, most bits of the long value end up being unnecessary)

* see `org.rsankar.lucenecodecs.field.IndexedLongField.BytesRefToLong(BytesRef term)` to convert a BytesRef to long if using the IndexedLongField

Then, include the subclass in `src/main/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat`

### Future work

Additional work that needs to be done includes:
- Coming up with a better tuning algorithm to derive the best possible hash table size and hash function
- Reducing space usage (while this library improves search performance, it does make the index larger in size)
