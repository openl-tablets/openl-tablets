# Unauthenticated reads the login screen depends on

The login screen is drawn before anyone has signed in, and it reads two endpoints while doing so. Both are
listed in `SecurityConfig.staticResourcesFilterChain`, which runs no security filters at all.

These steps live in a folder of their own because the suite resets cookies at a folder boundary: at the suite
root they would ride the administrator's session and pass without proving anything. `010` is the guard — it
shows that a `/rest` call really is unauthenticated here, so the `200`s that follow mean the whitelist, not a
leaked session.

`020` carries no `.resp`: the framework then expects `200` and ignores the body. `030` checks the body, because
the OpenL AI plugin reads `openl.version` from it without signing in to pick its MCP server and to warn about an
incompatible OpenL Studio. Wildcards stand for every value but the shape of `openl.version`, so the step checks
the set of fields and the major.minor.patch form of the version, not the running version itself.
