# EPBDS-16670 — the list of tables says the category a table inherits

The tree of a module files a table under its category, and a table that declares none of its own takes the one it
inherits, as the tree of the old editor did. A module gives one to every table of it when the project takes its
module properties from the file names, by a pattern naming `%category%`. The list of tables reports that category in
a field of its own, `category`, while `properties` keeps only what the table declares.

`010-setup` creates a project, opened, whose descriptor names the pattern `%category%`, so its module
`rules/Pricing.xlsx` gives the category `Pricing` to every table of it. Both tables stand on the sheet `Rules`:
`Discount` declares no category, and `Bonus` declares `Bonuses`.

`020-category` — `Discount` is filed under `Pricing`, which it inherits, not under its sheet `Rules`, and `Bonus`
under its own `Bonuses`.
