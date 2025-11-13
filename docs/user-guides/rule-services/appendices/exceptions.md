## Appendix C: Types of Exceptions in OpenL Rule Services

The following table describes exception types in OpenL Rule Services:
<br/>**Cause:** error("Some message") in rules
<br/>**Status code:** 400
<br/>**REST:** 
<br/>{
<br/>  message : "Some message",
<br/>  type : "USER_ERROR"
<br/>}
<br/>
<br/>**Cause**: Runtime execution error in OpenL rules, such as NPE, CCE, and DivByZero.
<br/>**Status code:** 500
<br/>**REST:**
<br/>{
<br/>  message : "Cannot convert '1ab2' to Double",
<br/>  type : "RULES_RUNTIME"
<br/>}
<br/>
<br/>**Cause**: Compilation and parsing errors.
<br/>**Status code:** 500
<br/>**REST:**
<br/>{
<br/>  message : "Missed condition column in Rules table",
<br/>  type : "COMPILATION"
<br/>}
<br/>
<br/>**Cause**: Other exception outside the OpenL engine, such as NPE, CCE, and AccessException.
<br/>**Status code:** 500
<br/>**REST:**
<br/>{
<br/>  message : "Cannot be null",
<br/>  type : "SYSTEM"
<br/>}
<br/>
<br/>**Cause**: Validation errors in input parameters, such as a value outside of a valid domain or wrong value in the context.
<br/>**Status code:** 500
<br/>**REST:**
<br/>{ 
<br/>  message : "'Mister' is outside of valid domain ['Male', 'Female']", 
<br/>  type : "RULES_RUNTIME" 
<br/>}

