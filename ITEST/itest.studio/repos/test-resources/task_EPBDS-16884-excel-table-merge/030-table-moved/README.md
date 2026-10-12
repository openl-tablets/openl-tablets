# Table moved in one branch, changed in the other

The side branch inserts FR and IT into `rate`. The empty rows under `rate` are too few for both, so OpenL
Studio moves the table below `minAge`. Master changes the rate of US where the table was. Sync finds the
table at its new place by its header and merges the change of master there.
