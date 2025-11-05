## Invoking OpenL rules from Java

The simplest way to execute OpenL rules is to use the `OpenLService` class, which provides a `call(...)` static method.
This class is located in the following Maven dependency:

```xml
<dependency>
    <groupId>org.openl.rules</groupId>
    <artifactId>org.openl.rules.ruleservice</artifactId>
    <version>5.27.7</version>
</dependency>
```

Here's a Java example of how to invoke OpenL rules::

```java
import org.openl.rules.ruleservice.OpenLService;

public class Example {

    public static void main(String[] args) throws Exception {

        // Before the first call, it is necessary to establish certain configurations,
        // such as through the utilization of environment variables.
        System.setProperty("production-repository.uri", "/path/to/deployment/repository/folder");
        System.setProperty("production-repository.factory", "repo-file");
        System.setProperty("ruleservice.isProvideRuntimeContext", "false");

        // The first call initializes lazily OpenL Rules engine.
        var result = OpenLService.call("my-project", "sayHello", "World");

        System.out.println(result);

        // Free resources.
        OpenLService.reset();
    }
}
```

The above Java example uses a filesystem as a repository of OpenL Rules deployments. You can provide configuration in
any way described in the [Configuration](../configuration/overview.md) article.

In some cases, it can introduce complexity due to dependency on the filesystem. OpenL supports different repositories
where deployments can be stored. One of these is `repo-jar`, which is the default repository of the OpenL Rule Service.
Therefore, you can simply place a JAR file containing the OpenL project, and it will be automatically loaded.

This feature can be useful in serverless solutions, such as:

- Apache Spark jobs
- AWS Lambdas
- Azure Functions
- Google Cloud Functions.

To package an OpenL project into a JAR file, you can use the `openl-maven-plugin`, such as:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>openl-in-jar</artifactId>
    <version>0.1</version>
    <packaging>openl-jar</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openl.rules</groupId>
                <artifactId>openl-maven-plugin</artifactId>
                <version>5.27.7</version>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
```

An example of how it looks for AWS Lambda:

```java
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.openl.rules.ruleservice.OpenLService;

public class HandlerOpenLService implements RequestHandler<String, String> {

    static {
        // Trigger lazy initialization for AWS Lambda SnapStart
        try {
            OpenLService.call("my-project", "toString");
        } catch (Exception unexpected) {
            throw new RuntimeException(unexpected);
        }
    }

    @Override
    public String handleRequest(String event, Context context) {
        try {
            return (String) OpenLService.call("my-project", "sayHello", event);
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
```

An example of how to create a UDF for Apache Spark to call any OpenL rules:

```java
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.api.java.UDF3;
import org.openl.rules.ruleservice.OpenLService;

public class OpenLInvoker implements UDF3<String, String, String, Row> {

    /**
     * Returns a result in the first column or error in the second column.
     */
    @Override
    public Row call(String serviceName, String ruleName, String json) {
        try {
            return RowFactory.create(OpenLService.callJSON(serviceName, ruleName, json), null);
        } catch (Exception e) {
            return RowFactory.create(null, e.getMessage());
        }
    }
}
```

An example of how to create a UDF for Apache Spark to call any OpenL rules with two arguments:

```java
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.api.java.UDF4;
import org.openl.rules.ruleservice.OpenLService;

public class OpenLInvoker2 implements UDF4<String, String, String, String, Row> {

    /**
     * Returns a result in the first column or error in the second column.
     */
    @Override
    public Row call(String serviceName, String ruleName, String arg1, String arg2) {
        try {
            return RowFactory.create(OpenLService.callJSONArgs(serviceName, ruleName, arg1, arg2), null);
        } catch (Exception e) {
            return RowFactory.create(null, e.getMessage());
        }
    }
}
```

An example of how to call any OpenL rules from the pyspark:

```python
from pyspark.sql import SparkSession
from pyspark import SparkConf
import pyspark.sql.functions as sf

conf = SparkConf()
conf.set("spark.jars.packages", "org.openl.rules:org.openl.rules.ruleservice:5.27.6")
spark = SparkSession.builder.config(conf=conf).getOrCreate()
df = spark.read.json("requests.json")

result = df.withColumn('output', sf.reflect(
    sf.lit("org.openl.rules.ruleservice.OpenLService"),
    sf.lit("tryJSON"),
    sf.lit("my-project"),
    sf.lit("sayHello"),
    df.input
))
# Returns a JSON with two fields:
# if null:        { "result":null, "error":null }
# if succesfull:  { "result":"OK", "error":null }
# if error:       { "result":null, "error":{"message":"error"} }

```
