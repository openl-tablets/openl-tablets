## Troubleshooting

### Common Issues

#### Port 8080 Already in Use
- Change Tomcat port in `server.xml`
- Or stop conflicting application

#### JAVA_HOME Not Set
```bash
# Windows
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot"

# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

#### Out of Memory Errors
- Increase heap size: `-Xmx4096m` or higher
- Monitor with JConsole or VisualVM

#### Database Connection Fails
- Verify JDBC driver is in `<TOMCAT_HOME>\lib\`
- Check database URL, username, password
- Ensure database server is running
- Verify firewall rules allow connection

#### Encoding Issues
- Ensure `URIEncoding="UTF-8"` in Tomcat connector
- Set file encoding: `-Dfile.encoding=UTF-8`

---

