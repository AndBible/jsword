tx-push:
	tx push -s -r andbible.biblenames

	# cp src/jvmMain/resources/BibleNames.properties src/jvmMain/resources/BibleNames_en_XX.properties
	# rm src/jvmMain/resources/BibleNames_en_XX.properties

tx-pull:
	tx pull --force --all
	# Download language corrections to english (en_GB in transifex, mapped to en via transifex config)
	# tx pull -l en_GB --force --minimum-perc 0 -r andbible.biblenames
	# move en_GB to BibleNames.properties
	cp src/jvmMain/resources/BibleNames_en_XX.properties src/jvmMain/resources/BibleNames_en.properties
	mv src/jvmMain/resources/BibleNames_en_XX.properties src/jvmMain/resources/BibleNames.properties

