# Two bundles changed

Each branch adds a message together with its German translation: master `claim.date.required`, the side branch
`claim.amount.min`, both at the end of `i18n/message.properties` and of `i18n/message_de.properties`. Both
branches also correct the German text of `vehicle.year.max`, each in its own way. The merge answers `conflicts`
on both bundles: one conflict in the English bundle, two in the German one.

Both bundles are resolved by hand in one request, each merged line by line as the Resolve Conflicts dialog puts it
together: both new keys in each bundle, your key first, and their German text of `vehicle.year.max`. Master holds
both bundles so merged. The German bundle is UTF-8 text, and its umlauts come through the merge unchanged.
