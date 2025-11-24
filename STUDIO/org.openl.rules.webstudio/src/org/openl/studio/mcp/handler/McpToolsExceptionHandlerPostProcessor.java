package org.openl.studio.mcp.handler;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Bean post-processor that applies McpToolsExceptionHandler to methods annotated with @Tool.
 */
@Component
public class McpToolsExceptionHandlerPostProcessor extends AbstractAdvisingBeanPostProcessor implements InitializingBean {

    private final McpToolsExceptionHandler mcpToolsExceptionHandler;

    public McpToolsExceptionHandlerPostProcessor(McpToolsExceptionHandler mcpToolsExceptionHandler) {
        this.mcpToolsExceptionHandler = mcpToolsExceptionHandler;
    }

    @Override
    public void afterPropertiesSet() {
        this.advisor = new DefaultPointcutAdvisor(
                AnnotationMatchingPointcut.forMethodAnnotation(Tool.class),
                mcpToolsExceptionHandler
        );
    }
}
