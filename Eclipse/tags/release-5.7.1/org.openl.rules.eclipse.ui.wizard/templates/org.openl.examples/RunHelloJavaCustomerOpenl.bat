@echo off

cd %~dp0

@%~dp0/../lang.config/runopenl -file openl/HelloJavaCustomer.openl main


rem @%~dp0/run org.openl.main.OpenlMain -file src/openl/HelloJavaCustomer.openl main

pause
