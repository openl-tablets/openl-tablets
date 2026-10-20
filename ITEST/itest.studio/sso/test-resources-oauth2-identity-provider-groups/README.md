# Audit log of the groups of the identity provider

The identity provider gives the user `audit_analyst` the group `audit-analysts` and then takes it away. The group of
OpenL Studio of the same name gives the administrator privilege, and the group holds a role on all the production
repositories, on the design repository and on a project. The audit log names the user as the subject of each: added
to the group and granted the roles when the identity provider reports the group, removed from it and revoked the
roles when it no longer does, each marked as coming from the identity provider.

`OAuthTest.smoke` runs the folders in turn and takes the user out of the group in Keycloak before `030-left`.
