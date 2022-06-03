# OpenL RuleService Profiler

__OpenL RuleService Profiler__ will help to analyze OpenL Rule Tables stacktrace calls and discover weak points of rule performance
using __YourKit__ or any alternative.

This tool decorates all OpenL Spreadsheet, Methods, Rules, SmartRules, and ect. tables with the following pattern:

```
org.openl.rules.profiler.<Table Name>$<Module Name>
```

As a result, OpenL Rule Tables stacktrace calls can be easily found and analyzed

## Configuration

1. Copy built `./target/org.openl.rules.profiler-<version>.jar` file to `lib` folder inside OpenL RuleServices application  
2. Add `javaagent` VM option to the command line of OpenL RuleService application:
```
-javaagent:<openl project directory>/Util/org.openl.rules.profiler/target/lib/aspectjweaver.jar
```
3. Set up YourKit or any alternative

## Usage

1. Run OpenL RuleService application
2. Run profiling tool
3. Execute for example REST request to the required endpoint of OpenL Project to be profiled
4. Investigate caught profiling report.