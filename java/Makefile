
install: test
	./gradlew install

publish-snapshot: clean
	./gradlew artifactoryPublish

publish: clean
ifndef VERSION
	$(error VERSION is undefined for Stack Release)
endif
	echo "#DO NOT EDIT MANUALLY. Change in Makefile">gradle.properties
	echo version=$(VERSION)>>gradle.properties
	echo group=io.muoncore.protocol>>gradle.properties
	echo exclude=>>gradle.properties
	./gradlew artifactoryPublish
	git add gradle.properties
	git commit -m "Update version to $(VERSION )while publishing"
	git push origin

test:
	./gradlew --refresh-dependencies check

clean:
	./gradlew clean

testextended:
	./gradlew check

