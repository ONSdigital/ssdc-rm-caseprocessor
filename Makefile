build:
	mvn clean install

build_no_test:
	mvn clean install -Dmaven.test.skip=true -DdockerCompose.skip=true
