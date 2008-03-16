SET OPENL_VERSION=5.0.6

call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.core -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.core_%OPENL_VERSION%.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.commons -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.commons_%OPENL_VERSION%.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.rules -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.rules_%OPENL_VERSION%.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.rules.validator -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.rules.validator_%OPENL_VERSION%.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.j -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.j_%OPENL_VERSION%.jar -DgeneratePom=true
call mvn install:install-file -DgroupId=org.openl.rules -DartifactId=org.openl.rules.helpers -Dversion=%OPENL_VERSION% -Dpackaging=jar -Dfile=plugins/org.openl.rules.helpers_%OPENL_VERSION%.jar -DgeneratePom=true
