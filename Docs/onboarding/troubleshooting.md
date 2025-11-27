# Troubleshooting Guide

**Last Updated**: 2025-11-05
**Target Audience**: Developers encountering issues with OpenL Tablets

---

## Table of Contents

- [Build Issues](#build-issues)
- [Runtime Issues](#runtime-issues)
- [Development Environment](#development-environment)
- [Performance Problems](#performance-problems)
- [Common Errors](#common-errors)

---

## Build Issues

### Maven Build Fails with "Could not resolve dependencies"

**Symptoms**:
```
[ERROR] Failed to execute goal on project org.openl.rules:
Could not resolve dependencies for project org.openl:org.openl.rules:jar:6.0.0-SNAPSHOT
```

**Solutions**:

1. **Update dependencies**:
```bash
mvn clean install -U
```

2. **Clear local repository**:
```bash
rm -rf ~/.m2/repository/org/openl
mvn clean install
```

3. **Check network/proxy settings** in `~/.m2/settings.xml`

### Out of Memory During Build

**Symptoms**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions**:

1. **Increase Maven memory**:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
mvn clean install
```

2. **Build modules separately**:
```bash
cd DEV && mvn clean install
cd ../STUDIO && mvn clean install
cd ../WSFrontend && mvn clean install
```

3. **Use quick profile**:
```bash
mvn clean install -Dquick -DskipTests
```

### Frontend Build Fails

**Symptoms**:
```
npm ERR! code ELIFECYCLE
npm ERR! errno 1
```

**Solutions**:

1. **Clean and reinstall**:
```bash
cd STUDIO/studio-ui
rm -rf node_modules package-lock.json dist
npm install
npm run build
```

2. **Check Node version**:
```bash
node --version  # Should be 24.x or compatible
npm --version
```

3. **Clear npm cache**:
```bash
npm cache clean --force
npm install
```

---

## Runtime Issues

### OpenL Studio Won't Start

**Symptoms**:
```
Error creating bean with name 'dataSource'
```

**Solutions**:

1. **Check port availability**:
```bash
lsof -i :8080
# Kill process if needed
kill -9 <PID>
```

2. **Check application.yml configuration**

3. **Verify Java version**:
```bash
java -version  # Should be Java 21+
```

4. **Check logs**:
```bash
tail -f STUDIO/org.openl.rules.webstudio/logs/application.log
```

### ClassNotFoundException

**Symptoms**:
```
java.lang.ClassNotFoundException: org.openl.rules.XXX
```

**Solutions**:

1. **Rebuild dependencies**:
```bash
mvn clean install -DskipTests
```

2. **Check classpath** - ensure all dependencies are in target/lib

3. **Verify module installation**:
```bash
ls ~/.m2/repository/org/openl/
```

### Service Deployment Fails

**Symptoms**:
```
Failed to deploy service: MyRuleService
```

**Solutions**:

1. **Check rules-deploy.xml syntax**

2. **Verify rule project exists** in repository

3. **Check service logs**:
```bash
tail -f WSFrontend/org.openl.rules.ruleservice.ws/logs/application.log
```

4. **Validate rules compile**:
```bash
# Use openl-maven-plugin
mvn openl:compile
```

---

## Development Environment

### IDE Not Recognizing Generated Code

**Symptoms**:
- Red errors on generated classes
- Cannot find symbol errors in IDE

**Solutions**:

1. **Refresh/reimport Maven project**

2. **Mark generated sources directories**:
```
target/generated-sources/openl -> Mark as Generated Sources Root
```

3. **Rebuild project** in IDE

4. **Ensure Maven plugin ran**:
```bash
mvn generate-sources
```

### Hot Reload Not Working

**Symptoms**:
- Code changes don't appear without restart

**Solutions**:

1. **Use Spring Boot DevTools** (already included):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

2. **Enable auto-build** in IDE

3. **For frontend**:
```bash
cd STUDIO/studio-ui
npm start  # Runs with hot reload
```

### Debugger Not Attaching

**Solutions**:

1. **Maven debug mode**:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

2. **Attach remote debugger** to port 5005

3. **Check firewall** not blocking debugging port

---

## Performance Problems

### Slow Rule Compilation

**Symptoms**:
- Rules take minutes to compile
- First request very slow

**Solutions**:

1. **Enable eager compilation**:
```yaml
# application.yml
ruleservice:
  lazy-compilation: false
```

2. **Optimize Excel files**:
- Remove unnecessary formatting
- Reduce file size
- Use indexed decision tables

3. **Increase memory**:
```bash
export MAVEN_OPTS="-Xmx8g"
```

4. **Use profiler** to identify bottlenecks:
```java
Profiler.enable();
// ... execute rules
Profiler.printResults();
```

### High Memory Usage

**Symptoms**:
```
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Solutions**:

1. **Increase heap**:
```bash
java -Xmx8g -Xms2g -jar application.jar
```

2. **Check for memory leaks**:
```bash
jmap -heap <PID>
jmap -histo <PID> | head -20
```

3. **Enable GC logging**:
```bash
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log
```

4. **Reduce cache sizes** in configuration

### Slow Database Queries

**Symptoms**:
- Repository operations slow
- Git operations timing out

**Solutions**:

1. **Enable query logging**:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

2. **Optimize repository configuration**

3. **Check network latency** to remote repositories

4. **Use connection pooling**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
```

---

## Common Errors

### "Table not found" Error

**Symptoms**:
```
org.openl.rules.table.TableNotFoundException: Table 'MyTable' not found
```

**Causes & Solutions**:

1. **Typo in table name** - Check spelling
2. **Table in different module** - Check module dependencies
3. **Excel file not loaded** - Verify file in project
4. **Case sensitivity** - Table names are case-sensitive

### "Method not found" Error

**Symptoms**:
```
org.openl.rules.method.MethodNotFoundException: Method 'myMethod' not found
```

**Causes & Solutions**:

1. **Method signature mismatch** - Check parameter types
2. **Method in different version** - Check service version
3. **Overload ambiguity** - Be more specific with types
4. **Excel not compiled** - Rebuild project

### "Type conversion error"

**Symptoms**:
```
Cannot convert value of type X to type Y
```

**Causes & Solutions**:

1. **Incompatible types** - Check type definitions
2. **Missing converter** - Add custom type converter
3. **Null handling** - Check for null values
4. **Date format issues** - Use consistent date formats

### "Permission denied" Error

**Symptoms**:
```
Access is denied
org.springframework.security.access.AccessDeniedException
```

**Causes & Solutions**:

1. **Not authenticated** - Log in first
2. **Insufficient permissions** - Check user roles
3. **ACL misconfiguration** - Review ACL settings
4. **Session expired** - Log in again

### "Port already in use"

**Symptoms**:
```
Web server failed to start. Port 8080 was already in use.
```

**Solutions**:

1. **Find and kill process**:
```bash
# Find process
lsof -i :8080
# Or
netstat -ano | grep 8080

# Kill process
kill -9 <PID>
```

2. **Use different port**:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

3. **Change in application.yml**:
```yaml
server:
  port: 8090
```

---

## Getting Help

### Enable Debug Logging

**Application**:
```yaml
# application.yml
logging:
  level:
    org.openl: DEBUG
    org.openl.rules: TRACE
```

**Maven**:
```bash
mvn clean install -X  # Debug mode
```

### Collect Diagnostic Information

```bash
# Java version
java -version

# Maven version
mvn -version

# Check environment
env | grep JAVA
env | grep MAVEN

# List installed modules
ls ~/.m2/repository/org/openl/

# Check running processes
ps aux | grep java
```

### Where to Report Issues

1. **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
2. **Include**:
   - OpenL version
   - Java version
   - Operating system
   - Full error message
   - Steps to reproduce
   - Relevant logs

---

## Quick Diagnostic Checklist

When encountering issues, check:

- [ ] Java 21+ installed and in PATH
- [ ] Maven 3.9.9+ installed
- [ ] JAVA_HOME set correctly
- [ ] Sufficient memory (8GB+ recommended)
- [ ] Ports 8080, 8081 available
- [ ] Internet connection for dependencies
- [ ] Latest code pulled from git
- [ ] Maven repository not corrupted
- [ ] Correct branch checked out
- [ ] All required modules built

---

**See Also**:
- [Common Tasks](common-tasks.md)
- [Development Setup](development-setup.md)
- [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)

**Last Updated**: 2025-11-05
