import javax.ws.rs.core.HttpHeaders
import java.lang.reflect.Method

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

class BeforeInterceptor implements ServiceMethodBeforeAdvice {

    void before(Method interfaceMethod, Object proxy, Object... args) throws Throwable {
        HttpHeaders httpHeaders = (HttpHeaders) args[1];
        args[0] = httpHeaders.getLanguage().toString();
    }

}