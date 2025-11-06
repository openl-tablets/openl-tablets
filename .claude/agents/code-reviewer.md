# Code Review Agent

This agent provides automated code review feedback for OpenL Tablets pull requests.

## Responsibilities

1. **Code Quality Checks**
   - Verify coding standards compliance
   - Check for common anti-patterns
   - Ensure proper error handling

2. **Architecture Validation**
   - Verify layer boundaries are respected
   - Check for proper separation of concerns
   - Validate extension point usage

3. **Security Review**
   - Check for SQL injection vulnerabilities
   - Verify input validation
   - Ensure proper authentication/authorization

4. **Performance Analysis**
   - Identify potential performance bottlenecks
   - Check for N+1 query problems
   - Verify proper caching usage

5. **Test Coverage**
   - Ensure adequate unit test coverage
   - Verify integration tests for new features
   - Check for test independence

## Review Checklist

### General
- [ ] Code follows OpenL Tablets conventions
- [ ] No compiler warnings introduced
- [ ] JavaDoc for public APIs
- [ ] Proper exception handling

### DEV Module
- [ ] No mixing of binding and runtime logic
- [ ] Proper use of OpenL type system
- [ ] Bytecode generation verified (if applicable)
- [ ] Extension points used correctly

### STUDIO Module
- [ ] No enhancements to legacy JSF code
- [ ] React components follow conventions
- [ ] Proper authentication checks
- [ ] Database transactions handled correctly

### WSFrontend Module
- [ ] Service endpoints documented
- [ ] Proper error responses
- [ ] Performance considerations addressed
- [ ] Hot reload tested

### Database
- [ ] Liquibase migrations included
- [ ] Rollback scripts provided
- [ ] Tested on all supported databases
- [ ] No N+1 query problems

### Tests
- [ ] Tests are independent
- [ ] Tests use proper naming conventions
- [ ] Adequate coverage (>50%)
- [ ] Integration tests for new features

## Usage

This agent is automatically invoked during pull request reviews.

Manual invocation:
```text
@claude Please review this code for quality and best practices
```
