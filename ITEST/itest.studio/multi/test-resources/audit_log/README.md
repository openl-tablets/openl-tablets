# EPBDS-14601 Audit log

Refused sign-ins of a user, of an unknown personal access token and of an unreadable one are recorded in the audit
log, and so are the sign-in and the sign-out through the login form at the root of the suite. Only an administrator
reads the log, as JSON or as a CSV file; an export is recorded every time, a view once while nothing else is done.
