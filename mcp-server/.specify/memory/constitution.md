# OpenL Tablets MCP Server - Constitution

## Project Identity

**Name**: OpenL Tablets MCP Server
**Purpose**: Model Context Protocol server for OpenL Tablets Business Rules Management System
**Version**: 1.0.0
**License**: MIT
**Repository**: https://github.com/openl-tablets/openl-tablets/tree/main/mcp-server

## Mission Statement

Provide a production-ready, enterprise-grade MCP server that enables AI coding agents to interact seamlessly with OpenL Tablets Business Rules Management System through a type-safe, well-documented, and secure interface.

## Core Principles

### 1. Type Safety Above All

- **Strict TypeScript** with zero tolerance for `any` types
- **Runtime validation** using Zod schemas for all inputs
- **Explicit return types** on all functions
- **Type inference** from schemas to avoid duplication
- **Type guards** for proper error handling

**Rationale**: Type safety prevents runtime errors and provides excellent IDE support for developers.

### 2. Security First

- **Never log credentials** - All error messages must be sanitized
- **Input validation** - Validate all user inputs before processing
- **URL validation** - Check all URLs for format and safety
- **Timeout limits** - All network requests must have timeouts
- **Least privilege** - Request only necessary permissions

**Implementation**:
```typescript
// All errors pass through sanitizeError()
catch (error: unknown) {
  const sanitizedMessage = sanitizeError(error);
  throw new McpError(ErrorCode.InternalError, sanitizedMessage);
}
```

### 3. Modular Architecture

- **Single responsibility** - Each module has one clear purpose
- **Clear separation** - No circular dependencies
- **Well-defined interfaces** - Clean contracts between modules
- **Easy to extend** - Adding features should be straightforward

**Module Structure**:
- `index.ts` - Server orchestration
- `client.ts` - OpenL API client
- `auth.ts` - Authentication lifecycle
- `tools.ts` - Tool definitions
- `schemas.ts` - Input validation
- `types.ts` - Type definitions
- `constants.ts` - Configuration
- `utils.ts` - Security utilities

### 4. Documentation as Code

- **JSDoc on all public APIs** - No exceptions
- **Parameter descriptions** - Explain what each parameter does
- **Usage examples** - Show how to use complex functions
- **External documentation** - Comprehensive guides for users

**Documentation Set**:
- README.md - User-facing overview
- AUTHENTICATION.md - Auth setup guide
- CONTRIBUTING.md - Developer guide
- TESTING.md - Testing documentation
- EXAMPLES.md - Usage examples
- BEST_PRACTICES.md - Implementation practices

### 5. Test-Driven Quality

- **Comprehensive tests** - All critical paths covered
- **Mock data** - Realistic test scenarios
- **Fast feedback** - Tests run quickly in watch mode
- **Coverage goals** - Maintain >80% coverage on critical modules

**Testing Stack**:
- Jest with ESM support
- Nock for HTTP mocking
- TypeScript support via ts-jest

### 6. Error Handling Consistency

- **Unknown over any** - Error types should be unknown
- **Type guards** - Check error types before accessing properties
- **Enhanced context** - Include HTTP status, endpoint, method
- **Sanitized messages** - Redact all credentials

**Error Pattern**:
```typescript
catch (error: unknown) {
  if (axios.isAxiosError(error)) {
    throw new McpError(
      ErrorCode.InternalError,
      `API error (${error.response?.status}): ${sanitizeError(error)}`
    );
  }
  throw error;
}
```

### 7. Performance Consciousness

- **Token caching** - Cache OAuth tokens with expiration
- **Connection pooling** - Use Axios default pooling
- **Lazy evaluation** - Fetch resources only when needed
- **Concurrent safety** - Prevent duplicate requests

### 8. Maintainability Focus

- **Clear naming** - Functions named for what they do
- **Consistent patterns** - Same approach throughout codebase
- **No magic numbers** - All constants defined and named
- **Comments when needed** - Explain complex logic

### 9. MCP Protocol Compliance

- **Latest SDK** - Use stable MCP SDK v1.21.1+
- **Standard patterns** - Follow MCP best practices
- **Tool metadata** - Include version, category, auth requirements
- **Resource URIs** - Follow MCP URI conventions

### 10. Dependency Hygiene

- **Minimal dependencies** - Only essential packages
- **Active maintenance** - All dependencies actively maintained
- **Security audit** - Regular npm audit checks
- **Version pinning** - Use exact versions in production

**Production Dependencies** (5 only):
- `@modelcontextprotocol/sdk`
- `axios`
- `form-data`
- `zod`
- `zod-to-json-schema`

## Development Guidelines

### Code Style

**Naming Conventions**:
- Classes: `PascalCase`
- Functions/Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Interfaces: `PascalCase`
- Files: `kebab-case`

**TypeScript Rules**:
- Strict mode enabled
- No implicit any
- Explicit return types
- Prefer interfaces over types
- Use async/await over promises

**ESLint Compliance**:
- 0 errors
- 0 warnings
- Fix issues, don't suppress (except documented exceptions)

### Commit Guidelines

**Conventional Commits Format**:
```
type(scope): subject

body (optional)

footer (optional)
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Testing changes
- `chore`: Maintenance tasks

**Example**:
```
feat(tools): add table validation tool

Add new tool 'validate_table' that checks table structure
and data integrity before deployment.

Closes #123
```

### Testing Requirements

**Before Submitting**:
- [ ] All tests pass (`npm test`)
- [ ] Linter clean (`npm run lint`)
- [ ] Build successful (`npm run build`)
- [ ] Documentation updated
- [ ] Tests added for new features

**Test Coverage Targets**:
- Critical modules (auth, client, index): >85%
- Utilities: >90%
- Overall project: >80%

### Extension Pattern

**Adding a New Tool** (5 Steps):
1. Define Zod schema in `schemas.ts`
2. Add tool definition to `tools.ts`
3. Add API method to `client.ts` (if needed)
4. Add tool handler in `index.ts`
5. Add tests and examples

**Adding Authentication Method**:
1. Update types in `types.ts`
2. Implement in `auth.ts`
3. Update `getAuthMethod()`
4. Document in `AUTHENTICATION.md`

## Quality Gates

### Pre-Commit
- Code compiles without errors
- ESLint passes with 0 errors/warnings
- All tests pass

### Pre-PR
- Test coverage maintained
- Documentation updated
- Changelog updated (if applicable)
- Examples added (if new feature)

### Pre-Release
- Full test suite passes
- npm audit clean (0 vulnerabilities)
- Documentation complete and reviewed
- Examples tested and verified

## Non-Negotiables

1. **No credentials in code** - Ever. Use environment variables.
2. **No any types** - Use unknown and type guards instead.
3. **No suppressing ESLint** - Fix issues, don't hide them.
4. **No missing tests** - New features require tests.
5. **No incomplete docs** - Public APIs must have JSDoc.
6. **No security vulnerabilities** - npm audit must be clean.
7. **No circular dependencies** - Keep module graph acyclic.
8. **No magic numbers** - Define constants with meaningful names.

## Success Metrics

**Code Quality**:
- TypeScript strict: ✓
- ESLint clean: ✓
- Test coverage: >80%
- Documentation: Complete

**Security**:
- npm audit: 0 vulnerabilities
- Credential sanitization: 100%
- Input validation: All inputs validated

**Performance**:
- Token caching: Implemented
- Connection pooling: Enabled
- Memory leaks: None

**Maintainability**:
- Modular architecture: ✓
- Clear patterns: ✓
- Extension guides: ✓
- No technical debt

## Decision-Making Framework

When faced with design decisions, prioritize in this order:

1. **Security** - Is it safe?
2. **Type Safety** - Is it type-safe?
3. **Simplicity** - Is it simple?
4. **Performance** - Is it fast enough?
5. **Extensibility** - Can it grow?

## Philosophy

> "Make it work, make it right, make it fast - in that order."
>
> "Type safety isn't optional - it's the foundation."
>
> "Security is not a feature - it's a requirement."
>
> "Documentation is for humans - code is for machines."

## Enforcement

This constitution is enforced through:

- **Automated checks** - ESLint, TypeScript, Jest
- **Code review** - All PRs reviewed for compliance
- **CI/CD pipeline** - Gates prevent non-compliant code
- **Documentation** - Guidelines clearly documented

## Evolution

This constitution may evolve as the project grows, but core principles (type safety, security, modularity) are immutable.

**Amendment Process**:
1. Propose change in issue or discussion
2. Document rationale and impact
3. Gather feedback from contributors
4. Update constitution with consensus
5. Update tooling to enforce new rules

---

*Last Updated: 2025-11-13*
*Version: 1.0.0*
*Status: Active*
