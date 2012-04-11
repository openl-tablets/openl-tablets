rem This setup is standard for Eclipse environment; 
rem you may want change if your configuration is different
set PLUGIN_HOME=%~dp0/../../plugins
set PLUGIN_VERSION=1.1.1



set JAVA_CONSOLE=java

set OPENL_CORE=%PLUGIN_HOME%/org.openl.core_%PLUGIN_VERSION%

rem set _LIB=
set _LIB=%_LIB%;%OPENL_CORE%/lib/org.openl.core.jar


set _LIB=%_LIB%;%OPENL_CORE%/lib/commons-logging-1.0.3/commons-logging.jar
set _LIB=%_LIB%;%OPENL_CORE%/lib/commons-logging-1.0.3/commons-logging-api.jar
set _LIB=%_LIB%;%OPENL_CORE%/lib/commons-lang-1.0.1/commons-lang-1.0.1.jar



set _LIB=%_LIB%;../lang.config/classes

set _USER_CP=classes;%_USER_CP%


set VM_PARAMS=%_USER_VM_PARAMS%

