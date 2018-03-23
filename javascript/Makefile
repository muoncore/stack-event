
.PHONY: test

build: clean
	-tsc

clean:
	rm -rf distribution

publish-snapshot:
	yarn
	yarn build
	npm version --no-git-tag-version prerelease
	npm publish --tag next
	git add package.json
	git commit -m "Update snapshot version"
	git push origin


test:
	yarn
	tsc
	yarn test
