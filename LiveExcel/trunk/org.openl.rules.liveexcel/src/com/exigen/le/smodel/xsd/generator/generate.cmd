@echo on
rem  Generate ServiceModel XSD file using JDK schemagen utility
rem  Before running - correct JDK_HOME 
SET JDK_HOME=C:\Program Files\Java\jdk1.6.0_18
SET CP=..\..\..\..\..\..\..\..\..\target\classes
SET OUT=.

"%JDK_HOME%\bin\schemagen.exe"  -cp %CP% -d %OUT% com.exigen.le.smodel.ServiceModel
rename schema1.xsd servicemodel.xsd
pause