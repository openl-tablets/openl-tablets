# OpenL Tablets - Troubleshooting Guide

**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-06

---

## Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [Installation Issues](#installation-issues)
- [Build Issues](#build-issues)
- [Runtime Issues](#runtime-issues)
- [Performance Issues](#performance-issues)
- [Database Issues](#database-issues)
- [Rule Compilation Issues](#rule-compilation-issues)
- [Deployment Issues](#deployment-issues)
- [Docker Issues](#docker-issues)
- [Kubernetes Issues](#kubernetes-issues)
- [Security Issues](#security-issues)
- [UI Issues](#ui-issues)
- [Getting Help](#getting-help)

---

## Quick Diagnostics

### Health Check Commands

```bash
# Check if service is running
curl http://localhost:8080/actuator/health

# Check database connectivity
curl http://localhost:8080/actuator/health/db

# Check disk space
curl http://localhost:8080/actuator/health/diskSpace

# View all health indicators
curl http://localhost:8080/actuator/health | jq
```

### Log Locations

| Component | Log Location |
|-----------|--------------|
| **OpenL Studio** | `/var/log/openl/studio.log` or `STUDIO/target/studio.log` |
| **Rule Service** | `/var/log/openl/ruleservice.log` or `WSFrontend/target/ruleservice.log` |
| **Docker Container** | `docker logs <container-id>` |
| **Kubernetes Pod** | `kubectl logs <pod-name> -n openl-tablets` |

### Quick Log Analysis

```bash
# View last 100 lines
tail -n 100 /var/log/openl/studio.log

# Follow logs in real-time
tail -f /var/log/openl/studio.log

# Search for errors
grep -i "error" /var/log/openl/studio.log

# Search for exceptions
grep -A 10 "Exception" /var/log/openl/studio.log

# Count errors by type
grep "ERROR" /var/log/openl/studio.log | cut -d' ' -f5 | sort | uniq -c | sort -rn
```

---

## Installation Issues

### Issue: JDK Not Found

**Symptoms**:
```
Error: JAVA_HOME is not defined correctly
```

**Solutions**:

```bash
# 1. Check if Java is installed
java -version

# 2. Find Java installation
which java
readlink -f $(which java)

# 3. Set JAVA_HOME (Linux/Mac)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 4. Set JAVA_HOME (Windows)
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
setx PATH "%JAVA_HOME%\bin;%PATH%"

# 5. Verify
echo $JAVA_HOME
java -version
```

### Issue: Maven Command Not Found

**Symptoms**:
```
bash: mvn: command not found
```

**Solutions**:

```bash
# 1. Install Maven (Ubuntu/Debian)
sudo apt update
sudo apt install maven

# 2. Install Maven (Mac)
brew install maven

# 3. Install Maven (Manual)
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 /opt/
export M2_HOME=/opt/apache-maven-3.9.9
export PATH=$M2_HOME/bin:$PATH

# 4. Verify
mvn -version
```

### Issue: Permission Denied

**Symptoms**:
```
Permission denied: /opt/openl
```

**Solutions**:

```bash
# 1. Change ownership
sudo chown -R $USER:$USER /opt/openl

# 2. Or use sudo
sudo mvn clean install

# 3. Or change installation directory
mvn clean install -DinstallDir=$HOME/openl
```

---

## Build Issues

### Issue: Build Fails with "Out of Memory"

**Symptoms**:
```
OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Solutions**:

```bash
# 1. Increase Maven memory
export MAVEN_OPTS="-Xmx4g -XX:MaxPermSize=512m"

# 2. Or set in .mavenrc
echo "MAVEN_OPTS=-Xmx4g" > ~/.mavenrc

# 3. Build with limited parallelism
mvn clean install -T1

# 4. Skip tests to reduce memory usage
mvn clean install -DskipTests
```

### Issue: Test Failures

**Symptoms**:
```
Tests run: 150, Failures: 3, Errors: 0, Skipped: 0
```

**Solutions**:

```bash
# 1. Run specific failed test
mvn test -Dtest=MyFailingTest

# 2. Run with verbose output
mvn test -X -Dtest=MyFailingTest

# 3. Skip tests temporarily
mvn clean install -DskipTests

# 4. Skip only failing tests (not recommended for long-term)
mvn clean install -Dmaven.test.failure.ignore=true

# 5. Update test dependencies
mvn clean install -U
```

### Issue: Docker Build Fails

**Symptoms**:
```
Cannot connect to the Docker daemon
```

**Solutions**:

```bash
# 1. Check Docker is running
docker ps

# 2. Start Docker service (Linux)
sudo systemctl start docker

# 3. Start Docker Desktop (Mac/Windows)
open -a Docker

# 4. Check Docker permissions (Linux)
sudo usermod -aG docker $USER
# Log out and log back in

# 5. Skip Docker tests
mvn clean install -DnoDocker
```

### Issue: Dependency Resolution Failures

**Symptoms**:
```
Could not resolve dependencies for project org.openl:...
```

**Solutions**:

```bash
# 1. Force update dependencies
mvn clean install -U

# 2. Clear local Maven repository
rm -rf ~/.m2/repository/org/openl
mvn clean install

# 3. Use Maven central explicitly
mvn clean install -DdownloadSources -DdownloadJavadocs

# 4. Check Maven settings.xml
cat ~/.m2/settings.xml

# 5. Try with offline mode disabled
mvn clean install --no-transfer-progress
```

---

## Runtime Issues

### Issue: Application Won't Start

**Symptoms**:
```
Application startup failed
Port 8080 already in use
```

**Solutions**:

```bash
# 1. Check if port is in use
lsof -i :8080
netstat -an | grep 8080

# 2. Kill process using the port
kill -9 $(lsof -t -i:8080)

# 3. Use different port
java -jar studio.war --server.port=8081

# 4. Check logs for actual error
tail -f logs/spring.log

# 5. Verify database connectivity
telnet localhost 5432  # PostgreSQL
telnet localhost 3306  # MySQL
```

### Issue: Out of Memory at Runtime

**Symptoms**:
```
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Metaspace
```

**Solutions**:

```bash
# 1. Increase heap size
java -Xms2g -Xmx4g -jar studio.war

# 2. Increase metaspace
java -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -jar studio.war

# 3. Enable heap dumps for analysis
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -jar studio.war

# 4. Analyze heap dump
jhat /tmp/heapdump.hprof
# Or use Eclipse Memory Analyzer (MAT)

# 5. Monitor memory usage
jstat -gc <pid> 1000
```

**Memory Configuration for Different Scenarios**:

| Scenario | Heap Size | Metaspace | GC |
|----------|-----------|-----------|-----|
| **Development** | -Xmx2g | -XX:MaxMetaspaceSize=256m | -XX:+UseG1GC |
| **Production (Small)** | -Xmx4g | -XX:MaxMetaspaceSize=512m | -XX:+UseG1GC |
| **Production (Large)** | -Xmx8g | -XX:MaxMetaspaceSize=1g | -XX:+UseG1GC -XX:MaxGCPauseMillis=200 |

### Issue: Slow Response Times

**Symptoms**:
- API responses take > 5 seconds
- UI freezes or hangs

**Diagnostics**:

```bash
# 1. Check CPU usage
top
htop

# 2. Check thread dump
jstack <pid> > thread-dump.txt

# 3. Enable GC logging
java -Xlog:gc*:file=gc.log -jar studio.war

# 4. Profile with JFR
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar studio.war

# 5. Check database queries
# Enable SQL logging in application.properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Solutions**:

```properties
# application.properties

# 1. Enable caching
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=3600s

# 2. Optimize database connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# 3. Enable compression
server.compression.enabled=true

# 4. Increase thread pool
server.tomcat.threads.max=200
```

### Issue: Connection Pool Exhausted

**Symptoms**:
```
Connection is not available, request timed out after 30000ms
```

**Solutions**:

```properties
# 1. Increase pool size
spring.datasource.hikari.maximum-pool-size=50

# 2. Reduce connection timeout
spring.datasource.hikari.connection-timeout=20000

# 3. Enable connection leak detection
spring.datasource.hikari.leak-detection-threshold=60000

# 4. Log pool stats
logging.level.com.zaxxer.hikari.HikariConfig=DEBUG
logging.level.com.zaxxer.hikari=TRACE
```

---

## Performance Issues

### Issue: High GC Overhead

**Symptoms**:
```
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Diagnostics**:

```bash
# 1. Monitor GC activity
jstat -gcutil <pid> 1000

# 2. Enable detailed GC logging
java -Xlog:gc*=debug:file=gc-debug.log:time,uptime,level,tags -jar studio.war

# 3. Analyze GC logs
# Use GCViewer or GCEasy.io
```

**Solutions**:

```bash
# 1. Switch to G1GC (recommended)
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=16m \
     -jar studio.war

# 2. Tune G1GC
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:G1ReservePercent=10 \
     -jar studio.war

# 3. Enable string deduplication
java -XX:+UseG1GC -XX:+UseStringDeduplication -jar studio.war
```

### Issue: High CPU Usage

**Diagnostics**:

```bash
# 1. Identify hot threads
top -H -p <pid>

# 2. Thread dump
jstack <pid> > thread-dump-1.txt
sleep 5
jstack <pid> > thread-dump-2.txt

# 3. Compare thread dumps
diff thread-dump-1.txt thread-dump-2.txt

# 4. CPU profiling
perf record -g -p <pid>
perf report
```

**Solutions**:

1. **Optimize rules**: Review and optimize decision tables
2. **Add caching**: Cache frequently executed rules
3. **Limit parallelism**: Reduce concurrent compilation threads
4. **Database optimization**: Add indexes, optimize queries

### Issue: Memory Leaks

**Diagnostics**:

```bash
# 1. Monitor heap usage over time
jstat -gc <pid> 10000

# 2. Take heap dumps at intervals
jmap -dump:format=b,file=heap1.bin <pid>
# Wait 1 hour
jmap -dump:format=b,file=heap2.bin <pid>

# 3. Compare heap dumps in Eclipse MAT
# Look for growing collections, leaked objects

# 4. Use VisualVM or JProfiler for live monitoring
```

**Common Causes**:
- Unclosed database connections
- Static collections that grow unbounded
- ThreadLocal variables not cleaned up
- Event listeners not unregistered

**Solutions**:

```java
// Example: Clean up ThreadLocal
private static final ThreadLocal<Context> contextHolder =
    ThreadLocal.withInitial(Context::new);

@Override
public void destroy() {
    contextHolder.remove();  // Clean up!
}
```

---

## Database Issues

### Issue: Cannot Connect to Database

**Symptoms**:
```
Connection refused
Communications link failure
```

**Diagnostics**:

```bash
# 1. Check database is running
# PostgreSQL
sudo systemctl status postgresql
ps aux | grep postgres

# MySQL
sudo systemctl status mysql
ps aux | grep mysql

# 2. Test connection
psql -h localhost -U openl -d openl_prod  # PostgreSQL
mysql -h localhost -u openl -p openl_prod  # MySQL

# 3. Check port
netstat -an | grep 5432  # PostgreSQL
netstat -an | grep 3306  # MySQL

# 4. Check firewall
sudo ufw status
sudo iptables -L
```

**Solutions**:

```bash
# 1. Start database
sudo systemctl start postgresql
sudo systemctl start mysql

# 2. Check PostgreSQL pg_hba.conf
sudo nano /etc/postgresql/16/main/pg_hba.conf
# Add: host all all 0.0.0.0/0 md5

# 3. Check MySQL bind-address
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
# Set: bind-address = 0.0.0.0

# 4. Restart database
sudo systemctl restart postgresql
sudo systemctl restart mysql

# 5. Verify connection string
# PostgreSQL
jdbc:postgresql://localhost:5432/openl_prod

# MySQL
jdbc:mysql://localhost:3306/openl_prod?useSSL=false
```

### Issue: Liquibase Migration Failures

**Symptoms**:
```
Liquibase failed to start
Validation Failed: 1 change sets check sum
```

**Solutions**:

```bash
# 1. Clear Liquibase checksums
# Connect to database
psql -U openl openl_prod

# Clear checksums
UPDATE databasechangelog SET md5sum = NULL;

# 2. Force update
spring.liquibase.drop-first=true  # DANGER: Drops all tables!

# 3. Skip Liquibase (temporary)
spring.liquibase.enabled=false

# 4. Manual migration
psql -U openl openl_prod < migration.sql
```

### Issue: Database Deadlocks

**Symptoms**:
```
Deadlock detected
Transaction was deadlocked
```

**Diagnostics**:

```sql
-- PostgreSQL: View active locks
SELECT * FROM pg_locks WHERE NOT granted;

-- PostgreSQL: View blocking queries
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename  AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- MySQL: View running transactions
SELECT * FROM information_schema.INNODB_TRX;

-- MySQL: View locks
SELECT * FROM information_schema.INNODB_LOCKS;
```

**Solutions**:

```properties
# 1. Reduce transaction scope
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# 2. Set isolation level
spring.jpa.properties.hibernate.connection.isolation=2  # READ_COMMITTED

# 3. Add timeouts
spring.datasource.hikari.connection-timeout=30000
spring.jpa.properties.javax.persistence.lock.timeout=10000
```

---

## Rule Compilation Issues

### Issue: Excel File Cannot Be Parsed

**Symptoms**:
```
Error parsing Excel file
Invalid table format
Unsupported file format
```

**Solutions**:

1. **Verify Excel file format**:
   - Use .xls (Excel 97-2003) or .xlsx (Excel 2007+)
   - Avoid .xlsm (macro-enabled) files in production
   - Check file is not corrupted

2. **Check Excel compatibility**:
   ```bash
   # Verify file with Apache POI
   java -cp poi-5.3.0.jar org.apache.poi.ss.util.ExcelExtractor myfile.xlsx
   ```

3. **Common Excel issues**:
   - Hidden sheets or columns
   - Merged cells in table headers
   - Formula errors (#REF!, #VALUE!)
   - Special characters in sheet names

4. **Debug parsing**:
   ```properties
   logging.level.org.openl.rules.table=DEBUG
   logging.level.org.openl.rules.lang.xls=DEBUG
   ```

### Issue: Type Resolution Errors

**Symptoms**:
```
Cannot resolve type 'MyType'
Type mismatch: expected String, found Integer
```

**Solutions**:

1. **Check datatype definitions**:
   - Ensure datatype tables are defined before use
   - Check spelling and case sensitivity
   - Verify all required properties are defined

2. **Import required types**:
   ```
   // In Excel, add Environment table
   Environment
   dependency = path/to/datatypes.xlsx
   ```

3. **Enable type debugging**:
   ```properties
   logging.level.org.openl.binding=DEBUG
   logging.level.org.openl.types=DEBUG
   ```

### Issue: Method Not Found

**Symptoms**:
```
Method 'calculatePremium' not found
Cannot find method matching signature
```

**Solutions**:

1. **Check method signature**:
   - Parameter types must match exactly
   - Parameter order matters
   - Check for typos in method name

2. **Verify table name**:
   ```
   Method String calculatePremium(Driver driver, Vehicle vehicle)
   ```

3. **Check visibility**:
   - Method table must not be private
   - Ensure method is in active module

---

## Deployment Issues

### Issue: WAR File Deploy Fails

**Symptoms**:
```
Failed to deploy application
Context initialization failed
```

**Solutions**:

```bash
# 1. Check WAR file integrity
jar -tf studio.war | head
unzip -t studio.war

# 2. Check application server logs
tail -f $TOMCAT_HOME/logs/catalina.out
tail -f $JBOSS_HOME/standalone/log/server.log

# 3. Increase deployment timeout (Tomcat)
# Edit $TOMCAT_HOME/conf/server.xml
<Connector port="8080"
           connectionTimeout="60000"
           .../>

# 4. Verify JAVA_HOME
echo $JAVA_HOME
```

### Issue: Hot Reload Not Working

**Symptoms**:
- Rule changes don't take effect
- Must restart server for changes

**Solutions**:

```properties
# 1. Enable hot reload
ruleservice.deploy.hot.reload=true
ruleservice.deploy.watch.interval=5000

# 2. Check file system watcher
logging.level.org.openl.rules.ruleservice.publish=DEBUG

# 3. Manual reload via API
curl -X POST http://localhost:8080/admin/reload

# 4. Clear cache
curl -X POST http://localhost:8080/admin/cache/clear
```

---

## Docker Issues

### Issue: Container Exits Immediately

**Symptoms**:
```bash
docker ps -a
# Container status: Exited (1)
```

**Diagnostics**:

```bash
# 1. View container logs
docker logs <container-id>

# 2. Run with interactive shell
docker run -it openltablets/webstudio:latest /bin/sh

# 3. Check entrypoint
docker inspect openltablets/webstudio:latest | grep -A 5 Entrypoint
```

**Solutions**:

```bash
# 1. Override entrypoint for debugging
docker run -it --entrypoint /bin/bash openltablets/webstudio:latest

# 2. Check environment variables
docker run --rm openltablets/webstudio:latest env

# 3. Mount logs volume
docker run -v /tmp/logs:/var/log/openl openltablets/webstudio:latest
```

### Issue: Docker Network Issues

**Symptoms**:
- Containers cannot communicate
- Cannot connect to database container

**Solutions**:

```bash
# 1. Check network
docker network ls
docker network inspect bridge

# 2. Use custom network
docker network create openl-network
docker run --network openl-network --name db postgres:16
docker run --network openl-network --name studio openltablets/webstudio:latest

# 3. Use Docker Compose
docker compose up
docker compose ps
docker compose logs
```

---

## Kubernetes Issues

### Issue: Pod CrashLoopBackOff

**Symptoms**:
```bash
kubectl get pods
# STATUS: CrashLoopBackOff
```

**Diagnostics**:

```bash
# 1. View pod logs
kubectl logs <pod-name> -n openl-tablets

# 2. View previous container logs
kubectl logs <pod-name> --previous -n openl-tablets

# 3. Describe pod
kubectl describe pod <pod-name> -n openl-tablets

# 4. Get events
kubectl get events -n openl-tablets --sort-by='.lastTimestamp'
```

**Solutions**:

```yaml
# 1. Adjust liveness/readiness probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 90  # Increase delay
  periodSeconds: 30
  failureThreshold: 5  # Allow more failures

# 2. Increase resources
resources:
  requests:
    memory: "2Gi"  # Was 1Gi
    cpu: "1000m"   # Was 500m
```

### Issue: ImagePullBackOff

**Symptoms**:
```
Failed to pull image
ImagePullBackOff
```

**Solutions**:

```bash
# 1. Check image exists
docker pull openltablets/webstudio:latest

# 2. Create image pull secret
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=<username> \
  --docker-password=<password> \
  -n openl-tablets

# 3. Use secret in deployment
spec:
  imagePullSecrets:
  - name: regcred

# 4. Check image pull policy
imagePullPolicy: IfNotPresent  # or Always
```

---

## Security Issues

### Issue: Authentication Fails

**Symptoms**:
```
401 Unauthorized
Invalid credentials
```

**Solutions**:

```bash
# 1. Check credentials
curl -u admin:admin123 http://localhost:8080/api/projects

# 2. Reset password (database)
psql -U openl openl_prod
UPDATE users SET password = crypt('newpassword', gen_salt('bf')) WHERE username = 'admin';

# 3. Enable debug logging
logging.level.org.springframework.security=DEBUG

# 4. Check authentication provider
spring.security.user.name=admin
spring.security.user.password=admin123
```

### Issue: CORS Errors

**Symptoms**:
```
Access-Control-Allow-Origin header is not present
CORS policy: No 'Access-Control-Allow-Origin' header
```

**Solutions**:

```properties
# application.properties

# 1. Enable CORS
management.endpoints.web.cors.allowed-origins=http://localhost:3000
management.endpoints.web.cors.allowed-methods=GET,POST,PUT,DELETE
management.endpoints.web.cors.allowed-headers=*

# 2. Or configure in code
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }
}
```

### Issue: SSL/TLS Errors

**Symptoms**:
```
unable to find valid certification path
PKIX path building failed
```

**Solutions**:

```bash
# 1. Import certificate to Java keystore
keytool -import -trustcacerts -alias mycert \
  -file certificate.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit

# 2. Disable SSL verification (NOT for production!)
curl -k https://localhost:8443/api/projects

# 3. Use custom truststore
java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=changeit \
     -jar studio.war
```

---

## UI Issues

### Issue: OpenL Studio UI Not Loading

**Symptoms**:
- Blank page
- Infinite loading spinner
- Console errors

**Diagnostics**:

```bash
# 1. Check browser console (F12)
# Look for JavaScript errors, network errors

# 2. Check network tab
# Look for failed requests (404, 500)

# 3. Check server logs
tail -f logs/studio.log | grep ERROR

# 4. Clear browser cache
# Chrome: Ctrl+Shift+Delete
# Firefox: Ctrl+Shift+Delete
```

**Solutions**:

1. **Clear browser cache**
2. **Disable browser extensions**
3. **Try incognito/private mode**
4. **Check for proxy/firewall issues**
5. **Verify server is running**

### Issue: React UI Build Failures

**Symptoms**:
```
npm ERR! Build failed
Module not found
```

**Solutions**:

```bash
# 1. Clean and reinstall
cd STUDIO/studio-ui
rm -rf node_modules package-lock.json
npm install

# 2. Clear npm cache
npm cache clean --force
npm install

# 3. Use specific Node version
nvm install 20
nvm use 20
npm install

# 4. Check for peer dependency issues
npm ls
npm dedupe
```

---

## Getting Help

### Before Asking for Help

1. **Check logs**: Review application logs for error messages
2. **Search existing issues**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
3. **Try quick fixes**: Restart, clear cache, update dependencies
4. **Gather information**: Version, OS, Java version, stack trace

### Information to Provide

When reporting issues, include:

```
**Environment**:
- OpenL Tablets Version: 6.0.0-SNAPSHOT
- Java Version: 21.0.1
- OS: Ubuntu 22.04
- Database: PostgreSQL 16
- Deployment: Docker / Kubernetes / Tomcat

**Steps to Reproduce**:
1. ...
2. ...
3. ...

**Expected Behavior**:
...

**Actual Behavior**:
...

**Logs**:
```
Paste relevant log excerpt here
```

**Additional Context**:
...
```

### Support Channels

- **GitHub Issues**: [openl-tablets/openl-tablets/issues](https://github.com/openl-tablets/openl-tablets/issues)
- **GitHub Discussions**: [openl-tablets/openl-tablets/discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Documentation**: [docs/](../docs/)
- **Website**: [openl-tablets.org](https://openl-tablets.org)

---

## Additional Resources

- [FAQ](FAQ.md)
- [Architecture Guide](ARCHITECTURE.md)
- [Deployment Guide](DEPLOYMENT.md)
- [Performance Tuning](guides/performance-tuning.md)
- [Security Best Practices](guides/security-best-practices.md)

---

**Still having issues?** Open an issue on [GitHub](https://github.com/openl-tablets/openl-tablets/issues) with detailed information about your problem.
