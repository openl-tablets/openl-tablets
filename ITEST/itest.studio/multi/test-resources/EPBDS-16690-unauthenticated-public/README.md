# Unauthenticated reads the login screen depends on

The login screen is drawn before anyone has signed in, and it reads two endpoints while doing so. Both are
listed in `SecurityConfig.staticResourcesFilterChain`, which runs no security filters at all.

These steps live in a folder of their own because the suite resets cookies at a folder boundary: at the suite
root they would ride the administrator's session and pass without proving anything. `010` is the guard — it
shows that a `/rest` call really is unauthenticated here, so the `200`s that follow mean the whitelist, not a
leaked session.

`020` and `030` carry no `.resp`: the framework then expects `200` and ignores the body, which is what we want
for a payload that names the running version.
