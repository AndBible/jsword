# Third-party notices for KSword search

## Apache Lucene 3.6.2 derived tables (Apache License 2.0)

`src/embeddedResources/search/` holds data tables generated from Apache Lucene 3.6.2 (`lucene-core` and
`lucene-analyzers`), and the classic tokenizer's DFA tables extracted from the same jar:

- `lucene-letter-ranges.tsv`, `lucene-lowercase.tsv`, `lucene-ascii-folding.tsv`,
  `lucene-character-categories.tsv`: character tables derived from the Lucene 3.6.2 analyzers' use of the JDK
  `Character` API.
- `lucene-classic-tokenizer-2.9.bin`: the `ClassicTokenizerImpl` tables (`ZZ_CMAP`, `ZZ_ACTION`, `ZZ_ROWMAP`,
  `ZZ_TRANS`, `ZZ_ATTRIBUTE`) extracted losslessly from `lucene-core-3.6.2.jar`.

Lucene is licensed under the Apache License, Version 2.0, reproduced in `search/LICENSE.apache-2.0.txt`.
Provenance, generation details and SHA-256 values are in `search/PROVENANCE.md`.
