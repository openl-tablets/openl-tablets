# The removed settings of the profile are refused

OpenL Studio keeps **My Settings** in the browser, so the profile no longer carries a setting. A user who sends
`PUT /rest/users/profile` with a setting the profile once had is refused with 400, naming the field:

- the table theme, removed by EPBDS-7164;
- the table, testing and trace settings, removed by EPBDS-16996.
