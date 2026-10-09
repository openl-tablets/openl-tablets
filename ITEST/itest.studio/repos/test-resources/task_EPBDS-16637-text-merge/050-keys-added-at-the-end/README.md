# Keys added at the end

Each branch appends a key of its own at the end of the bundle - the usual way a new message is added. Both
insert at the same place, so the line-level merge cannot tell which goes first: the merge answers
`conflicts`, the same as `git merge` does, and the bundle needs a manual resolution. Cancelling the merge
leaves master as it was.

This is the expected behaviour, not a defect.
