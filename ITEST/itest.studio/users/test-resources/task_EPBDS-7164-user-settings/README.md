# The settings of a user stay with the user

Two users change the table, testing and trace settings of their profiles through `PUT /rest/users/profile`:

- the first changes every setting;
- the second reads the defaults, then changes two settings of its own.

Each user reads back only what they saved, and the first user still reads their own settings after the second saved.
The table theme settings were removed: a profile naming one is refused as naming an unknown field.
