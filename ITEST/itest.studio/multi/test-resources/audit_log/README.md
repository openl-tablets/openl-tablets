# EPBDS-14601 Audit log

Refused sign-ins of a user, of an unknown personal access token and of an unreadable one are recorded in the audit
log, each with its reason, and so are the sign-in and the sign-out through the login form at the root of the suite.
A refused token is answered with what to do about it; a token of the user sent with a wrong secret is recorded with
the user as its owner. The requests a user
sends with Basic authentication start one session, by `basic`. Only an administrator reads the log, as JSON or as a
CSV file; an export is recorded every time, a view once while nothing else is done. A change of the details of a user
is recorded with the values before and after; a change of the password is recorded without them, and a personal
access token created and deleted by its name. A role on the design repositories granted to the user, changed and
revoked is recorded with the roles before and after, and the change of a role the user may not make is recorded as a
refused request. A project created from an archive and deleted is recorded with the
role its creator got on it and lost with it. A save of two system settings and the save that restores them are
each recorded as one change of the settings, with the values before and after. The saves restart OpenL Studio, which
is recorded, and so are the ends of the session of the login form and of the session of Basic authentication the
restart ended; the next request with Basic authentication starts a session again right after the restart. The start
of OpenL Studio is recorded once, with its version.
