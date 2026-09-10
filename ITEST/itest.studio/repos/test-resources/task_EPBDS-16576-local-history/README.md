# Comparing two versions from the local history

This suite verifies that two versions of a module are compared through the Compare API:

- editing a table leaves a version in the local history beside the revision the project was opened at;
- the two versions are compared by the identifiers the local history reports, for the named module;
- the comparison answers with its own identifier and is read, watched and released as any other one;
- a version the history does not hold is refused before a comparison is started.

The comparison reads copies of the versions, so the result stays readable whatever happens to the
history afterwards.
