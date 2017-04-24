.PHONY: clean test test-clj test-cljs test-cljs-advanced lein-deps deps

#
# TODO Get deps
#

checkouts/mapgraph/:
	exit 1 "Clone mapgraph and add a symlink at ./checkouts/mapgraph"

lein-deps:
	lein with-profile backend-test,frontend-test,dev,user deps

deps: checkouts/mapgraph/


# Lein
#

clean:
	lein clean


# Test
#

test: test-clj test-cljs test-cljs-advanced

test-clj: deps clean
	lein with-profile backend-test test

test-cljs: deps clean
	lein with-profile +frontend-test,-css doo node test once

test-cljs-advanced: deps clean
	lein with-profile +frontend-test,-css doo node test-advanced once


# Build
#

build: build-clj build-cljs

build-clj: deps clean
	lein with-profile backend uberjar

build-cljs: deps clean
	lein with-profile frontend cljsbuild once prod

# Styles
#

dev-styles: deps
	lein with-profile frontend-dev garden auto


dev-styles-player: deps
	lein with-profile player-dev garden auto
