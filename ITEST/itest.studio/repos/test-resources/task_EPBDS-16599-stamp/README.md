# EPBDS-16599 — What OpenL Studio notes about an edit

An installation can be asked to record who last changed each table and when (`update.system.properties`, off
by default). The Editor wrote that note on every save; here it was written only when the table's properties
were the thing being written, so a table changed through its cells carried a note about some earlier edit.

The suite turns the recording on, writes a cell through the raw-source actions, and reads the table back: the
note rides in the same save as the change, so the table comes back carrying both. The recording is turned off
again in the teardown — the setting belongs to the whole installation, and every other suite expects it off.
