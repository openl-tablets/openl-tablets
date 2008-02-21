call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.core -Dversion=5.0.5 -Dpackaging=jar -Dfile=plugins/org.openl.core_5.0.5.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.commons -Dversion=5.0.5 -Dpackaging=jar -Dfile=plugins/org.openl.commons_5.0.5.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.rules -Dversion=5.0.5 -Dpackaging=jar -Dfile=plugins/org.openl.rules_5.0.5.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.rules.validator -Dversion=5.0.5 -Dpackaging=jar -Dfile=plugins/org.openl.rules.validator_5.0.5.jar -DgeneratePom=true
