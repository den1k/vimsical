.PHONY: clean test test-clj test-cljs test-cljs-advanced lein-deps deps

#
# Deps
#

deps:
	bin/lein with-profiles +backend-test,+frontend-test,+dev,+user,+test deps

#
# Lein
#

clean:
	bin/lein clean

#
# Test
#

test: test-clj test-integration test-cljs test-cljs-advanced

test-clj: clean
	bin/lein with-profile backend-test test

test-cljs: clean
	bin/lein with-profile +frontend-test,-css doo node test once

test-cljs-advanced: clean
	bin/lein with-profile +frontend-test,-css doo node test-advanced once

test-integration: clean
	bin/lein with-profile integration-test test

#
# CI
#

ci-image:
	docker build -t	julienfantin/ci:$(tag) .circleci/images/primary/
	docker login
	docker push julienfantin/ci:$(tag)

#
# Build
#

build: build-clj build-cljs

build-clj: clean
	bin/lein with-profile backend uberjar

build-cljs: clean
	bin/lein with-profile frontend cljsbuild once prod

#
# Styles
#

dev-styles:
	bin/lein with-profile frontend-dev garden auto


dev-styles-player:
	bin/lein with-profile player-dev garden auto

#
# Dev Infra
#

infra/.env:
ifeq ($(DATOMIC_LOGIN),)
	$(error DATOMIC_LOGIN is undefined)
endif
ifeq ($(DATOMIC_PASSWORD),)
	$(error DATOMIC_PASSWORD is undefined)
endif
ifeq ($(DATOMIC_LICENSE_KEY),)
	$(error DATOMIC_LICENSE_KEY is undefined)
endif
	echo "DATOMIC_LOGIN=${DATOMIC_LOGIN}" >> infra/.env
	echo "DATOMIC_PASSWORD=${DATOMIC_PASSWORD}" >> infra/.env
	echo "DATOMIC_LICENSE_KEY=${DATOMIC_LICENSE_KEY}" >> infra/.env

infra-start: infra/.env
	cd infra && docker-compose up -d

infra-logs:
	cd infra && docker-compose logs -f

infra-stop:
	cd infra && docker-compose down -v --remove-orphans

infra-run: infra-start infra-logs

#
# Dev Backend
#

run: infra-start
	bin/lein with-profile backend-dev run
