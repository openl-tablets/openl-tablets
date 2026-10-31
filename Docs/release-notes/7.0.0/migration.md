---
title: "OpenL Tablets 7.0.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.0.0 requires no database changes and no Java version change. Three changes need
attention. Groovy moves from `4.0.33` to `6.0.0`, skipping the whole 5.x line, so a project that carries Groovy
sources needs a source-compatibility check; every deployment runs the new Groovy runtime, so the administrator
notes below apply even where no project carries Groovy sources. Separately, the `/web` prefix of the OpenL Studio
API is removed, `/rest` is the only prefix left, and the WebSocket endpoint moves out of it to `/ws` — which affects
everyone who calls that API from outside the browser. Finally, the WAR files are no longer published to Maven
Central: they are the assets of the GitHub release, which affects every build and deployment that fetches them
from Maven.

## Rules Authors

* **The OpenL Studio screens need nothing from you for the API prefix change.** They call the new address
  on their own.

* **A bookmark of a project from OpenL Studio 6.4 opens nothing.** A project is addressed by a new id, so an address
  saved before the upgrade, and one that names a project by its name, leads to no project. Open the project from the
  **Projects** tab and bookmark it again.

* **My Settings are kept in the browser and open from the header.** The settings icon beside the theme icon opens
  them, and a change applies at once. They are no longer saved in the user profile, so each browser starts from the
  defaults, and the settings saved before the upgrade are not used. The user icon lists **Personal Access Tokens**
  right under **My Profile** instead.

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

* **Arithmetic on `Double` values computes with the decimals the numbers show.** The operators `+`, `-`, `*`, `/`,
  `%`, `++`, `--` and their assignments, and the functions `remainder`, `quotient`, `floorDiv` and `mod`, round the
  exact result of the written numbers once to the nearest `Double`: `0.1 + 0.2` is `0.3`, `1.15 * 100` is `115` and
  `1.1 % 0.1` is `0`. Before, they computed in binary and gave `0.30000000000000004`, `114.99999999999999` and
  `2.7755575615628914E-17`. A test that expects such a binary result, or compares it with `====`, needs the decimal
  one. A number of 16 or 17 digits with a fraction is a computed one, such as the quotient `1 / 3`: it counts as its
  `Double`, and a result of it within the rounding it carries of a shorter decimal is that decimal. So `1 / 3 * 3` is
  `1.0` and `9 / 35 * 7` is `1.8`, where binary gave `1.0` and `1.7999999999999998`. `quotient` and `floorDiv` agree
  with `remainder` and `mod`: a dividend within a rounding of a multiple of the divisor counts as that multiple, and
  `mod` of a multiple is `0.0`, not `-0.0`. `Float` values still compute in binary.

* **`sum`, `avg` and `product` of `Double` values compute with the decimals the numbers show and round once.** Their
  result is the `Double` nearest to the exact total: `sum(0.1, 0.2)` is `0.3`, and ten times `0.1` sum to `1`. Before,
  they rounded after every value and gave `0.30000000000000004` and `0.9999999999999999`. A computed number counts as
  its `Double`, as in the operators, so `sum(1 / 3, 1 / 3, 1 / 3)` is `1`. A result rounded once can
  differ from adding the values one by one with `+`: `sum(1e16, 1, 1, 1, 1)` is `1.0000000000000004E16`. A `product`
  of finite values with a zero is zero, also when the other values overflow, where it gave `NaN`. `median` averages
  its two middle values the same way: the median of `0.1` and `0.2` is `0.15`, where it was `0.15000000000000002`.
  `varS`, `varP`, `stdevS`, `stdevP`, `covarS`, `covarP`, `correl`, `rsq`, `slope`, `intercept` and `forecast` of
  `Double` values take this average, so their results can change in the last digits. `Float` values still compute in
  binary.

* **A test compares a `BigDecimal` result with all its digits.** Without a precision, the expected value must equal
  the result, and with a precision `(n)` their difference may be up to `1E-n`, computed without rounding. Before, both
  were rounded to a `Double`, so a test passed when they differed after about the 16th digit: `0.6666666666666667`
  matched two thirds, which is `0.6666666666666666666666666666666667` as a `BigDecimal`. Write the expected value
  with all its digits, or give the column a precision, such as `_res_ (10)`. A `BigInteger` result with a precision
  is compared with all its digits too. A `Double` or `Float` result is still compared in binary, so its difference
  can be a little larger than it looks: with `(1)`, `0.4` does not match `0.3`. For such a result, `(5)` allows the
  `Double` nearest to `0.00001` now, where it allowed only `9.999999999999999E-6`, so `0.00001` did not match `0`.

* **`min`, `small`, `big` and `median` return NaN when one of the values is NaN, as `max` does.** This holds
  wherever NaN stands among the values or in an array of `Double`, `double`, `Float` or `float` values. Empty values
  are still skipped. Before:

  - `min` skipped NaN: `min(Double.NaN, 1.5)` was `1.5`, and `min(new Double[] {3, Double.NaN, 1})` was `1.0`.
  - `small`, `big` and `median` counted NaN as the largest number. For `values = new Double[] {3, Double.NaN, 1}`,
    `small(values, 1)` was `1.0`, `big(values, 2)` was `3.0` and `median(values)` was `3.0`, while `median` of one or
    two values already returned NaN.

  To leave NaN out, filter it out first, as in `min(values[(v) @ !isNaN(v)])`.

* **A `DateRange` accepts ISO dates.** A date range in a cell or in `new DateRange(...)` reads `2024-12-31`, with an
  optional time `2024-12-31 23:59:59` or `2024-12-31T23:59:59`, in every range form, such as
  `[2024-01-01; 2024-07-01)`, `2024-01-01 - 2024-12-31` or `>= 2024-01-01`. The US form `12/31/2024` keeps working,
  also next to an ISO date in the same range. Before, only the US form worked: a `DateRange` cell with an ISO date,
  and a condition of a smart rule or a simple rule over a `Date` parameter with an ISO range or an ISO date and time,
  failed to compile with `Cannot parse cell value`, and `new DateRange("2024-12-31")` stopped the call with an error.
  A date in a range is still the local date and time of the server, so a range written with US dates matches the
  same dates as before.

* **The literal `null` added to or subtracted from a number counts as `0`.** It takes the type of the other operand,
  as an empty value of that type does: `null + 3` is `3` and `null - 3` is `-3`. Before, both were an empty `Date`, so
  `"" + (null + 3)` gave `null` and `null + 3 == 3` gave `false`; a `byte` or `short` operand and `null - 'c'` did the
  same. A character counts as a number here, as in `'c' + 1`, so `null + 'c'` and `'c' + null` are `99` instead of the
  texts `nullc` and `cnull`. Multiplying, dividing and comparing with `null` give the same results as before.

* **A math function of an empty value is empty.** `abs`, `acos`, `asin`, `atan`, `atan2`, `cbrt`, `ceil`,
  `copySign`, `cos`, `cosh`, `exp`, `expm1`, `floor`, `getExponent`, `IEEEremainder`, `log`, `log10`, `log1p`,
  `nextAfter`, `pow`, `rint`, `scalb`, `signum`, `sin`, `sinh`, `sqrt`, `tan`, `tanh`, `toDegrees`, `toRadians` and
  `ulp` return an empty value when an argument is empty, as `round(v)`, `quotient(v, 2)` and `mod(v, 2)` already did.
  Before, they computed with `0` in place of the empty value: `abs(v)` and `sqrt(v)` were `0`, `cos(v)` was `1`,
  `log(v)` was `-Infinity`, `pow(v, 2)` was `0` and `pow(2, v)` was `2`, while `pow(v, v)` stopped the call with an
  error. A rule that needs a number checks the value first, for example `v == null ? 0 : sqrt(v)`. A value of a
  primitive type, such as a `double` parameter, is never empty, so its results do not change. The functions return
  wrapper types, so a Spreadsheet step that holds such a result is a `Double` instead of a `double`.

* **A percent value equals the fraction it stands for.** `0.07%` is `0.0007` and `99.99%` is `0.9999`, as the
  fractions written out are, both as a percent literal in a formula and as a text in a `Double` cell or in a
  Spreadsheet cell without a type. Before, they were `0.0007000000000000001` and `0.9998999999999999`, so a result
  computed from such a value can change in its last digit. A `BigDecimal` cell already read the exact fraction, and a
  `Float` cell reads the same value as before. A percent literal also takes any number of decimals and an exponent:
  `0.5%`, `12.345%` and `1.5e2%` compile, while before it had no decimals or exactly two.

* **The `%` and `**` operators are short forms of `remainder` and `pow`.** They are no longer deprecated, and
  `x % y` and `x ** y` give the same results as `remainder(x, y)` and `pow(x, y)` for positive, negative, zero and
  empty operands. `%` keeps the sign of the dividend and the type of its operands, as before. A rule gets different
  results where the old operators differed from the functions:

  - `**` returns a `Double`: `2 ** 3` is `8.0` and `2 ** -1` is `0.5`, while before they were the whole numbers `8`
    and `0`. A `BigInteger` or a `BigDecimal` raised to an `Integer` power stays exact, as `pow(x, n)` now is for
    them, while `**` between two `BigInteger` or two `BigDecimal` values no longer compiles: write
    `x ** y.intValue()` there.
  - An empty operand gives an empty result. Before, `%` read an empty operand as `0`, so `x % 3` was `0` and
    `7 % x` stopped with `/ by zero`, and `x ** y` with an empty `y` was `1`.

* **The deprecated `->` operator is removed.** It was never backed by an implementation, so no working rule uses it.

* **The deprecated `++` and `--` operators show a warning.** They still work, and OpenL Studio marks every use, for
  example with `DEPRECATED '++' operator will be removed in the next version. Use 'x += 1' instead.` Rewrite the
  expression: `x++` and `++x` become `x += 1`, `x--` and `--x` become `x -= 1`, and a loop such as
  `for (int i = 0; i < n; i++)` becomes `for (int i = 0; i < n; i += 1)`. Where a rule uses the value of `x++`
  itself, as in `y = x++`, read it first: `y = x; x += 1;`.

* **The functions `dateToString`, `stringToDate`, `parseFormattedDouble`, `addIgnoreNull` and
  `addArrayElementIgnoreNull` are removed.** A rule that still calls one of them no longer compiles, for example with
  `Method 'dateToString(java.util.Date)' is not found.` The `openl:migrate` goal of the OpenL Maven plugin rewrites
  such calls in the workbooks of a project, choosing the replacement by the types of the arguments. Otherwise, rewrite
  the call:

  - **`dateToString(date)`** — `toString(date, "M/d/yy")`
  - **`dateToString(date, pattern)`** — `toString(date, pattern)`
  - **`stringToDate(text)`** — `toDate(text)`
  - **`parseFormattedDouble(text)`** — `(Double) toNumber(text)`
  - **`addIgnoreNull(array, element)`** — `element == null ? array : add(array, element)`
  - **`addIgnoreNull(array, index, element)`** — `element == null ? array : addElement(array, index, element)`

  `addArrayElementIgnoreNull` takes the same arguments as `addIgnoreNull` and is rewritten the same way. The rewrite
  writes the array and the element twice, so `openl:migrate` leaves a call where one of them is computed by another
  call, such as `addIgnoreNull(names, lookup(id))`: that call would run twice. The `DEFAULT_DOUBLE_FORMAT` constant is
  removed with them, so write its pattern `"#,##0.00"` instead. Check the rewritten rules for these differences:

  - The removed functions wrote and read numbers and dates in the language of the server. The replacements always use
    US English: a comma groups the digits, a point starts the fraction and month names are English. On a German server,
    `dateToString(date, "MMMM")` gave `Mai`, while `toString(date, "MMMM")` gives `May`.
  - `toString(date)` without a pattern gives `04/30/2015`, where `dateToString(date)` gave `4/30/15`. The pattern
    `M/d/yy` keeps the short form.
  - A replacement returns an empty value where the removed function stopped the call with an error. This is the case
    for `toDate` and `toNumber` with a text they cannot read. `toDate` and `toNumber` also need the whole text to be a date or a number: `toNumber("12abc")` is empty,
    while `parseFormattedDouble("12abc")` read `12`.
  - `toNumber` returns a `Number`, which a rule cannot return as a `Double` or use in arithmetic. The `(Double)` cast
    turns it into a `Double`, as `parseFormattedDouble` returned, and keeps an empty value empty.
  - `parseFormattedDouble(text, pattern)` has no direct replacement, so `openl:migrate` leaves it. `toNumber` reads
    the comma that groups the digits. Remove other symbols of the pattern first, such as a currency sign:
    `(Double) toNumber(replace(text, "$", ""))`.

* **`format(value)` and `format(value, pattern)` write a number or a date as `toString` does.**
  `format(value, pattern)` gives the text of `toString(value, pattern)`, `format(amount)` the text of
  `toString(amount, "#,##0.00")`, such as `1,234.50`, and `format(date)` the text of `toString(date)`. A rule that
  calls `format` compiles as before and `openl:migrate` leaves it, but its result changes:

  - `format(date)` gives `04/30/2015` instead of `4/30/15`. Write `format(date, "M/d/yy")` to keep the old text.
  - `format` uses US English instead of the language of the server, as `toString` does. On a German server,
    `format(1234.5)` gave `1.234,50` and now gives `1,234.50`.
  - `format(amount)` of an empty value returns an empty value, where the old `format` wrote `0.00`. Write
    `format(amount == null ? 0 : amount)` to keep the old text.
  - `format(amount, pattern)` with an invalid pattern returns an empty value instead of stopping the call with an
    error.

  `format(pattern, values)`, which fills a text such as `format("{0} days", n)`, works as before.

* **A number has no `$` sign.** The `$` sign is removed from formulas and ranges, because it reads differently across
  countries and also starts a reference to a spreadsheet step. A formula or a range that still uses it usually no
  longer compiles. Do not rely on that: `$600` also names a step called `600` or a variable called `$600`, and a
  formula next to such a step compiles with its value. Search the formulas for a `$` before a digit and remove the
  sign:

  | Before          | After         |
  |-----------------|---------------|
  | `= $600 * 2`    | `= 600 * 2`   |
  | `= $2,500 + x`  | `= 2,500 + x` |
  | `>= $2,500`     | `>= 2500`     |
  | `[$1K .. $10K)` | `[1K .. 10K)` |

  The `K`, `M` and `B` multipliers stay, and their value is exact now. `1.5M` is the `Integer` `1500000` as before,
  while a value with a fraction, such as `1.0005K`, is the `Double` `1000.5` instead of being rounded to an `Integer`.
  A value beyond the `Integer` limits, such as `3B`, is a `Long` instead of a compilation error.

  A comma groups the thousands only between groups of exactly three digits, as in `1,000.00` or `12,345,678`. In a
  formula, it does so in a number that starts with `1` to `9`, as before, and any other comma separates values:
  `max(0,500)` and `max(12,34)` compare two numbers.

  > [!Note]
  > A comma between digits stays ambiguous in a formula that lists values, such as an array or the arguments of a
  > function: `{10,500,600}` is an array of one number, `10500600`, and `{1,000}` an array of `1000`. Write a space
  > after a comma that separates values: `{10, 500, 600}` holds three numbers, and `{1, 000}` holds `1` and `0`.

  A range used to drop every comma: in a condition column of ranges of a smart rule or a simple rule, `77,99` was the
  number `7799`. Now it is the list of `77` and `99`, which makes the column a column of lists, where `1,500` is the
  list of `1` and `500` too. Write such a number without the comma.

  A text cell of a number type in a Data, Test or Spreadsheet table reads the commas that group thousands too, such as
  `1,000.50`, while before it did not compile. An array cell keeps separating its values by every comma.

* **A whole number in a formula is decimal, even when it starts with zeros.** Octal numbers are removed, so `010` is
  `10` instead of `8`, and `0777` is `777` instead of `511`. `08`, `09`, `008` and `009` compile, and the numbers from
  `00` to `07` keep their value. A hexadecimal number such as `0xFF` and an octal escape in a character or a string,
  such as `'\101'`, are not affected.

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

* **`round(x, mode)` stops at the limit of the `Integer` range, as `round(x)` does.** Both return an `Integer`:
  `round(5000000000.4)` and `round(5000000000.4, DOWN)` are `2147483647`, and `round(-5000000000.4, DOWN)` is
  `-2147483648`. Before, `round(x, mode)` wrapped around and gave `705032704` and `-705032704`. To round a number beyond
  the `Integer` range, keep the type of the number with `round(x, 0)` or `round(x, 0, mode)`:
  `round(5000000000.6, 0, DOWN)` is `5.0E9`. Check also these differences:

  - A whole number, such as an `Integer` or a `Long`, is rounded exactly. `round(16777217)` is `16777217`, and
    `round(16777217, 0)` is `1.6777217E7`, a `Double` as for a fraction. Before, a whole number was rounded as a
    `Float`, so they were `16777216` and the `Float` `1.6777216E7`.
  - `round(-5.0E9f)` is `-2147483648` instead of `-2147483647`, as the limit of the `Integer` range.
  - `round(x, n)` rounds a large whole `Double` exactly. Before, it could change in its last digit:
    `round(1.0E15, 1)` was `1.0000000000000001E15`.

* **`transform unique to` skips an empty element of an array, as the other array index operators except
  `transform to` do.** It does not evaluate the expression for an element that is `null`. Before, it evaluated the
  expression for the empty element: `(x) transform unique to isEmpty(x) ? -1 : x` gave `-1` for it, and
  `transform unique to length()`, which calls a method of the element without naming it, stopped with a
  `NullPointerException`. `transform to` keeps evaluating the expression for every element, an empty one too, so its
  result has as many values as the array; a method of an empty element called without naming it, as in
  `transform to length()`, gives an empty value, or `0` for a primitive type, instead of the
  `NullPointerException`.

* **`transform unique to` returns an empty value for a missing array, as the other array index operators do.**
  Applied to an array or a collection that is `null`, such as the values of a missing `Map`, it gives `null`. Before,
  it gave an empty array for a missing array and stopped with a `NullPointerException` for a missing collection.

* **`ORDER BY` returns the original elements of an array even when they are converted for comparison.** For an
  `Object[]` or `Number[]` holding `Integer` values, `[(Long x) order by x]` compares `Long` values but returns the
  original `Integer` values in order. Before, it returned converted `Long` values. `SELECT ALL` and `SPLIT BY`
  already returned original elements. Use `transform to` when the result must contain converted values.

* **`remove` gives an empty value for a missing array.** `remove((Integer[]) null, 0)` is `null`, as `removeElement`,
  `slice` and `removeNulls` already give for a missing array. Before, it stopped the call with `Index: 0, Length: 0`.
  A position outside an existing array still stops the call.

* **An array function keeps the type of the array.** `removeElement`, `slice`, `removeNulls` and `sort` applied to
  a primitive array, such as `int[]`, return an `int[]` instead of an `Integer[]`, as `remove` already did, and
  `sort(3, 1, 2)` sorts an `int[]`. `addElement` keeps the type when the added element fits it:
  `addElement(new int[] {1, 2}, 1, 5)` is an `int[]`, while an element of another type gives the closest common type,
  so `2.5` in an `int[]` gives a `double[]` instead of a `Double[]`. A rule, a field or a parameter declared as
  `Integer[]` still gets an `Integer[]`, while a Spreadsheet step without a type takes the type of the result.

* **`add` and `addAll` take the added elements into the type of the result.** An element that does not fit the
  array widens it, as for `addElement`: `add(new int[] {1}, 2.5)` is the `double[]` `[1.0, 2.5]`, and
  `add(new int[] {1, 2}, (Integer) null)` is an `Integer[]` that ends with an empty element. Before, the result kept
  the array type, so `2.5` was cut to `2` and the empty element became `0`. A boxed array stays boxed:
  `add(new Integer[] {1, null}, 2.5)` is a `Double[]` that keeps its empty element.

* **`flatten` gives an empty value when every array it gets is missing.** `flatten((Integer[]) null)` is `null`
  instead of an empty array, as `slice`, `sort` and the other array functions give for a missing array. A missing
  array next to another one still adds no elements, so `flatten((Integer[]) null, new Integer[] {1})` is `[1]`.

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
* **The user profile carries no settings.** `GET /rest/users/profile` no longer returns `showHeader`,
  `showFormulas`, `testsPerPage`, `testsFailuresOnly`, `testsFailuresPerTest`, `showComplexResult` and
  `showRealNumbers`, and `PUT /rest/users/profile` refuses a request that sends one of them with `400` and
  `Unknown field '<name>'`. OpenL Studio keeps these settings in the browser. Remove them from the requests of
  your clients.
* **A local change is answered with its moment in ISO 8601.** `modifiedOn` of
  `GET /rest/projects/{id}/local-history` used to be text formatted with the **Date Format** and **Time Format** of
  the system settings, in the time zone of the server, and the revision a module was opened at carried its id
  there. It is now a moment in UTC, such as `2026-10-29T18:20:19.123Z`, and that revision carries no `modifiedOn`.
  A client formats the moment itself.
* **A version of a table is answered by the values of its dimensions, not by a name.** `GET
  /rest/projects/{id}/tables` no longer answers `displayName`, such as `carPrice [effectiveDate=07/01/2020 10:30:00
  AM]`, and the nodes of `GET /rest/projects/{id}/tables/graph` no longer answer `dimensionProperties`, keyed by the
  names a business user reads. Both answer `dimensions` instead: the value of every dimension property of the version
  by its technical name, a date in ISO 8601, an enum by its name, several values as an array, such as
  `{"effectiveDate": "2020-07-01T10:30:00.000", "state": ["AR"]}`. A version node of the graph is named as its
  table is. Build the label of a version from `name` and `dimensions`.
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
* **A single file posted to the files API follows `conflictPolicy`.** `POST /rest/projects/{id}/files/{path}` and
  `POST /rest/repos/{repo}/files/{path}` used to ignore it for a file and answer `409` with
  `openl.error.409.file.create.failed.message` whenever a file was already there. `OVERWRITE` and `REPLACE` now
  replace the file and `SKIP` leaves it as it is, both answering `204` instead of `201`. `FAIL`, the default, answers
  `409` with `openl.error.409.file.archive.entry.exists.message`, naming the file. The raw `application/octet-stream`
  form takes `conflictPolicy` too.
* **`POST /rest/projects/{id}/run` refuses a table that does not compile.** It used to start the run, and
  `GET /rest/projects/{id}/run/result` answered `200` with no `errors` and a result worked out as if the cells that
  could not be read were empty, or with no result at all. It now answers `409` with the code
  `openl.error.409.run.table.compile.errors.message` and the first compilation error of the table. A table that
  compiles within its own module but not with the rest of the project runs with `fromModule`, as the Run button of
  the editor does.
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
* **A row appended to a raw table needs a cell for every column.** `POST /rest/projects/{id}/tables/{tableId}/lines`
  with the `RawSource` table type used to fill the missing trailing cells of a short row with blanks. It now answers
  `400` with `openl.error.400.table.action.row.width.message` and appends nothing, as the raw table actions do. A row
  wider than the table answers this code too, instead of `openl.error.400.table.append.column.count.message`. Send
  `{"value": null}` for a blank cell.
* **A request parameter that breaks a constraint is coded by the constraint**, as a request body field is. Its `code`
  used to hold the message key in braces, such as `openl.error.{jakarta.validation.constraints.Min.message}`. It is
  now `openl.error.Min`, `openl.error.Size` for a comment that is too long, and `openl.error.SidExistsConstraint`
  for an ACL request naming a user or a group that does not exist. The `message` is unchanged.
* **`treeView` and `profiles` are gone from `/rest/users/profile`.** `GET` no longer returns them, and a `PUT` that
  still sends `treeView` answers `400` with `Unknown field 'treeView'`. Drop the field from the body.
* **A `PUT /rest/users/profile` that leaves a field out keeps the stored value**, for a name, the e-mail and the display
  name. It used to clear a missing name, refuse a missing e-mail or display name, and answer `500` on a missing
  `treeView` after the names and the e-mail were already saved. An e-mail or a display name that is sent still cannot
  be empty, and a request that fails now changes nothing.
* **`PUT /rest/users/{username}` is for administrators only.** A non-administrator now receives `403` even when
  `{username}` is their own. Use `PUT /rest/users/info` to edit your own information or `PUT /rest/users/profile` to
  change your own password with the current password.
* **A project id is an opaque 13-character value.** Every project id of the REST API and of the WebSocket
  destinations used to be the Base64 of `repository:name`, and a project name was accepted in its place: the
  `{projectId}` of every `/rest/projects/{projectId}/...` path, `{project-id}` of `/rest/acls/projects/{project-id}`,
  `projectId` of `POST /rest/deployments` and `POST /rest/deployments/{id}`, `sourceProject` of a project copy and
  `dependsOn` of `GET /rest/projects`. An old id and a project name answer `404` now, and `dependsOn` answers `400`
  for a text that is not an id. Read the id from `id` of `GET /rest/projects` — filter it with `repository` and
  `name` — or from the answer of the request that created or copied the project, and do not build or decode it. The
  id is computed from the repository and the folder of the project, so it survives a rename in `rules.xml`, a branch
  switch, an open, a close and a restart. While the server runs, the id of a project whose folder moved outside OpenL
  Studio still serves the project, naming the new id in the body and in `Content-Location`, and the id of a deleted
  project answers `410`. A deployment keeps its id.
* **A name shared by several projects no longer answers `409` with candidates.** The answer, with the code
  `openl.error.409.project.identifier.ambiguous.message` and a `candidates` list, is gone: an id names one project.
* **`sourceRepositoryId` of a project copy is gone.** `POST /rest/repos/{repo}/projects/{name}/from-project` takes the
  source by its id, which names its repository too. A request that still sends the field answers `400`.
* **Deprecated REST endpoints are removed.** Call their replacements:
  - **`GET /rest/users/options`** — `GET /rest/settings`, where `emailVerification` is
    `supportedFeatures.emailVerification`. `userMode` there is `INTERNAL` for `multi`, `EXTERNAL` for `ad`, `oauth2`
    and `saml`, and empty for `single`; `canCreateInternalUsers` was `true` for `INTERNAL` only.
  - **`GET /rest/mail/settings`, `PUT /rest/mail/settings`** — `GET /rest/admin/settings/mail` and
    `POST /rest/admin/settings/mail`. `POST` takes the same `url`, `username` and `password` fields. `GET` does not
    return a saved password: it answers an object with `"secret": true` in its place, and a field fixed by the
    application properties as an object with its `value` and `"readOnly": true`.
  - **`GET /rest/projects/{id}/status`** — `GET /rest/projects/{id}?include=status`, which answers the same status
    as `compileStatus`. Its `branch` parameter only checked the branch the project was on; the project response
    names that branch in `branch`.
  - **`GET /rest/repos/{repo}/projects/{name}/history` and its `/branches/{branch}/` form** —
    `GET /rest/projects/{id}/history`, with `?branch={branch}` for the second. The project is named by its id, which
    `GET /rest/projects` returns, so a rename in `rules.xml` that is not saved yet does not hide its history.
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
* **`org.openl.rules.util.Round` has no public methods with primitive arguments.** The overloads that took a `double`
  or a `float`, such as `round(double)`, are private, so a Java call such as `Round.round(2.5)` compiles against the
  boxed overload and returns an `Integer` instead of an `int`. Code compiled against an earlier version fails with
  `NoSuchMethodError` until it is compiled again. The rules functions of `org.openl.rules.util` are not meant for Java
  code.
* **`org.openl.source.impl.ModuleFileSourceCodeModule` and `org.openl.syntax.exception.Runnable` are removed.** The
  source module was deprecated since 5.23.10; build a `URLSourceCodeModule` from the module URL instead. Nothing in
  OpenL Tablets used the `Runnable` interface; use `java.util.concurrent.Callable` or an interface of your own.
* **`RulesUtils.remove` has one generic form.** Its overloads for `int[]`, `long[]`, `double[]`, `float[]`, `short[]`,
  `byte[]`, `char[]` and `boolean[]` and for the arrays of their boxed types are removed; rules still get an array of
  the type they pass. The generic `remove(T[] array, int index)` returns `null` for a `null` array instead of throwing
  an `IndexOutOfBoundsException`. Java code that passes an array of a boxed type compiles unchanged, but code compiled
  against an earlier version fails with `NoSuchMethodError` until it is compiled again. Code that removes an element
  of a primitive array calls `ArrayUtils.remove` of Apache Commons Lang, which the removed overloads called.
* **`RulesUtils.flatten` returns `Object`.** It returns a primitive array, such as `int[]`, when the elements are
  primitive, so its Java return type is `Object` instead of `Object[]`. A Java caller casts the result to the array
  type it expects.
* **The math functions moved from `RulesUtils` to `org.openl.rules.util.Maths`.** `abs`, `sqrt`, `pow`, the other math
  functions and the `PI` and `E` constants are gone from `org.openl.rules.helpers.RulesUtils`. The rules functions of
  `org.openl.rules.util` are not meant for Java code, so Java and Groovy code calls `java.lang.Math`, which the
  removed functions called: `RulesUtils.sqrt(x)` becomes `Math.sqrt(x)`. Two names differ: `getExponent(x, y)` with
  two arguments is `Math.hypot(x, y)`, and `nextAfter(x)` with one argument is `Math.nextUp(x)`.
* **`org.openl.rules.serialization.JsonUtils` is removed.** Create an `ObjectMapper` with
  `JacksonObjectMapperFactoryBean` once and keep it: a configured mapper is thread-safe, so it also replaces
  `getCachedObjectMapper`. The mapper of `toJSON(value)` and `fromJSON(json, type)` wrote dates in this format:

  ```java
  var factory = new JacksonObjectMapperFactoryBean();
  factory.setDefaultDateFormat(new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
  ObjectMapper mapper = factory.createJacksonObjectMapper();
  ```

  - **`toJSON(value)`, `fromJSON(json, type)`** — `mapper.writeValueAsString(value)`, `mapper.readValue(json, type)`
  - **`createJacksonObjectMapper(types, mode)` and the forms that take `types`** — a factory with
    `setOverrideClasses(Set.of(types))` and `setDefaultTypingMode(mode)`. `toJSON` used `JAVA_LANG_OBJECT`, and
    `fromJSON` used `DISABLED`
  - **`splitJSON(json)`** — `mapper.readTree(json).properties()`, writing each value with `mapper.writeValueAsString`

  A rule that imports `JsonUtils` in an Environment table fails to compile with `Identifier 'JsonUtils' is not
  found.` Import `org.openl.rules.serialization.JacksonObjectMapperFactoryBean` instead and call the mapper, as in
  `new JacksonObjectMapperFactoryBean().createJacksonObjectMapper().writeValueAsString(value)`.
* **More deprecated Java API is removed.** Rewrite the calls:
  - **`new RulesDeployerService(Properties)`** — `new RulesDeployerService(properties::getProperty)`
  - **`RulesDeployerService.deploy(File, boolean)`** — `deploy(file.toPath(), ignoreIfExists)`
  - **`DeployStrategy.fromString(text)`** — `DeployStrategy.valueOf(text)`, which reads no `true`, `false` or blank
    text
  - **`DefaultTypingMode.EVERYTHING`** — `NON_FINAL_AND_ENUMS`, as described for administrators below
  - **`JacksonObjectMapperFactoryBean.setSimpleClassNameAsTypingPropertyValue(true)`** —
    `setJsonTypeInfoId(JsonTypeInfo.Id.NAME)`
  - **`new OpenLServiceFactoryBean()` with `setProxyInterface` and `setServiceName`** —
    `new OpenLServiceFactoryBean(proxyInterface, serviceName)`; a Spring XML file passes both as `<constructor-arg>`
  - **`PublisherType.WEBSERVICE` in `@QualifyPublisherType`** — remove it from the list. No publisher of that type
    exists, so a field, a method or an entity qualified for it alone was never filled or stored
  - **`BranchRepository.createBranch(projectPath, branch)`** — `createRepositoryBranch(branch, getBranch())`
  - **`BranchRepository.createBranch(projectPath, branch, startPoint)`** — `createRepositoryBranch(branch, startPoint)`
  - **`BranchRepository.deleteBranch(null, branch)`** — `deleteRepositoryBranch(branch)`; any other project path
    was rejected already
  - **`BranchRepository.getBranches(projectPath)`** — `listBranches()`, which it returned already

  A repository plugin that overrides one of the removed `BranchRepository` methods with `@Override` no longer
  compiles; delete the override.
* **A Maven build can no longer extend the WAR of OpenL Rule Services.** Maven Central has no `war` of
  `org.openl.rules.ruleservice.ws` and no `org.openl.rules.ruleservice.ws.all` or `org.openl.rules.webstudio`
  at all, so a `war` dependency on them no longer resolves. Build the extension as a JAR file with the
  `org.openl.rules.ruleservice.ws` dependency in the `provided` scope, and put it into `WEB-INF/lib` of the web
  application, as the
  [Advanced Configuration](https://openl-tablets.github.io/openl-tablets/user-guides/rule-services/advanced-configuration)
  guide describes. The jar of `org.openl.rules.ruleservice.ws` and the other libraries stay in Maven Central. A
  build of OpenL Tablets from source installs the OpenL Studio war as `org.openl.rules.studio:studio-backend`.
* **The OpenL Maven Plugin names the deployment archive after `finalName`.** The `deploymentName` parameter is
  deprecated for removal, has no effect and logs a warning. The archive of a project that depends on other OpenL
  projects is always `<finalName>-deployment.zip`, and OpenL Rule Services deploy it under the name `<finalName>`,
  which `openl:deploy` looks for as well. Remove `<deploymentName>` from the plugin configuration — the
  `pom.unused-parameters` step of `openl:migrate` removes it, together with `<deploymentPackage>` and the parameters
  the plugin no longer declares — and update `ruleservice.datasource.deployments` filters, clients and links that
  used the old name.

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

* **The restore comment template names the pattern of its date: `{datetime:MM/dd/yyyy hh:mm:ss a}`.** A bare
  `{datetime}` is now written in ISO 8601, such as `2026-10-29T18:20:19Z`, and a pattern of `DateTimeFormatter`
  after a colon writes it another way. Either is written in UTC, whereas the comments were written in the time zone
  of the server before: on a server outside UTC, a new comment names the same moment at another hour. The default
  template keeps the pattern the comments were written in before. On the first start,
  OpenL Studio moves the pattern of the removed date and time settings into the restore comment templates it keeps:
  a saved template with a bare `{datetime}` takes that pattern, or `MM/dd/yyyy hh:mm:ss a` where none was set, and
  where the installation set a pattern of its own, the design repositories that kept the default template take it
  too. A pattern with letters a comment does not write — a time zone, or `u`, which `SimpleDateFormat` read as the day
  of the week — is not carried over: the log names it, and the template takes `MM/dd/yyyy hh:mm:ss a`. A template
  set in the environment or in `application.properties` is not changed; give it a pattern yourself.

* **`data.format.date`, `data.format.time` and `data.format.datetime` are removed**, and with them **Date Format** and
  **Time Format** of the **System** section. A value left in the application properties has no effect; remove it.
  `GET` and `PUT /rest/admin/settings/system` no longer carry `datePattern` and `timeFormat`. Every screen shows a
  date as the regional settings of the web browser of the user do, and each user picks a format of their own in
  **My Settings**, under **Date & Time**.

* **`ruleservice.datasource.deploy.classpath.jars` takes `NEVER`, `IF_ABSENT` or `ALWAYS` only.** Replace `true` with
  `IF_ABSENT` and `false` with `NEVER`, the values they stood for. With `true` or `false`, OpenL Rule Services
  publishes no service, and its log reports `Failed to convert value of type 'java.lang.String' to required type
  'org.openl.rules.ruleservice.loader.DeployStrategy'`.

* **The `EVERYTHING` default typing mode is removed, as Jackson deprecated it.** A service fails to deploy when
  `ruleservice.jackson.defaultTypingMode` or `jackson.defaultTypingMode` in its `rules-deploy.xml` is `EVERYTHING`.
  `NON_FINAL_AND_ENUMS` is the closest mode: it writes the type of every value except a value of a final class, such
  as `Long` or `LocalDate`. The `config.deploy.jackson-typing` step of `openl:migrate` and the **Migrate** button of
  the **Deploy Configuration** tab in OpenL Studio put it in place of `EVERYTHING` in `rules-deploy.xml`.

* **`ruleservice.jackson.simpleClassNameAsTypingPropertyValue` is no longer read**, nor
  `jackson.simpleClassNameAsTypingPropertyValue` in `rules-deploy.xml`. It turned the `CLASS` type id into `NAME`,
  which is the default of `ruleservice.jackson.jsonTypeInfoId`. A service that sets `jackson.jsonTypeInfoId` to
  `CLASS` together with it now writes and expects full class names; set `jackson.jsonTypeInfoId` to `NAME` instead.
  The `config.deploy.jackson-typing` step of `openl:migrate` and the **Migrate** button of the **Deploy
  Configuration** tab remove the setting from `rules-deploy.xml` and, where it was `true`, set `jackson.jsonTypeInfoId`
  to `NAME` unless it names a type id other than `CLASS`. Where `rules-deploy.xml` sets no type id, they set `NAME`
  too, so a service whose `ruleservice.jackson.jsonTypeInfoId` is another id, such as `MINIMAL_CLASS`, needs that id in
  `jackson.jsonTypeInfoId` instead.

* **Repoint anything that routes or allows `/web`.** Check reverse-proxy location blocks, ingress rules, API
  gateway routes, WAF path rules and the `cors.allowed.origins` consumers for `/web`, and change them to
  `/rest`. A proxy that forwards `/web/ws` or `/rest/ws` for the WebSocket must forward `/ws` instead, with the
  upgrade headers and an idle timeout long enough for a connection that stays open. It no longer shares a location
  with `/rest`.

* **The `sse-algorithm` of an AWS S3 repository now encrypts the objects, and the new `sse-kms-key-id` chooses the
  KMS key.** Before, OpenL only saved the value in the metadata of each object, so the default encryption of the
  bucket applied whatever the value was. Now every object that OpenL writes is sent with the algorithm. Objects
  written before the upgrade keep their encryption.

  - With `aws:kms` or `aws:kms:dsse`, grant the identity that OpenL uses the `kms:GenerateDataKey` and `kms:Decrypt`
    permissions on the key.
  - Clear the setting for a storage that is compatible with S3 but does not support the algorithm, because it refuses
    the writes.
  - A value that the S3 API does not define, such as `AWS_KMS`, or a key set for another algorithm is refused. OpenL
    Rule Services does not start, OpenL Studio does not save the repository settings, and a repository that is
    configured in the properties file is shown as failed, with the reason in the log.
  - OpenL Studio saved the text `null` when **None** was chosen. It is read as no algorithm, and the next save of the
    repository settings removes it.

* **An e-mail verification link deployed under a context path containing `web` is fixed.** With the default
  `/webstudio` context path the link previously lost that path and did not resolve. No action is required
  beyond upgrading; a link sent by an earlier version stays broken.

* **Download the WAR files from the GitHub release, under new names.** The release `7.0.0` on
  [GitHub](https://github.com/openl-tablets/openl-tablets/releases) holds `openl-studio.war`, `ruleservices.war`
  and `ruleservices-all.war`, the former `org.openl.rules.webstudio`, `org.openl.rules.ruleservice.ws` and
  `org.openl.rules.ruleservice.ws.all` WARs. Each is signed in the `.asc` file of the same name with the key that
  signs the Maven Central artifacts. Repoint every script and pipeline that downloads a WAR from Maven Central, such
  as `https://github.com/openl-tablets/openl-tablets/releases/download/7.0.0/openl-studio.war` in place of
  `org/openl/rules/org.openl.rules.webstudio/7.0.0/org.openl.rules.webstudio-7.0.0.war`. A servlet container takes
  the context path from the file name, so deploy `openl-studio.war` as `webstudio.war` to keep the `/webstudio`
  path. The Docker images and the DEMO package need nothing.

* **OpenL Studio neither shows nor changes the build files and the files of version control systems.** The new
  setting `repository.ignored-paths`, **Ignored Paths** in the **System** section of the administration, lists them:
  by default the files of Git, such as `.gitignore` and the `.github` folder, the Maven files `pom.xml`, `mvnw` and
  `.mvn`, `.dockerignore`, and the files of Subversion and CVS. The `.git` folder is always ignored.

  - The `pom.xml` of a project is no longer listed, exported in a ZIP archive or deployed, and a save of the project
    keeps it as the repository holds it. A tool that read it from a deployment or an export reads it from the
    repository.
  - A project uploaded as a ZIP archive leaves these files out, as it left out `.svn`, `CVS` and `.cvsignore`, now
    whatever their letter case.
  - In a Git repository, a revision that changes only these files, such as a version bump in `pom.xml` made by a CI
    pipeline, is a technical revision: the history of the project hides it unless technical revisions are shown.
  - Clear the setting to show these files again. To add the build output folder, such as `target`, keep the defaults
    by referring to them: `repository.ignored-paths = ${repository.default-ignored-paths}, **/target/**`.

* **OpenL Studio starts its MCP server on a free port of the loopback interface.** OpenL Studio serves the server at
  `/mcp` of its own address; the Docker image carries Node.js, and a WAR deployment starts the server when Node.js is
  on the `PATH`. Set `mcp.enabled=false` to keep the server off. A reverse proxy in front of OpenL Studio that forwards
  only the paths of the application needs `/mcp` too, so that AI agents reach the server.

## Testing Recommendations

After upgrading, verify in a non-production environment:

1. Every project that carries a `groovy/` folder compiles and deploys.
2. A service published from such a project answers as it did before.
3. Heap headroom, if the maximum heap is set close to the previous usage.
4. A client that calls the OpenL Studio API answers on `/rest`, and its WebSocket connects to `/ws`.
