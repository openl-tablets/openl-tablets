package org.openl.rules.webstudio.security;

import org.acegisecurity.intercept.AbstractSecurityInterceptor;
import org.acegisecurity.intercept.ObjectDefinitionSource;
import org.acegisecurity.intercept.InterceptorStatusToken;
import org.acegisecurity.intercept.method.MethodDefinitionSource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author Aliaksandr Antonik.
 */
public class ProceedingAspectJSecurityInterceptor extends AbstractSecurityInterceptor {
	private MethodDefinitionSource objectDefinitionSource;

	/**
	 * Indicates the type of secure objects the subclass will be presenting to
	 * the abstract parent for processing. This is used to ensure collaborators
	 * wired to the <code>AbstractSecurityInterceptor</code> all support the
	 * indicated secure object class.
	 *
	 * @return the type of secure object the subclass provides services for
	 */
	public Class getSecureObjectClass() {
		return JoinPoint.class;
	}

	public ObjectDefinitionSource obtainObjectDefinitionSource() {
		return objectDefinitionSource;
	}

	public void setObjectDefinitionSource(MethodDefinitionSource newSource) {
		objectDefinitionSource = newSource;
	}

	public MethodDefinitionSource getObjectDefinitionSource() {
		return objectDefinitionSource;
	}


	/**
     * This method should be used to enforce security on a <code>ProceedingJoinPoint</code>.
     *
     * @param jp The AspectJ joint point being invoked which requires a security decision
	  *
     * @return The returned value from the method invocation
     */
    public Object invoke(ProceedingJoinPoint jp) throws Throwable {
        Object result = null;
        InterceptorStatusToken token = super.beforeInvocation(jp);

        try {
            jp.proceed();
        } finally {
            result = super.afterInvocation(token, result);
        }

        return result;
    }

}
