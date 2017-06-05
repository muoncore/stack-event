
publish-snapshot:
	$(MAKE) -C java publish-snapshot
	$(MAKE) -C javascript publish-snapshot

test:
	-$(MAKE) -C java test
	-$(MAKE) -C javascript test
