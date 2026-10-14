# Same key changed

Both branches reword `vehicle.year.max` differently. Only a person can choose the text, so the merge answers
`conflicts` and the bundle needs a manual resolution.

This is the expected behaviour, not a defect.

Merged line by line, the conflict is the one line of `vehicle.year.max` in every version. Received again, the
conflict is resolved by taking their text, **Take Theirs** of the merge window, which leaves their version of the
bundle on master.
