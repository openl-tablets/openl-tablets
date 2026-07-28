package org.openl.studio.security;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Bean post-processor that applies {@link CommitInfoInterceptor} to methods
 * annotated with {@link CommitInfoRequired}.
 */
@Component
@RequiredArgsConstructor
public class CommitInfoPostProcessor extends AbstractAdvisingBeanPostProcessor implements InitializingBean {

    private final CommitInfoInterceptor commitInfoInterceptor;

    @Override
    public void afterPropertiesSet() {
        this.advisor = new DefaultPointcutAdvisor(
                AnnotationMatchingPointcut.forMethodAnnotation(CommitInfoRequired.class),
                commitInfoInterceptor
        );
    }
}
