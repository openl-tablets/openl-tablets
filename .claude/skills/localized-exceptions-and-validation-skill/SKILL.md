---
name: localized-exceptions-and-validation-skill
description: This skill describes how to work with localized error messages for REST exceptions and Spring validators in OpenL Tablets WebStudio.
---
# Localized Exceptions and Validation Messages

This skill describes how to work with localized error messages for REST exceptions and Spring validators in OpenL Tablets WebStudio.

## Overview

OpenL Tablets uses a consistent pattern for localizing error messages:
1. **Validators** use Spring's `Validator` interface with `Errors` object
2. **Exceptions** extend `RestRuntimeException` with `@ResponseStatus` annotation
3. **Messages** are stored in `ValidationMessages.properties`

## Message Key Patterns

### Validator Messages (no status code)

For Spring validators, message keys use the pattern:
```
openl.error.<feature>.<detail>.message
```

Example:
```properties
openl.error.resource.base-path.invalid.message = The base path ''{0}'' is not valid.
openl.error.branch.name.empty.message = The branch name cannot be empty.
```

### Exception Messages (with status code)

For REST exceptions, message keys include the HTTP status code:
```
openl.error.<status_code>.<feature>.<detail>.message
```

The status code is derived from `@ResponseStatus` annotation on the exception class.

Example:
```properties
openl.error.400.resource.base-path.not-folder.message = The base path ''{0}'' is not a folder.
openl.error.404.project.not-found.message = The project ''{0}'' is not found.
openl.error.409.project.locked.message = The project is locked by another user.
```

## Using Messages in Code

### In Validators

Use short keys (without `openl.error.` prefix):

```java
// Field-level error
errors.rejectValue("fieldName", "resource.base-path.invalid.message", new Object[]{basePath}, null);

// Global/object-level error
errors.reject("branch.name.empty.message");

// With message arguments
errors.rejectValue("extensions", "resource.extension.invalid.message", new Object[]{ext}, null);
```

### In Exceptions

Use short keys (without `openl.error.<status>.` prefix):

```java
// NotFoundException (404)
throw new NotFoundException("project.not-found.message", projectName);

// BadRequestException (400)
throw new BadRequestException("resource.base-path.not-folder.message", new Object[]{basePath});

// ConflictException (409)
throw new ConflictException("project.locked.message");
```

## Available Exception Classes

| Exception Class | HTTP Status | Annotation |
|-----------------|-------------|------------|
| `BadRequestException` | 400 | `@ResponseStatus(HttpStatus.BAD_REQUEST)` |
| `ForbiddenException` | 403 | `@ResponseStatus(HttpStatus.FORBIDDEN)` |
| `NotFoundException` | 404 | `@ResponseStatus(HttpStatus.NOT_FOUND)` |
| `ConflictException` | 409 | `@ResponseStatus(HttpStatus.CONFLICT)` |

Location: `org.openl.studio.common.exception`

## Creating a Validator

### 1. Create Validator Class

```java
package org.openl.studio.projects.validator.resource;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class MyModelValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return MyModel.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var model = (MyModel) target;

        if (model.getName() == null) {
            errors.rejectValue("name", "mymodel.name.empty.message");
        }
    }
}
```

### 2. Add Messages to ValidationMessages.properties

```properties
openl.error.mymodel.name.empty.message = The name cannot be empty.
```

### 3. Use in Service with BeanValidationProvider

```java
@Service
public class MyService {

    private final BeanValidationProvider validationProvider;
    private final MyModelValidator myModelValidator;

    public MyService(BeanValidationProvider validationProvider,
                     MyModelValidator myModelValidator) {
        this.validationProvider = validationProvider;
        this.myModelValidator = myModelValidator;
    }

    public void process(MyModel model) {
        validationProvider.validate(model, myModelValidator);
        // If validation fails, ValidationException is thrown automatically
        // ... rest of the logic
    }
}
```

## Message Argument Placeholders

Use `{0}`, `{1}`, etc. for positional arguments, or `''` for literal single quotes:

```properties
openl.error.branch.name.exists.message = Branch ''{0}'' already exists in repository.
openl.error.branch.name.exists.1.message = Cannot create branch ''{0}'' because ''{1}'' already exists.
```

## Files Reference

| File | Purpose |
|------|---------|
| `STUDIO/.../resources/ValidationMessages.properties` | All localized messages |
| `STUDIO/.../common/exception/*.java` | Exception classes |
| `STUDIO/.../common/validation/BeanValidationProvider.java` | Validation executor |
| `STUDIO/.../validator/**/*.java` | Validator implementations |

## Example: Complete Validator Pattern

See `NewBranchValidator` for a complete example using predicate chains:

```java
@Override
public void validate(Object target, Errors errors) {
    Predicate<String> chain = name -> validateNotEmpty(name, errors);
    chain = chain.and(name -> validateFormat(name, errors));
    chain = chain.and(name -> validateUniqueness(name, errors));
    chain.test((String) target);
}

private boolean validateNotEmpty(String name, Errors errors) {
    if (StringUtils.isBlank(name)) {
        errors.reject("branch.name.empty.message");
        return false;  // Stop chain
    }
    return true;  // Continue chain
}
```
