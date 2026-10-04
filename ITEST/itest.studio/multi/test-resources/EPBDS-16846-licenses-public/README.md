# The lists of third-party libraries are public

The About dialog reads `/licenses/frontend-licenses.json` and `/licenses/backend-licenses.json`, files the build leaves
beside the pages. Like every static resource, `/licenses` is listed in `SecurityConfig.staticResourcesFilterChain`,
which runs no security filters at all.

The steps live in a folder of their own because the suite resets cookies at a folder boundary: at the suite root they
would ride the administrator's session, and the `200`s would prove nothing.
