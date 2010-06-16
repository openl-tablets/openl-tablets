1) build project using the following command:
	mvn clean install

2) create archetype
	a) call mvn archetype:create-from-project
	b) cd target/generated-sources/archetype/
	c) call mvn install

3) use archetype
	a) call mvn archetype:generate -DarchetypeCatalog=local