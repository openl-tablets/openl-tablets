package org.openl.studio.projects.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Bean post-processor that applies {@link ProjectLockInterceptor} to methods
 * annotated with {@link LockForEditing}.
 *
 * <p>Proxied as the target class rather than by its interfaces: the service that takes projects up is injected
 * by class throughout the application, and an interface proxy is not one.
 */
@Component
@RequiredArgsConstructor
public class ProjectLockPostProcessor extends AbstractAdvisingBeanPostProcessor implements InitializingBean {

    private final transient ProjectLockInterceptor projectLockInterceptor;

    @Override
    public void afterPropertiesSet() {
        setProxyTargetClass(true);
        this.advisor = new DefaultPointcutAdvisor(
                AnnotationMatchingPointcut.forMethodAnnotation(LockForEditing.class),
                projectLockInterceptor
        );
    }
}
