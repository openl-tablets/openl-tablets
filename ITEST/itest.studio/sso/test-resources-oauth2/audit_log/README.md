# EPBDS-14601 Audit log of bearer tokens

A bearer token Keycloak did not issue is refused and recorded as a failed sign-in by a bearer token. A personal access
token the administrator creates and deletes with a bearer token of Keycloak is recorded as done with a bearer token.
The requests the administrator sends with the bearer token of Keycloak start one session, by `bearer`.
