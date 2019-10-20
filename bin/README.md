This repo is still in its very early stages. The code that already exists here demonstrates feasibility of the approach. I will be cleaning it up and adding documentation shortly.

The end goal is to provide a capability to index terms in Lucene that can be encoded as numbers. By default, Lucene retrieves index terms through a Trie-like data structure - while this works well for text terms, there are better options for terms that are encoded as numbers which will improve query performance.
