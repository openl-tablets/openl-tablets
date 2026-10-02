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

* **An infinite number matches only the same infinity in a decision table condition.** In a condition over `Double`,
  `double`, `Float` or `float` values, the input `Infinity` or `-Infinity` matches a rule only when its condition cell
  holds that infinity or is empty, as `==` compares them. Before, it matched the rule of a number of the column, such
  as `0`, and a condition cell with `Infinity` could also match `-Infinity` or a number.

* **A `DoubleRange` condition matches exactly the numbers its range holds.** A range without an upper limit, such as
  `> 10`, `10+` or `more than 10`, matches `Infinity`. A negative bound keeps the number next to it on the correct
  side: `(-1; 0)` matches `-0.9999999999999999`, while `[-2; -1]` and `-1` do not. Before, `Infinity` matched only a
  rule whose condition cell is empty. At a negative bound that is a power of two, such as `-1` or `-4`, the number
  next to the bound fell on the wrong side.

* **`min` returns NaN when one of the values is NaN, as `max` does.** A NaN value makes the result of both functions
  NaN, whatever its position among the values or in an array of `Double`, `double`, `Float` or `float` values:
  `min(Double.NaN, 1.5)` and `min(new Double[] {3, Double.NaN, 1})` are `NaN`. Before, `min` skipped NaN and returned
  `1.5` and `1.0`. Empty values are still skipped. To skip NaN as before, filter it out first, as in
  `min(values[(v) @ !isNaN(v)])`.

* **The literal `null` added to or subtracted from a number counts as `0`.** It takes the type of the other operand,
  as an empty value of that type does: `null + 3` is `3` and `null - 3` is `-3`. Before, both were an empty `Date`, so
  `"" + (null + 3)` gave `null` and `null + 3 == 3` gave `false`; a `byte` or `short` operand and `null - 'c'` did the
  same. A character counts as a number here, as in `'c' + 1`, so `null + 'c'` and `'c' + null` are `99` instead of the
  texts `nullc` and `cnull`. Multiplying, dividing and comparing with `null` give the same results as before.

* **A percent value equals the fraction it stands for.** `0.07%` is `0.0007` and `99.99%` is `0.9999`, as the
  fractions written out are, both as a percent literal in a formula and as a text in a `Double` cell or in a
  Spreadsheet cell without a type. Before, they were `0.0007000000000000001` and `0.9998999999999999`, so a result
  computed from such a value can change in its last digit. A `BigDecimal` cell already read the exact fraction, and a
  `Float` cell reads the same value as before. A percent literal also takes any number of decimals and an exponent:
  `0.5%`, `12.345%` and `1.5e2%` compile, while before it had no decimals or exactly two.

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

* **The functions `format`, `dateToString`, `stringToDate`, `parseFormattedDouble`, `addIgnoreNull` and
  `addArrayElementIgnoreNull` are removed.** A rule that still calls one of them no longer compiles, for example with
  `Method 'format(java.lang.Double)' is not found.` Rewrite the call:

  - **`format(amount)`** — `toString(amount, "#,##0.00")`
  - **`format(amount, pattern)`** — `toString(amount, pattern)`
  - **`format(date)`, `dateToString(date)`** — `toString(date, "M/d/yy")`
  - **`format(date, pattern)`, `dateToString(date, pattern)`** — `toString(date, pattern)`
  - **`stringToDate(text)`** — `toDate(text)`
  - **`parseFormattedDouble(text)`** — `toNumber(text)`
  - **`addIgnoreNull(array, element)`** — `element == null ? array : add(array, element)`
  - **`addIgnoreNull(array, index, element)`** — `element == null ? array : addElement(array, index, element)`

  `addArrayElementIgnoreNull` takes the same arguments as `addIgnoreNull` and is rewritten the same way. The
  `DEFAULT_DOUBLE_FORMAT` constant is removed with them, so write its pattern `"#,##0.00"` instead. Check the
  rewritten rules for these differences:

  - The removed functions wrote and read numbers and dates in the language of the server. The replacements always use
    US English: a comma groups the digits, a point starts the fraction and month names are English. On a German server,
    `format(1234.5)` gave `1.234,50`, while `toString(1234.5, "#,##0.00")` gives `1,234.50`.
  - `toString(date)` without a pattern gives `04/30/2015`, where `format(date)` gave `4/30/15`. The pattern `M/d/yy`
    keeps the short form.
  - A replacement returns an empty value where the removed function stopped the call with an error. This is the case
    for `toString(amount, pattern)` with an invalid pattern, and for `toDate` and `toNumber` with a text they cannot
    read. `toDate` and `toNumber` also need the whole text to be a date or a number: `toNumber("12abc")` is empty,
    while `parseFormattedDouble("12abc")` read `12`.
  - `parseFormattedDouble(text, pattern)` has no direct replacement. `toNumber` reads the comma that groups the
    digits. Remove other symbols of the pattern first, such as a currency sign: `toNumber(replace(text, "$", ""))`.

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

* **A text cell reads an exponent with a small `e`.** The letter case no longer matters for any number type, as in a
  formula: `1.5e3` is `1500` like `1.5E3`, and `1e3` is `1000` like `1E3`. Before, the small letter failed to compile
  with `Cannot parse cell value '1.5e3'`. Only a Spreadsheet cell without a type calculates differently: its text
  `1e3` is now the `Double` `1000.0` instead of the text `1e3`. Give such a step the type `String`, as in
  `Code : String`, to keep the text.

* **`toNumber` reads an exponent with a small `e`.** `toNumber("1.5e3")` is `1500`, like `toNumber("1.5E3")`.
  Before, it was `null`, although `isNumeric("1.5e3")` was already `true`, so a rule that checks the result of
  `toNumber` for `null` now gets the number for such a text.

* **`like` checks an empty text against the pattern, and `@` is a letter of any alphabet.** An empty or `null` text
  matches a pattern that needs no characters: `like("", "*")` and `like(null, "*")` are `true`, while before an empty
  text matched only an empty pattern. `@` matches a letter such as `Ø`, `ß` or `Д`, so `like("Øre", "@@@")` and
  `like("Мова", "@+")` are `true`. Before, `@` matched only the letters A to Z; a rule that has to keep that limit
  writes the set `[A-Za-z]` instead of `@`.

* **`textSplit` cuts the text at every whole separator.** A separator that starts inside a partial match of it is
  no longer missed: `textSplit(", ", "a,, b")` is `["a,", "b"]` and `textSplit("ab", "aab")` is `["a"]`, where
  before the text came back whole. The start of a separator at the end of the text stays in the last part:
  `textSplit(", ", "a, b,")` is `["a", "b,"]` instead of `["a", "b"]`. An empty text has no parts, so
  `textSplit(",", "")` is an empty array instead of an array with one empty text. With a separator of one character,
  or none, only an empty text gives a different result.

* **`toDate` needs the whole text to be the date.** More text after the date gives an empty value instead of the
  date read from the start: `toDate("04/30/2015abc")`, `toDate("15.03.2024x", "dd.MM.yyyy")` and
  `toDate("2024-03-15T10:20")` are empty. Blanks around the date are still ignored. For a text with a time, read the
  time with the pattern, as in `toDate(text, "yyyy-MM-dd'T'HH:mm")`, or cut the date out, as in
  `toDate(substring(text, 0, 10))`. An empty pattern reads the forms `toDate(text)` reads: `toDate("2024-03-15", "")`
  is the date instead of empty, and the two-digit year of `toDate("01/11/12", "")` is 2012 instead of the year 12.

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
* **A run whose `runtimeContext` picks a table version that does not compile now answers `409`.**
  `POST /rest/projects/{id}/run` used to answer `202` and then a result with no value and no errors. It now answers
  `409` with `openl.error.409.run.dispatched.compile.errors.message`, naming the first error of that version. The
  check covers only the version the requested table is dispatched to: a table it calls is not checked, and a table
  with a parameter filled in from the runtime context is skipped.
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
* **`POST /rest/projects/{id}/trace` refuses a table that does not compile.** It used to start the session for a table
  with a compilation error, taking an unparsable cell as empty, so the trace completed with a result computed from the
  rest of the table, and it answered `404` for a table whose header did not parse. It now answers `409` with
  `openl.error.409.trace.table.compile.errors.message`, naming the first error. The errors are read from the whole
  project, or from the module named by `fromModule`, and a test table is refused for errors in the tables it tests
  as well. A table that compiles within its own module but not with the rest of the project traces with
  `fromModule`, as the Trace button of the editor does. A table with an expression that does not compile used to halt
  on the failing step under `breakOnErrors`; it is refused with the same `409` now, so a client that traced it to find
  the error reads it from the answer. A table that only calls a table that does not compile is still traced and halts
  on the failing step.
* **`treeView` and `profiles` are gone from `/rest/users/profile`.** `GET` no longer returns them, and a `PUT` that
  still sends `treeView` answers `400` with `Unknown field 'treeView'`. Drop the field from the body.
* **A `PUT /rest/users/profile` that leaves a field out keeps the stored value**, for a name, the e-mail and the display
  name as for a setting. It used to clear a missing name, refuse a missing e-mail or display name, save a missing flag
  as `false` and a missing `testsPerPage` or `testsFailuresPerTest` as `0`, and answer `500` on a missing `treeView`
  after the names and the e-mail were already saved. An e-mail or a display name that is sent still cannot be empty,
  and a request that fails now changes nothing.
* **`testsPerPage` and `testsFailuresPerTest` take `-1` for all, or a positive number.** Any other value answers
  `400`.
* **Java and Groovy code loses the same functions.** `RulesUtils.format`, `dateToString`, `stringToDate`,
  `parseFormattedDouble`, `addIgnoreNull`, `addArrayElementIgnoreNull` and `DEFAULT_DOUBLE_FORMAT` are removed, and so
  is `DateTool.dateToString`. The rules functions of `org.openl.rules.util` are not meant for Java code, so call what
  the removed functions called. These calls keep the old results, including the language of the server, except that
  the removed date functions returned `null` for a `null` date:
  - **`format(number, pattern)`** — `new DecimalFormat(pattern).format(number)`, with `"#,##0.00"` when there was no
    pattern
  - **`format(date, pattern)`, `dateToString(date, pattern)`** — `new SimpleDateFormat(pattern).format(date)`
  - **`format(date)`, `dateToString(date)`** — `DateFormat.getDateInstance(DateFormat.SHORT).format(date)`
  - **`stringToDate(text)`** — `DateFormat.getDateInstance(DateFormat.SHORT).parse(text)`
  - **`parseFormattedDouble(text, pattern)`** — `new DecimalFormat(pattern).parse(text).doubleValue()`, with
    `"#,##0.00"` when there was no pattern
  - **`addIgnoreNull(array, element)`, `addArrayElementIgnoreNull(array, element)`** —
    `element == null ? array : ArrayUtils.add(array, element)` with `ArrayUtils` of Apache Commons Lang; the form with
    an index passes it as the second argument of `ArrayUtils.add`

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
