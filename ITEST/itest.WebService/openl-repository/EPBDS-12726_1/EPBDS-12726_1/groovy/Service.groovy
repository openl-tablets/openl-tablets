import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.annotations.ExternalParam
import org.openl.rules.ruleservice.core.annotations.Name
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor

import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders

interface Service {
    @ServiceCallBeforeInterceptor([BeforeInterceptor.class])
    String m1(String language, @ExternalParam @Context HttpHeaders headers)


    @ServiceCallBeforeInterceptor([BeforeInterceptor.class])
    String m2(String language, @ExternalParam @Context HttpHeaders headers, String name)

    String m3(@ExternalParam @Name("v") String v, int n)

}
