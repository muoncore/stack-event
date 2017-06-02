
publish-snapshot:
	npm install
	npm run build
	npm version --no-git-tag-version prerelease
	npm publish --tag next
	git add package.json
	git commit -m "Update snapshot version"
	git push origin
