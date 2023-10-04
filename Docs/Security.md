## Enabling Security in the OpenL Tablets Rule Services Web Service

OpenL Tablets Rule Services web service comes with the simple implementation of the OAuth2 authentication.
To enable it, define the following properties:

```properties
ruleservice.authentication.enabled = true
ruleservice.authentication.iss = https://accounts.google.com
ruleservice.authentication.jwks = https://www.googleapis.com/oauth2/v3/certs
#ruleservice.authentication.aud = https://openl-tablets.org
```

In case when the default security does not meet the requirements, a custom implementation can be added as follows:

```java
package org.openl.rules.ruleservice.spring;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Component
@Order(2)
public class BasicAuthorizationChecker implements AuthorizationChecker {

    @Override
    public boolean authorize(HttpServletRequest request) {
        var basicRealm = request.getHeader("Authorization");
        var path = request.getPathInfo();
        return isRequiredAuthorization(path) && isValidRealm(basicRealm);
    }

}
```

There can be several Spring beans. The order of authorization can be defined by the `@Order` annotation.
The authorization is successful if any of checkers returns `true`.
A custom access denied handler can be registered to be called when no checkers return `true`.

```java
package org.openl.rules.ruleservice.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AccessDeniedHandler;

@Order(0)
@Component
public class BasicAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
```
