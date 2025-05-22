package org.openl.security.acl.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

/**
 * Configuration for ACL services that are disabled.
 */
@Configuration
@ConditionalOnProperty(name = "user.mode", havingValue = "single")
public class DisabledAclConfiguration {

    @Bean
    public RepositoryAclService designRepositoryAclService() {
        return createDisabledAclService(RepositoryAclService.class);
    }

    @Bean
    public SimpleRepositoryAclService productionRepositoryAclService() {
        return createDisabledAclService(SimpleRepositoryAclService.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends SimpleRepositoryAclService> T createDisabledAclService(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{serviceClass},
                new DisabledAclServiceHandler(serviceClass.getSimpleName()));
    }

    private static final class DisabledAclServiceHandler implements InvocationHandler {

        private final String typeName;

        private DisabledAclServiceHandler(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("hashCode".equals(method.getName()) && method.getParameterCount() == 0) {
                return System.identityHashCode(proxy);
            } else if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
                return proxy == args[0];
            } else if ("toString".equals(method.getName()) && method.getParameterCount() == 0) {
                return String.format("Disabled%s@%s", typeName,
                        Integer.toHexString(System.identityHashCode(proxy)));
            }

            Class<?> returnType = method.getReturnType();
            if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
                return true;
            } else if (Map.class.isAssignableFrom(returnType)) {
                return Map.of();
            } else if (List.class.isAssignableFrom(returnType)) {
                return List.of();
            }
            return null;
        }
    }

}
