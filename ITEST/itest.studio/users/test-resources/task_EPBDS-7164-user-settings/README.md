# The settings of a user stay with the user

Two users change the settings of **My Settings** through `PUT /rest/users/profile`:

- the first changes every setting, the table theme and **Override with Studio theme** included;
- the second reads the defaults, then changes two settings of its own.

Each user reads back only what they saved, and the first user still reads their own settings after the second saved.
