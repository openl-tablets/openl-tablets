---
title: OpenL Tablets 5.9.3 Migration Notes
---

## Tomcat Configuration

1. Update `TOMCAT_HOME/conf/server.xml` by adding `URIEncoding="UTF-8"` to all `<Connector>` elements:

```xml
<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000"
           redirectPort="8443" URIEncoding="UTF-8"/>
```

2. Update the `-XX:MaxPermSize` attribute in `setenv.sh` (or `setenv.bat`) to:

```
-XX:MaxPermSize=384m
```
