# Security Policy

## Supported Versions

We release security updates for the following versions of OpenL Tablets:

| Version | Supported          |
| ------- | ------------------ |
| 6.x.x   | :white_check_mark: |
| 5.x.x   | :x:                |
| < 5.0   | :x:                |

## Reporting a Vulnerability

We take the security of OpenL Tablets seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### How to Report a Security Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them using one of the following methods:

1. **GitHub Security Advisories** (Preferred)
   - Navigate to the [Security tab](https://github.com/openl-tablets/openl-tablets/security) of this repository
   - Click "Report a vulnerability"
   - Fill out the form with details about the vulnerability

2. **Direct Contact**
   - Contact the project maintainers directly via GitHub
   - You can find the maintainers in the [pom.xml](pom.xml) file

### What to Include in Your Report

Please include the following information in your vulnerability report:

- **Type of vulnerability** (e.g., SQL injection, XSS, authentication bypass, etc.)
- **Full paths of source file(s)** related to the vulnerability
- **Location of the affected source code** (tag/branch/commit or direct URL)
- **Step-by-step instructions** to reproduce the vulnerability
- **Proof-of-concept or exploit code** (if possible)
- **Impact of the vulnerability** including how an attacker might exploit it
- **Any potential mitigations** you've identified

### What to Expect

After you submit a vulnerability report:

1. **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 3 business days
2. **Assessment**: We will investigate the vulnerability and assess its impact
3. **Communication**: We will keep you informed about our progress
4. **Resolution**: We will work on a fix and coordinate disclosure timing with you
5. **Credit**: We will credit you in the security advisory (unless you prefer to remain anonymous)

### Disclosure Policy

- We request that you give us reasonable time to address the vulnerability before any public disclosure
- We will work with you to understand the scope and severity of the issue
- We aim to address critical vulnerabilities within 30 days
- Once a fix is available, we will:
  - Release a security update
  - Publish a security advisory
  - Credit the reporter (if desired)

## Security Best Practices

### For Users

When deploying OpenL Tablets in production:

1. **Keep Updated**: Always use the latest supported version
2. **Use HTTPS**: Enable SSL/TLS for all web communications
3. **Strong Authentication**: Use strong passwords and enable multi-factor authentication when available
4. **Access Control**: Implement proper access controls and follow the principle of least privilege
5. **Network Security**: Place OpenL Tablets behind a firewall and restrict network access
6. **Regular Backups**: Maintain regular backups of your rules and data
7. **Monitor Logs**: Regularly review application logs for suspicious activity
8. **Security Scanning**: Perform regular security assessments and vulnerability scans

### For Developers

When contributing to OpenL Tablets:

1. **Input Validation**: Always validate and sanitize user input
2. **Output Encoding**: Properly encode output to prevent XSS attacks
3. **SQL Injection**: Use parameterized queries or prepared statements
4. **Authentication**: Follow secure authentication practices
5. **Authorization**: Implement proper authorization checks
6. **Dependency Management**: Keep dependencies up to date and monitor for vulnerabilities
7. **Code Review**: Participate in security-focused code reviews
8. **Testing**: Write security tests for your code

## Security Updates

Security updates will be:

- Announced on the GitHub repository
- Included in release notes
- Published as GitHub Security Advisories
- Available through our standard release channels

## Dependency Security

We actively monitor and update our dependencies to address known vulnerabilities:

- Regular dependency updates are performed
- We use tools like OWASP Dependency Check
- Critical security updates are prioritized

You can run a dependency security check locally:

```bash
mvn org.owasp:dependency-check-maven:check
```

## Known Security Considerations

### LGPL License

OpenL Tablets is licensed under the GNU Lesser General Public License (LGPL). When using OpenL Tablets in your application:

- Review the license terms to ensure compliance
- Understand the implications of dynamic linking vs. static linking
- Consult with legal counsel if you have questions about license compliance

### Third-Party Dependencies

OpenL Tablets uses various third-party libraries. While we strive to keep these updated:

- Review the security advisories for dependencies
- Assess your own risk tolerance
- Consider additional security measures in your deployment

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [GitHub Security Best Practices](https://docs.github.com/en/code-security)

## Questions

If you have questions about this security policy, please open a discussion in the GitHub repository or contact the maintainers.

Thank you for helping keep OpenL Tablets secure!
