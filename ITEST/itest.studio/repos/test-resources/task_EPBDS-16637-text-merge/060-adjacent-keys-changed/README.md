# Adjacent keys changed

Master rewords `driver.age.min`, the side branch rewords `driver.license.required` on the next line. No
unchanged line stands between the two changes, so they form one hunk: the merge answers `conflicts`, the same
as `git merge` does, and the bundle needs a manual resolution.

This is the expected behaviour, not a defect.

Merged line by line, the conflict holds both keys in every version. Received again, the conflict is resolved by
typing: your text of `driver.age.min` and their text of `driver.license.required`, as the merge window leaves them
once your lines are taken and the second one is typed over. Master holds both changes.
