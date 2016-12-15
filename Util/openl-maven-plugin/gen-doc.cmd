call mvn clean package site -Dreport-type=plugin-documentation -DskipTests=true

java -cp target/classes org.openl.rules.maven.utils.ReformatDocumentation

