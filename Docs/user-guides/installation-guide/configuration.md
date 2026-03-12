## Key Configuration Options

### Repository Types

#### JDBC Repository
- Database-backed storage
- Full versioning and history
- Multi-user collaboration
- Requires database setup

#### Git Repository
- Git-based version control
- Integration with GitHub, GitLab, Bitbucket
- Branching and merging support
- Requires Git credentials

#### AWS S3 Repository
- Cloud storage
- Scalable and distributed
- Requires AWS credentials and bucket

### User Authentication Modes

#### Multi-user (Database)
- Built-in user management
- Role-based access control
- Stored in database

#### Active Directory / LDAP
- Enterprise integration
- Centralized authentication
- Group-based permissions

#### SAML
- Single Sign-On (SSO)
- Works with Okta, Auth0, Azure AD
- Federation support

#### OAuth2
- Modern authentication
- Support for Google, GitHub, etc.
- Token-based

#### CAS
- Central Authentication Service
- University and enterprise SSO

---

## Cluster Mode Configuration

For multiple OpenL Studio instances sharing workload:

1. **Before First Launch**: Configure `openl.home.shared` in `application.properties`
2. **Shared Resources**: All instances must access the same:
   - Repository storage
   - Deployment configuration
   - User database
3. **Load Balancer**: Use sticky sessions for proper user experience

**Configuration Example:**
```properties
openl.home.shared=/path/to/shared/openl/home
```

---

