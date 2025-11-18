# Keywords

The following table represents BEX keyword equivalents to Java expressions:

| Java expression | BEX equivalents                      |
|-----------------|--------------------------------------|
| `==`            | `equals to` `is same as`                |
| `!=`            | `does not equal to` `is different from` |
| `a.b`           | `b of the a`                         |
| `<`             | `is less than`                       |
| `>`             | `is more than`                       |
| `<=`            | `is less or equal` `is in`           |
| `!>`            | `is no more than`                    |
| `>=`            | `is more or equal`                   |
| `!<`            | `is no less than`                    |

Because of these keywords, name clashes with business vocabulary can occur. The easiest way to avoid clashes is to use upper case notation when referring to model attributes because BEX grammar is case sensitive and all keywords are in lower case.

For example, assume there is an attribute called `isLessThanCoverageLimit`. If it is referred to as **is less than coverage limit**, a name clash with keywords **is less than** occurs. The workaround is to refer to the attribute as **Is Less Than Coverage Limit**.
