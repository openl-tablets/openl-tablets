# Context root of a webapp deployed under a context path

OpenL Studio is deployed under a context path, so its context root answers on two URLs. Requesting it **without**
the trailing slash reaches the static resources servlet with no path info at all, which is the case of
[EPBDS-16429](https://jira.eisgroup.com/browse/EPBDS-16429): the servlet answered HTTP 500 with
`NullPointerException: Cannot invoke "String.startsWith(String)" because "path" is null`.

- `010-context-root-without-slash` — `GET /openl-studio` serves the index page
- `020-context-root-with-slash` — `GET /openl-studio/` serves the same page
- `030-static-resource` — a static resource is still forwarded to the container's default servlet

Every page asserts the base href the servlet rewrites, so a page served by anything other than this servlet fails
the test.
