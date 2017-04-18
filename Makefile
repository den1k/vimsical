.PHONY: clean test test-clj test-cljs test-cljs-advanced

# Lein
#

clean:
	lein clean


# Test
#

test: test-clj test-cljs test-cljs-advanced

test-clj: clean
	lein with-profile backend-test test

test-cljs: clean
	lein with-profile +frontend-test,-css doo node test once

test-cljs-advanced: clean
	lein with-profile +frontend-test,-css doo node test-advanced once


# Build
#

build: build-clj build-cljs

build-clj: clean
	lein with-profile backend uberjar

build-cljs: clean
	lein with-profile frontend cljsbuild once prod

# Styles
#

dev-styles:
	lein with-profile frontend-dev garden auto