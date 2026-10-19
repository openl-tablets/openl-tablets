# EPBDS-14601 Audit log

Refused sign-ins of a user, of an unknown personal access token and of an unreadable one are recorded in the audit
log, and so are the sign-in and the sign-out through the login form at the root of the suite. Only an administrator
reads the log, as JSON or as a CSV file; an export is recorded every time, a view once while nothing else is done. A
change of the details and of the password of a user, and a personal access token created and deleted, are recorded
with the values before and after. So are a
role on the design repositories granted to the user, changed and revoked, and the change of a role the user may not
make is recorded as a refused request. A project created from an archive and deleted is recorded with the
role its creator got on it and lost with it.
