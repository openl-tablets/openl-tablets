# Keys added in different places

Master adds `driver.age.max` after `driver.age.min`, the side branch adds `vehicle.make.required` after
`vehicle.vin.invalid`. The insertions are apart, so the merge answers `success` and the bundle holds both
keys, each where its branch put it.
