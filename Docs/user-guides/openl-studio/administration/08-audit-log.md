### Reviewing the Audit Log

The audit log records who signed in to OpenL Studio and when, whose sign-in was refused, who signed out, and who read
the log itself. OpenL Studio keeps it in its database in every user mode but Single-User, which has no sign-in. Only
users with administrator privileges see it.

In the navigation menu, click **Audit Log**. The page lists the events, the newest first, a page at a time. The list
counts up to 10,000 events: when more match, it shows `10000+` and pages through the newest 10,000. Narrow the
filters to reach older events, or export them.

![](../images/audit-log.png "Audit log of OpenL Studio")

*Audit log of OpenL Studio*

Each event shows the following:

- **ID** — the number of the event, which tells it apart in the log and in an export.
- **Time** — when the event happened, to the second, in the time zone of the browser.
- **User** — who did the action. A refused sign-in names a user only when a user name was typed; a token is never
  recorded in its place. An action OpenL Studio does by itself, such as removing old events, names no user.
- **Signed in with** — how the user signed in, or tried to: **Login form**, **Basic authentication**, **Personal
  access token**, **Bearer token**, **OpenID Connect** or **SAML**. So an action a script or an agent did with a token
  tells so. An action that goes on after its request is answered tells it only for a token, OpenID Connect or SAML.
- **Action** — what was done.
- **Outcome** — **Success**, **Failure** when the action failed, such as a sign-in with a wrong password, or
  **Denied** when it was refused for lack of permission.
- **Object** — what the action was done to.
- **Subject** — the user or group whose access or membership the action changed.
- **Client Address** — the address the request came from. Behind a proxy, it is the address of the client only
  when the server reads the address the proxy forwards, as the OpenL Studio Docker image does.

Click the expand icon of an event to see its details, such as why a sign-in was refused.
The details never hold a password or a token: a personal access token is named by its public identifier.

The log records these actions:

- **Sign in** — a sign-in through the login form, SAML or OpenID Connect. A refused sign-in is recorded for every
  way of signing in, including Basic authentication, a bearer token and a personal access token, with the reason.
  A script or an agent that sends its credentials with every request, such as a personal access token, signs in
  anew every time: its sign-in is recorded at its first request, and again at the first one after its credentials
  were not used for as long as a session may stay idle, 30 minutes by default. A personal access token is named by
  its public identifier.
- **Sign out** — a sign-out, including one the identity provider asks for. A session left idle for longer than it
  may stay ends with a sign-out too, marked as timed out.
- **Audit log read** — a view or an export of the log, with the filters it was made with. Every export is recorded.
  A view is recorded once, when it starts: searching, filtering and turning the pages, with nothing else done in
  between, add no event. Any other action of the user, a new sign-in included, makes the next view recorded again.
- **Old events removed** — the removal of the events older than the retention period, with their number.

#### Finding Events

Click the filter icon in the header of a column to filter the list by it. The list then shows the events that match
the filters of all the columns, from its first page:

- **Time** — the first and the last second of the events, both included. Either one can be left open.
- **User**, **Object**, **Subject** and **Client Address** — text the name or the address contains, in any case.
  Type the text and press Enter; clear it to drop the filter.
- **Signed in with** — how the users who did the actions signed in, such as **Personal access token**.
- **Action** — the actions, each one by itself or all the actions of an area, such as **Sign-in**. Type in the
  search of the filter to find an action.
- **Outcome** — how the actions ended.

While a filter is set, **Clear filters** above the list drops all of them.

#### Exporting the Audit Log

Click **Export CSV** to save every event the filters match as a CSV file, a line per event, its ID first. The time
is written in UTC, and a value a spreadsheet application would run as a formula is written as text. Every export
is recorded in the log, with its filters.

#### Keeping the Audit Log

By default, the events are kept forever. A retention period removes the older events once a day, and a copy of every
event can also go to the console for a log collector. Both are set in `application.properties`, as described in
[Audit Log](../../installation-guide/configuration.md#audit-log) of the configuration guide.
