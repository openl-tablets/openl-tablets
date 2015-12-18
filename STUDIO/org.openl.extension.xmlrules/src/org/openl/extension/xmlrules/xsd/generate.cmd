@echo on
rem  Generate ServiceModel XSD file using JDK schemagen utility
rem  Before running - correct JDK_HOME
SET CP=..\..\..\..\..\..\target\classes
SET OUT=.

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.ExtensionModuleInfo
IF EXIST model.xsd del /F model.xsd
rename schema1.xsd model.xsd

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.TypeImpl
IF EXIST type.xsd del /F type.xsd
rename schema1.xsd type.xsd

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.DataInstanceImpl
IF EXIST data-instance.xsd del /F data-instance.xsd
rename schema1.xsd data-instance.xsd

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.TableImpl org.openl.extension.xmlrules.model.single.FunctionImpl
IF EXIST table-function.xsd del /F table-function.xsd
rename schema1.xsd table-function.xsd

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.Cells
IF EXIST cells.xsd del /F cells.xsd
rename schema1.xsd cells.xsd

schemagen.exe -cp %CP% -d %OUT% org.openl.extension.xmlrules.model.single.PredefinedType
IF EXIST predefined-type.xsd del /F predefined-type.xsd
rename schema1.xsd predefined-type.xsd
