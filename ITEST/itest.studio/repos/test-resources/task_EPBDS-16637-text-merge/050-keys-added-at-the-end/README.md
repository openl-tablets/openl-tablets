# Keys added at the end

Each branch appends a key of its own at the end of the bundle - the usual way a new message is added. Both
insert at the same place, so the line-level merge cannot tell which goes first: the merge answers
`conflicts`, the same as `git merge` does, and the bundle needs a manual resolution. Cancelling the merge
leaves master as it was.

This is the expected behaviour, not a defect.

Merged line by line, the bundle is the lines both branches left as they were, then the conflict of the two keys,
with no base lines, since each branch added its own key. Received again, the conflict is resolved by keeping both
keys, your key first, as **Take Yours** and then **Add Theirs** of the merge window leave them, and master holds
both keys.
