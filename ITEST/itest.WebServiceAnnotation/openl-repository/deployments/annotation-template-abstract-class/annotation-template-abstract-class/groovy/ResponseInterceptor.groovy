import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice

import javax.ws.rs.core.Response
import java.lang.reflect.Method

class ResponseInterceptor implements ServiceMethodAfterAdvice<Response> {

    Response afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return Response.ok().entity(new ResponseDto(result)).build();
    }

    Response afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(new ResponseDto(t.getMessage())).build();
    }

    private static class ResponseDto {

        private final Object body;

        ResponseDto(Object body) {
            this.body = body;
        }

        Object getBody() {
            return body;
        }
    }

}