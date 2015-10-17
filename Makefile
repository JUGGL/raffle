SHELL := /bin/bash
CODACY_PROJECT_TOKEN := 03f68f00f96d402c987a4da0adfaf43e

coverage: test
	@mvn cobertura:cobertura
	@scripts/coverage.sh

test:
	@mvn test
