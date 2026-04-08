---
title: OpenL Tablets 5.26.13 Migration Notes
---

### 1. Decimal Number Formatting

OpenL Rule Services now outputs `Double` and `Float` type numbers in a more readable format. Instead of displaying numbers like `3.1415E-5`, it will show them as `0.0000031415`.

---

### 2. Date Parsing Enhancement

The date parsing system has been upgraded to provide improved accuracy. Previously, invalid date components were ignored; the updated implementation now generates errors when encountering such issues.

Legend: In the Text column, *italic* marks the notable input portion. In the Actual in UTC column, **bold** marks incorrect values from the old behavior. In the Expected in UTC+3 column, ***bold italic*** marks timezone-adjusted values.

| Text                            | Actual in UTC                        | Expected in UTC              | Expected in UTC+3                  |
|:--------------------------------|:-------------------------------------|:-----------------------------|:-----------------------------------|
| 10/20/20 **17**                 | 20 October **2020** 00:00:00.000     | Error                        | Error                              |
| 10/20/2017 **18:00**            | 20 October 2017 **00:00**:00.000     | 20 October 2017 18:00:00.000 | 20 October 2017 18:00:00.000       |
| 10/20/2017 09:00                | 20 October 2017 09:00:00.000         | the same                     | the same                           |
| 10/20/2017, **10/20/2018**      | the first date is parsed only        | an array of dates            | an array of dates                  |
| 2017-10-20T09:00:00             | 20 October 2017 09:00:00.000         | the same                     | the same                           |
| 2017-10-20T09:00:00.**9876**    | 20 October 2017 09:00:00.**000**     | 20 October 2017 09:00:00.987 | 20 October 2017 09:00:00.987       |
| 2017-10-20T09:00:00.**9876Z**   | 20 October 2017 **09:00**:00.**000** | 20 October 2017 09:00:00.987 | 20 October 2017 ***12:00***:00.987 |
| 2017-10-20T09:00:00**Z**        | 20 October 2017 **09:00**:00.000     | 20 October 2017 09:00:00.000 | 20 October 2017 ***12:00***:00.000 |
| 2017-10-20T09:00:00**GMT+3**    | 20 October 2017 **09:00**:00.000     | 20 October 2017 06:00:00.000 | 20 October 2017 ***09:00***:00.000 |
| 2017-10-20T09:00:00+**03:30**   | 20 October 2017 **06:00**:00.000     | 20 October 2017 05:30:00.000 | 20 October 2017 ***08:30***:00.000 |
| 2017-10-20T09:00:12.**1212+03** | 20 October 2017 **09**:00:12.**000** | 20 October 2017 06:00:12.121 | 20 October 2017 ***09:00***:12.121 |
| 2007-04-05T**24:00**            | **5** April 2007 00:00:00.000        | 6 April 2007 00:00:00.000    | 6 April 2007 00:00:00.000          |
