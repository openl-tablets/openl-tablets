# Merging parallel changes of a message bundle

Sync merges a text file line by line, as `git merge` does: changes of the two branches that stand apart are
taken both, and changes of one place are a conflict left to a person. This suite records which changes of
`i18n/message.properties` Sync merges by itself and which ones it leaves to a manual resolution, and resolves
each such conflict by hand.

Every scenario writes the same base bundle on master, branches from it, changes the bundle on the side branch
and on master, and receives the side branch into master.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-different-keys-changed` | Different keys changed | merged automatically |
| `030-keys-added-in-different-places` | Keys added in different places | merged automatically |
| `040-same-key-added-on-both-sides` | Same key added on both sides | merged automatically |
| `050-keys-added-at-the-end` | Keys added at the end | conflict, both keys kept by hand |
| `060-adjacent-keys-changed` | Adjacent keys changed | conflict, both changes typed by hand |
| `070-same-key-changed` | Same key changed | conflict, their text taken by hand |
| `080-two-bundles-changed` | Keys added to two bundles, a German text changed | two bundles in conflict, resolved together |

A conflict here is the expected result, not a defect: the same changes conflict in `git merge`.

A scenario that ends in a conflict reads the bundle merged line by line (`GET /merge/conflicts/text`), the parts
the Resolve Conflicts dialog merges the file in place from, and cancels the merge, which leaves master as it was.
It then receives the side branch again and resolves the conflict by hand, as the dialog does: it sends the bundle
merged line by line, put together from the decisions made, as a `CUSTOM` resolution
(`POST /merge/conflicts/resolve`), and reads the bundle master holds then.
