# Cross-site protection of the sign-in form

`POST /login` is guarded like the API: a page on another site cannot sign the browser into an account of its
choosing (login CSRF). A same-origin sign-in still reaches the form, and the single sign-on callback under
`/login/...` is outside the mapping — the security chain answers it, not the filter.

The probes never sign in successfully, only a rejected attempt and a failed one, so the suite's session state is
untouched. They live in their own folder because even a failed sign-in issues a session cookie, and at the suite
root that cookie would displace the administrator's session the login sequence relies on.
