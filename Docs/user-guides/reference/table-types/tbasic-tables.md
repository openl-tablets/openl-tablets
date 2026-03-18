{
  "BankID": "commerz",
  "Limit": 5000
}
```

If the [description column](#spr_column_description) is marked with // it is dispalyed as field property in the JSON structure. However, the steps listed in this column are excluded from the schema tree representation.

![](../../../assets/images/reference/SpreadsheetWith::Value.png)

![](../../../assets/images/reference/StepsDescriptionInSchema.png)

*Step descriptions added via //\<ColumnName\> column*

**Note:** Rule descriptions included in API-exposed rules will appear in the OpenL Tablets Rule Services. If multiple versions of a rule are available, the selection is made randomly from those that contain non-empty descriptions.

![](../../../assets/images/reference/SRDescriptionInOpenLStudio.png)

![](../../../assets/images/reference/SpreadsheetDescriptionInRuleServices.png)

*Rule description displayed in OpenL Rule Services*

**Note:** If the Maven plugin is used for generating a spreadsheet result output model, system integration can be based on generated classes. A default Java package for generated Java beans for particular spreadsheet tables is set using the spreadsheetResultPackage table property. Nevertheless, it is recommended to avoid any integration based on generated classes.

##### Testing Spreadsheet Result

Cells of a spreadsheet result, which is returned by the rule table, can be tested as displayed in the following spreadsheet table.

![](../../../assets/images/reference/8a5821a4a51ef3e649ccc27e7f0564ce.png)

*A sample spreadsheet table*

Simplified syntax is used to pull results from a spreadsheet table if a spreadsheet table contains only one column besides the row name column:` _res_.$<row name>`.

![](../../../assets/images/reference/131a2435015f629720736f1ec7266eea.png)

*Test for the sample spreadsheet table*

Columns marked with the grey color determine income values, and columns marked with yellow determine the expected values for a specific number of cells. It is possible to test as many cells as needed.

The result of running this test in OpenL Studio is provided in the following output table.

![](../../../assets/images/reference/2ec745ab5fb408d59eb5094714b2d643.png)

*The sample spreadsheet test results*

It is possible to test cells of the resulting spreadsheet which contain values of complex types, such as:

