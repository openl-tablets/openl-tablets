# Virtual Machine Deployment

Deploy OpenL Tablets on traditional virtual machines (VMs) or bare metal servers.

## Overview

VM deployment is suitable for:
- On-premises infrastructure
- Air-gapped environments
- Organizations without container orchestration
- Traditional IT environments

## Deployment Options

| Option | Use Case | Complexity |
|--------|----------|------------|
| **Ubuntu + Docker Compose** | Modern Linux deployment | Low |
| **RHEL/CentOS + Docker Compose** | Enterprise Linux | Low-Medium |
| **Traditional Tomcat** | Legacy deployments | Medium |

---

## Prerequisites

### Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 4 cores | 8+ cores |
| **RAM** | 8GB | 16GB+ |
| **Disk** | 50GB | 200GB+ |
| **Network** | 1 Gbps | 10 Gbps |

### Software Requirements

- Operating System: Ubuntu 22.04+ or RHEL 8+
- Java 21+ (for traditional deployment)
- Docker 24.0+ and Docker Compose 2.0+ (for container deployment)
- PostgreSQL 12+ or MySQL 8+ (external database)

---

## Option 1: Ubuntu with Docker Compose (Recommended)

### Install Docker

```bash
# Update system
sudo apt-get update
sudo apt-get upgrade -y

# Install prerequisites
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group
sudo usermod -aG docker $USER

# Verify installation
docker --version
docker compose version
```

### Deploy OpenL Tablets

```bash
# Create deployment directory
mkdir -p ~/openl-tablets
cd ~/openl-tablets

# Download docker-compose file
curl -O https://raw.githubusercontent.com/openl-tablets/openl-tablets/master/docs/deployment/docker/docker-compose-multi.yaml

# Download configuration files
curl -O https://raw.githubusercontent.com/openl-tablets/openl-tablets/master/docs/deployment/docker/init-db.sql

# Create .env file with passwords
cat > .env <<EOF
DATABASE_PASSWORD=your_secure_password
ADMIN_PASSWORD=your_admin_password
REDIS_PASSWORD=your_redis_password
EOF

# Start services
docker compose -f docker-compose-multi.yaml up -d

# Verify deployment
docker compose ps
docker compose logs -f
```

### Access OpenL Tablets

```bash
# Get server IP
ip addr show

# Access in browser:
# - OpenL Studio: http://<server-ip>:8080
# - Rule Services: http://<server-ip>:9090
```

---

## Option 2: RHEL/CentOS with Docker Compose

### Install Docker (RHEL 8+)

```bash
# Update system
sudo dnf update -y

# Add Docker repository
sudo dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo

# Install Docker
sudo dnf install -y docker-ce docker-ce-cli containerd.io

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker --version
docker-compose --version
```

### Configure SELinux

```bash
# Allow Docker to access volumes
sudo setsebool -P container_manage_cgroup on

# Or set SELinux to permissive (not recommended for production)
# sudo setenforce 0
# sudo sed -i 's/^SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config
```

### Configure Firewall

```bash
# Open required ports
sudo firewall-cmd --permanent --add-port=8080/tcp  # Studio
sudo firewall-cmd --permanent --add-port=9090/tcp  # Rule Services
sudo firewall-cmd --permanent --add-port=80/tcp    # HTTP
sudo firewall-cmd --permanent --add-port=443/tcp   # HTTPS
sudo firewall-cmd --reload

# Verify
sudo firewall-cmd --list-ports
```

### Deploy OpenL Tablets

Follow the same steps as Ubuntu deployment above.

---

## Option 3: Traditional Tomcat Deployment

### Install Java

```bash
# Ubuntu
sudo apt-get install -y openjdk-21-jdk

# RHEL/CentOS
sudo dnf install -y java-21-openjdk java-21-openjdk-devel

# Verify
java -version
```

### Install Apache Tomcat

```bash
# Download Tomcat 10
cd /opt
sudo wget https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.18/bin/apache-tomcat-10.1.18.tar.gz
sudo tar xzf apache-tomcat-10.1.18.tar.gz
sudo ln -s apache-tomcat-10.1.18 tomcat

# Create tomcat user
sudo useradd -r -m -U -d /opt/tomcat -s /bin/false tomcat
sudo chown -R tomcat:tomcat /opt/tomcat/

# Set environment variables
sudo tee /etc/profile.d/tomcat.sh > /dev/null <<EOF
export CATALINA_HOME=/opt/tomcat
export CATALINA_BASE=/opt/tomcat
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
EOF

source /etc/profile.d/tomcat.sh
```

### Install PostgreSQL

```bash
# Ubuntu
sudo apt-get install -y postgresql postgresql-contrib

# RHEL/CentOS
sudo dnf install -y postgresql-server postgresql-contrib
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create database
sudo -u postgres psql -c "CREATE DATABASE openl_studio;"
sudo -u postgres psql -c "CREATE USER openl WITH PASSWORD 'changeme';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE openl_studio TO openl;"
```

### Deploy OpenL Studio

```bash
# Download OpenL Studio WAR
wget https://github.com/openl-tablets/openl-tablets/releases/download/v5.27.0/openl-studio.war

# Deploy to Tomcat
sudo cp openl-studio.war /opt/tomcat/webapps/
sudo chown tomcat:tomcat /opt/tomcat/webapps/openl-studio.war

# Configure application.properties
sudo mkdir -p /opt/openl/config
sudo tee /opt/openl/config/application.properties > /dev/null <<EOF
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/openl_studio
spring.datasource.username=openl
spring.datasource.password=changeme
spring.datasource.driver-class-name=org.postgresql.Driver

# Security
security.mode=multi-user

# Repository
repository.type=git
repository.path=/opt/openl/repositories

# User workspace
user.workspace.home=/opt/openl/user-workspaces
EOF

# Set JAVA_OPTS
sudo tee /opt/tomcat/bin/setenv.sh > /dev/null <<EOF
export JAVA_OPTS="\$JAVA_OPTS -Xms512m -Xmx2g -XX:+UseG1GC"
export JAVA_OPTS="\$JAVA_OPTS -Dspring.config.location=/opt/openl/config/application.properties"
EOF

sudo chmod +x /opt/tomcat/bin/setenv.sh

# Start Tomcat
sudo -u tomcat /opt/tomcat/bin/startup.sh

# View logs
tail -f /opt/tomcat/logs/catalina.out
```

### Create Systemd Service

```bash
# Create service file
sudo tee /etc/systemd/system/tomcat.service > /dev/null <<EOF
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_BASE=/opt/tomcat"
Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable tomcat
sudo systemctl start tomcat
sudo systemctl status tomcat
```

---

## Reverse Proxy Configuration

### Nginx

```bash
# Install Nginx
sudo apt-get install -y nginx  # Ubuntu
sudo dnf install -y nginx      # RHEL

# Configure site
sudo tee /etc/nginx/sites-available/openl <<EOF
server {
    listen 80;
    server_name openl.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /ruleservices/ {
        proxy_pass http://localhost:9090/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

# Enable site
sudo ln -s /etc/nginx/sites-available/openl /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx
```

### SSL with Let's Encrypt

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d openl.example.com

# Auto-renewal
sudo systemctl status certbot.timer
```

---

## Monitoring

### System Monitoring

```bash
# Install monitoring tools
sudo apt-get install -y htop iotop nethogs

# Monitor resources
htop
docker stats  # For container deployment
```

### Log Management

```bash
# View Docker logs
docker compose logs -f

# View Tomcat logs
tail -f /opt/tomcat/logs/catalina.out

# Configure log rotation
sudo tee /etc/logrotate.d/openl <<EOF
/opt/tomcat/logs/*.log {
    daily
    rotate 30
    compress
    missingok
    notifempty
    copytruncate
}
EOF
```

---

## Backup and Maintenance

### Automated Backup Script

```bash
#!/bin/bash
# backup.sh - Backup OpenL Tablets data

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d-%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
pg_dump -U openl openl_studio | gzip > $BACKUP_DIR/db-$DATE.sql.gz

# Backup workspace (Docker)
docker run --rm \
  -v openl-studio-workspace:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/workspace-$DATE.tar.gz -C /data .

# Backup workspace (Traditional)
tar czf $BACKUP_DIR/workspace-$DATE.tar.gz -C /opt/openl/workspace .

# Keep only last 7 days
find $BACKUP_DIR -name "db-*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "workspace-*.tar.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
```

### Schedule with Cron

```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * /home/user/backup.sh >> /var/log/openl-backup.log 2>&1
```

---

## Troubleshooting

### Docker Issues

```bash
# Check Docker status
sudo systemctl status docker

# View container logs
docker compose logs -f

# Restart containers
docker compose restart

# Clean up
docker system prune -a
```

### Tomcat Issues

```bash
# Check Tomcat status
sudo systemctl status tomcat

# View logs
tail -f /opt/tomcat/logs/catalina.out
tail -f /opt/tomcat/logs/localhost.*.log

# Check Java processes
ps aux | grep java

# Restart Tomcat
sudo systemctl restart tomcat
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
psql -h localhost -U openl -d openl_studio

# Check PostgreSQL status
sudo systemctl status postgresql

# View PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

---

## Security Hardening

### Firewall

```bash
# Ubuntu (UFW)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable

# RHEL (firewalld)
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### Automatic Security Updates

```bash
# Ubuntu
sudo apt-get install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades

# RHEL
sudo dnf install -y dnf-automatic
sudo systemctl enable --now dnf-automatic.timer
```

---

## Upgrading

### Docker Deployment

```bash
# Pull new images
docker compose pull

# Backup before upgrade
./backup.sh

# Restart with new images
docker compose down
docker compose up -d
```

### Traditional Deployment

```bash
# Backup
./backup.sh

# Download new WAR
wget https://github.com/openl-tablets/openl-tablets/releases/download/v6.0.0/openl-studio.war

# Stop Tomcat
sudo systemctl stop tomcat

# Replace WAR
sudo rm -rf /opt/tomcat/webapps/openl-studio*
sudo cp openl-studio.war /opt/tomcat/webapps/
sudo chown tomcat:tomcat /opt/tomcat/webapps/openl-studio.war

# Start Tomcat
sudo systemctl start tomcat

# Monitor logs
tail -f /opt/tomcat/logs/catalina.out
```

---

## Related Documentation

- [Installation Guide](../../user-guides/installation/) - Development setup
- [Docker Deployment](../docker/) - Container deployment
- [Configuration Reference](../../configuration/) - Configuration options
- [Security Guide](../../configuration/security.md) - Security best practices

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
