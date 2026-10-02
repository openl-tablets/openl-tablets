---
title: "OpenL Tablets 6.5.0 Migration Notes"
---

Upgrading to OpenL Tablets 6.5.0 requires no database changes and no Java version change. Two changes need
attention. Groovy moves from `4.0.33` to `6.0.0`, skipping the whole 5.x line, so a project that carries Groovy
sources needs a source-compatibility check; every deployment runs the new Groovy runtime, so the administrator
notes below apply even where no project carries Groovy sources. Separately, the `/web` prefix of the OpenL Studio
API is removed, `/rest` is the only prefix left, and the WebSocket endpoint moves out of it to `/ws` — which affects
everyone who calls that API from outside the browser.

## Rules Authors

* **The OpenL Studio screens need nothing from you for the API prefix change.** They call the new address
  on their own.

* **The Default Order is gone from My Settings.** The module tree opens on the view last picked in the browser, and on
  **By Excel Sheet** until one is picked. A Default Order saved before the upgrade is not used.

* **A condition over a Vocabulary column no longer fails on a value missing from the Vocabulary.** A decision table
  condition compares a Vocabulary column with a value of the base type, such as a `String` argument, in that base
  type, as its expression does. A value that is not in the Vocabulary equals no value of the column, so only a range
  that holds it or a rule whose condition cell is empty matches it. Before, the answer depended on how the table was
  evaluated: most tables stopped the call with `Object '...' is outside of valid domain`, even for a number that lies
  inside a range of the table, while a table with formulas in the condition cells answered as it does now. A table
  that has to reject such a value can end with a rule that leaves the condition cell empty and returns the result of
  the `error()` function.

* **The literal `null` added to or subtracted from a number counts as `0`.** It takes the type of the other operand,
  as an empty value of that type does: `null + 3` is `3` and `null - 3` is `-3`. Before, both were an empty `Date`, so
  `"" + (null + 3)` gave `null` and `null + 3 == 3` gave `false`; a `byte` or `short` operand and `null - 'c'` did the
  same. A character counts as a number here, as in `'c' + 1`, so `null + 'c'` and `'c' + null` are `99` instead of the
  texts `nullc` and `cnull`. Multiplying, dividing and comparing with `null` give the same results as before.

* **The deprecated `%`, `**` and `->` operators are removed.** A rule that still uses one of them no longer
  compiles. Rewrite the expression with a function:

  | Before   | After             |
  |----------|-------------------|
  | `x % y`  | `remainder(x, y)` |
  | `x ** y` | `pow(x, y)`       |

  `remainder` keeps the sign of the dividend, exactly as `%` did. `mod` keeps the sign of the divisor, so it gives a
  different result when the operands have opposite signs. `pow` returns a `double`, while `**` kept the operand
  type. `->` was never backed by an implementation, so no working rule uses it. Percent literals such as `10%` are
  not affected.

* **A number in a formula or a range is written in digits only.** The `$` sign, the thousands separator and the `K`,
  `M`, `B` multipliers are removed, because they read differently across countries and `$` also starts a reference
  to a spreadsheet step. A formula or a range that still uses one of them no longer compiles. Rewrite the number:

  | Before        | After             |
  |---------------|-------------------|
  | `= $600 * 2`  | `= 600 * 2`       |
  | `= 1.5M`      | `= 1500000`       |
  | `[1K .. 10K)` | `[1000 .. 10000)` |
  | `>= $2,500`   | `>= 2500`         |

  Two cases change the result instead of failing, so search the rules for a comma between digits:

  - In a formula, the comma separates values. `{1,500}` is an array of `1` and `500`, and `max(1,000)` compares `1`
    with `0`.
  - In a condition of a smart rule or a simple rule over numbers, `1,000` is the list of `1` and `0`, not `1000`.

  A leading zero makes a whole number octal, as in Java, so `08` and `09` no longer compile: write `Date(2021, 4, 8)`
  instead of `Date(2021, 04, 08)`. Number values in Data and Test tables never accepted these forms and are not
  affected.

* **A percent value in a whole-number cell has to be a whole number.** A text such as `5%` or `250%` in an `Integer`,
  `Long`, `Short` or `Byte` cell no longer compiles, just as the number `2.5` there does not:
  `Cannot parse cell value '250%'. Expected value of type 'Long'.` Before, the fraction was dropped without a warning,
  so `250%` was `2` and `5%` was `0`. A percent value without a fraction loads as before, `300%` as `3`. Use a `Double`
  or `BigDecimal` type to keep the fraction.

* **A named constructor argument takes its value from the rule.** In `new Customer(name = name)` and
  `Customer(name = name)`, the value `name` is the parameter or variable of the rule. Before, a value that had the
  name of a field of the type read that field of the new, still empty object, so `name = name` left the field empty,
  and so did `name = city` with a parameter `city`. A value can no longer read another field of the new object:
  `Customer(name = "Ann", city = name)` fails to compile with `Identifier 'name' is not found.` when the rule has no
  `name` of its own.

* **Named arguments after `new` always set the named fields.** `new Customer(city = city, name = name)` sets the
  fields as named, as `Customer(city = city, name = name)` does, and leaves the rule variables `name` and `city`
  unchanged. Before, when the rule had variables with the names of the fields and the type had a constructor for
  their types, the values went to that constructor in the written order: the fields could be swapped, `name = city`
  overwrote the variable `name`, and `new Box(city = city)` compiled for a `Box` without a `city` field. Arguments
  that are all `field = value` pairs are no longer assignments passed by position, so `new BigDecimal(x = 5)` with a
  variable `x` fails to compile; assign `x` before the call.

* **A source-compatibility check is needed only for a project with a `groovy/` folder.** Rules in Excel are not
  compiled by Groovy, so they need no re-save and no re-compile for the language changes below.

* **A static member can no longer be called through a parameterized type.** Groovy 6 rejects what Groovy 4 accepted,
  applying the rule Java has always had:

  ```groovy
  Literal<Node> x = Literal<Node>.of(true)   // no longer compiles
  Literal<Node> x = Literal.of(true)         // write this instead
  ```

  Groovy reports `Cannot refer to a static member of a generic type through a parameterization`. Removing the type
  argument from the call is the whole fix — the variable keeps its declared type and behavior does not change.

* **Recognize the symptom.** A Groovy class that fails to compile surfaces as a missing type rather than as a
  compilation error. The module reports `Cannot load type: <fully qualified class name>`, and a service built from
  that project fails to deploy. Compiling the project's Groovy sources directly against Groovy 6 shows the real
  message.

> [!Note]
> The rule above is the change OpenL's own projects had to adapt to. Groovy 6 carries further changes over the 4.x
> line, and the 5.x line is skipped entirely, so consult the Groovy release notes before upgrading a project that
> leans on less common language features.

## Developers

* **Replace `/web/` with `/rest/` in every client.** The path after the prefix is unchanged, so
  `/web/projects/{id}/files` becomes `/rest/projects/{id}/files`. There is no redirect and no compatibility
  period.
* **A check for a `2xx` is not enough while you migrate.** A `GET /web/...` no longer fails — the address falls
  through to the page the application is drawn on, so the response is `200` with an HTML body. A client that
  only tests the status code will parse that page as JSON. A non-GET answers `405`. Search your clients for
  the literal `/web` rather than relying on error handling to surface the change.
* **No client needs new credentials.** `/rest` accepts everything `/web` did and more: the session cookie in
  every mode, a Personal Access Token in all multi-user modes, HTTP Basic in `ad` and `multi`, and a Bearer
  token in `oauth2`.
* **The WebSocket endpoint is `{context}/ws`, and it is authenticated.** `/web/ws` and `/rest/ws` are gone. The
  handshake is a protocol of its own, so it has an address of its own, apart from the REST API. `/web/ws` used to
  admit an anonymous handshake, which is what made the public notification topic readable without signing in;
  that is no longer the case. Connect with the session cookie the browser already holds, or send an
  `Authorization` header on the handshake, as the REST API has always accepted.
* **In `oauth2` mode an unauthenticated API call now answers `401` with `WWW-Authenticate: Bearer`** instead
  of a bare `401`. A `Bearer` challenge raises no browser credential dialog, so a browser client is
  unaffected; a scripted client that inspects the header should expect it.
* **`POST /rest/admin/tag-config/fill` answers what the fill did to each project.** It used to answer
  `{"updated": N, "skipped": M}`. It now answers a list with one entry for every project it was asked for that a
  project name template derives a missing tag value for: the project name, the outcome, the tag values the
  project got and the ones it could not get, and why a project was left alone. A script that reads the two
  counters has to count the entries by their `outcome` instead.
* **A files-API write into a project that cannot be changed now names the reason.** A write under
  `/rest/projects/{id}/files` into a project locked by a user, or on a protected branch, used to answer `409`
  with the code `openl.error.409.project.status.update.failed.message`. It now answers `409` with
  `openl.error.409.file.project.locked.message`, naming the project and the user, or with
  `openl.error.409.file.project.branch.protected.message`, naming the project and the branch.
* **`treeView` and `profiles` are gone from `/rest/users/profile`.** `GET` no longer returns them, and a `PUT` that
  still sends `treeView` answers `400` with `Unknown field 'treeView'`. Drop the field from the body.
* **A `PUT /rest/users/profile` that leaves a field out keeps the stored value**, for a name, the e-mail and the display
  name as for a setting. It used to clear a missing name, refuse a missing e-mail or display name, save a missing flag
  as `false` and a missing `testsPerPage` or `testsFailuresPerTest` as `0`, and answer `500` on a missing `treeView`
  after the names and the e-mail were already saved. An e-mail or a display name that is sent still cannot be empty,
  and a request that fails now changes nothing.
* **`testsPerPage` and `testsFailuresPerTest` take `-1` for all, or a positive number.** Any other value answers
  `400`.

## Administrators

* **Groovy 6 requires Java 17 or later**, which OpenL Tablets already exceeds — it requires Java 21. No JDK change
  is needed.

* **Groovy 6 occupies more heap at rest than Groovy 4.** A deployment whose maximum heap is tuned close to its
  previous usage should re-measure before upgrading. For scale: the memory-constrained suite in OpenL's own build
  runs under a 61 MB cap and needed 2 MB more to pass on Groovy 6.

* **`groovy.use.classvalue` is now a mode, not a flag.** Groovy 6 reads `true` (the default) and `soft` as keeping
  the class-metadata cache backed by `java.lang.ClassValue`; any other value selects the map-based cache. Setting it
  to `false` keeps `ClassValue` out of the cache, which is what avoids the classloader pinning behind the metaspace
  leak of GROOVY-12142; the `soft` mode added by GROOVY-12281 keeps `ClassValue` but lets its entries be reclaimed.
  Groovy 5 ignored the property altogether, so a deployment that set it while passing through 5.x on its own should
  confirm the value again.

* **`rules.tree.view.default` and `rules.tree.view` are no longer read.** A default order set for all users in the
  application properties has no effect; remove it.

* **Repoint anything that routes or allows `/web`.** Check reverse-proxy location blocks, ingress rules, API
  gateway routes, WAF path rules and the `cors.allowed.origins` consumers for `/web`, and change them to
  `/rest`. A proxy that forwards `/web/ws` or `/rest/ws` for the WebSocket must forward `/ws` instead, with the
  upgrade headers and an idle timeout long enough for a connection that stays open. It no longer shares a location
  with `/rest`.

* **An e-mail verification link deployed under a context path containing `web` is fixed.** With the default
  `/webstudio` context path the link previously lost that path and did not resolve. No action is required
  beyond upgrading; a link sent by an earlier version stays broken.

## Testing Recommendations

After upgrading, verify in a non-production environment:

1. Every project that carries a `groovy/` folder compiles and deploys.
2. A service published from such a project answers as it did before.
3. Heap headroom, if the maximum heap is set close to the previous usage.
4. A client that calls the OpenL Studio API answers on `/rest`, and its WebSocket connects to `/ws`.
