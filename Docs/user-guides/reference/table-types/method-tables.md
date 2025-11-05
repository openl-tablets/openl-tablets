| Line of Business | `lob`                            | `String` | LOB (Line of Business)        | `lob`                              | Line of business the rule is applied for.                       |
| US State         | `usState`                        | `Enum`   | US States                     | `state`                            | US state where the rule is applied.                                   |
| Country          | `country`                        | `Enum`   | Countries                     | `country`                          | Country where the rule is applied.                                    |
| US Region        | `usRegion`                       | `Enum`   | US Region                     | `usregion`                         | US region where the rule is applied.                                  |
| Currency         | `currency`                       | `Enum`   | Currency                      | `currency`                         | Currency with which the rule is applied.                              |
| Language         | `lang`                           | `Enum`   | Language                      | `lang`                             | Language in which the rule is applied.                                |
| Region           | `region`                         | `Enum`   | Region                        | `region`                           | Economic region where the rule is applied.                            |
| Canada Province  | caProvince                       | `Enum`   | Canada Province               | caProvinces                        | Canada province of operation where the rule is applied.               |
| Canada Region    | caRegion                         | `Enum`   | Canada Region                 | caRegions                          | Canada region of operation where the rule is applied.                 |
| Nature           | nature                           | `String` | Nature                        | nature                             | User-defined business meaning value a rule is applied to.     |
| locale           | locale                           | `java.lang.Locale` | n/a                        | n/a                             | Property commonly used for internationalization and localization purposes.     |

For more information on how property values relate to runtime context values and what rule table is executed, see [Business Dimension Properties](#business-dimension-properties) and [Rules Runtime Context](#rules-runtime-context).

##### Creating a Test Table for a Spreadsheet or Decision Table with SpreadsheetResult as Input Parameter

To create a test table for a spreadsheet or decision table that has another SpreadsheetResult as an input parameter, define the test table input as follows:

