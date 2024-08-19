Functionality AbstractBookAnalyzer

Add Book to analyzer
    Is this necessary
        It is used to automatically get the right language from the book
            Might be done a level higher, when selecting the right analyser
Set DoStemming
    Stemming is done by default in language-specific analyzers
Set DoStopWords
    Initializing with empty stopwords set should disable stopwording

Why is createComponents necessary?
    I think to support optional stemming and stopwords
        Both jSword itself and and-bible nowhere use these options, except when initializing them to the defaults

# TODO

Fix deleted tests
