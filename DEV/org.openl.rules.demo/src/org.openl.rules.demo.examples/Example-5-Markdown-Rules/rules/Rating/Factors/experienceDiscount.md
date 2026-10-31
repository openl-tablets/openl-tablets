---
table: SmartRules Double experienceDiscount(Integer licenseYears)
---

Discount for the years the driver holds a license.

> The years a driver holds a license are matched by a range written in two columns, from and to, the upper
> bound not included. One title stands over both columns: its cell spans them with `colspan`, so the table is written
> in HTML, as a table with merged cells is.

<table>
<tr><td colspan="2">Years Licensed</td><td>Discount</td></tr>
<tr><td>0</td><td>3</td><td>0</td></tr>
<tr><td>3</td><td>10</td><td>0.05</td></tr>
<tr><td>10</td><td>100</td><td>0.1</td></tr>
</table>
