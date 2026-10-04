# The files of the user guides are public

A file of the user guides — a page source, an image, the table of contents — is a static resource, so
`SecurityConfig.staticResourcesFilterChain` runs no security filters on it, as on `/assets`. A page of a guide is
the application page, guarded as every other page: without a session it is sent to the login page.

An address is a file when its last part ends with an extension. `040` shows that a query cannot make a page look like
a file.

The steps live in a folder of their own because the suite resets cookies at a folder boundary: at the suite root they
would ride the administrator's session, and the `200`s would prove nothing.
