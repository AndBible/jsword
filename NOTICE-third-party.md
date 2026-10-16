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

## Lucene 3.6.2 analyzer ports (Apache License 2.0)

These commonMain files are hand ports of Lucene 3.6.2 classes (`lucene-analyzers`, Apache License 2.0, see
`search/LICENSE.apache-2.0.txt`), reproducing their behaviour under `Version.LUCENE_29` and the baseline JSword chains:

- `GermanStemmer.kt`: from `org.apache.lucene.analysis.de.GermanStemmer` (with `GermanStemFilter`).
- `ArabicLetterTokenizer.kt`, `ArabicNormalizer.kt`, `ArabicStemmer.kt` (also `StemmerUtil.delete`): from
  `org.apache.lucene.analysis.ar` and `org.apache.lucene.analysis.util.StemmerUtil`.
- `PersianAnalyzer.kt` (`PersianNormalizer`, from `org.apache.lucene.analysis.fa`), with the Arabic chain of
  `ArabicAnalyzer.kt`.

## Snowball stemmers (BSD 3-clause)

`src/commonMain/kotlin/org/crosswire/jsword/index/analysis/snowball/` is a Kotlin port of the Snowball stemmers as
shipped in Apache Lucene 3.6.2 (`org.tartarus.snowball`: `SnowballProgram`, `Among` and the Danish, Dutch, Finnish,
French, Italian, Norwegian, Portuguese, Russian, Spanish and Swedish stemmers, Snowball SVN rev 502 as modified by
Lucene; Lucene itself is Apache License 2.0). The stemmer classes were converted mechanically with
`scripts/snowball-j2k.py`. Each file carries the original copyright notice and BSD conditions:

- Copyright (c) 2001, Dr Martin Porter
- Copyright (c) 2002, Richard Boulton

All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted
under the 3-clause BSD conditions reproduced in the header of every file in that directory.
