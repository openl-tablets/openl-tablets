# Adjacent keys changed

Master rewords `driver.age.min`, the side branch rewords `driver.license.required` on the next line. No
unchanged line stands between the two changes, so they form one hunk: the merge answers `conflicts`, the same
as `git merge` does, and the bundle needs a manual resolution.

This is the expected behaviour, not a defect.
