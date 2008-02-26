package org.openl.rules.webstudio.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author Aliaksandr Antonik.
 */
@Aspect
public class SecurityAspect {
	private ProceedingAspectJSecurityInterceptor securityInterceptor;

	@Around("within(org.openl.rules..*) &&  execution(@org.acegisecurity.annotation.Secured public * *(..))")
	public Object domainObjectInstanceExecution(final ProceedingJoinPoint joinPoint) throws Throwable {
		if (securityInterceptor != null) {
			return securityInterceptor.invoke(joinPoint);
		} else {
			return joinPoint.proceed();
		}
	}

	public ProceedingAspectJSecurityInterceptor getSecurityInterceptor() {
		return securityInterceptor;
	}

	public void setSecurityInterceptor(ProceedingAspectJSecurityInterceptor securityInterceptorProceeding) {
		this.securityInterceptor = securityInterceptorProceeding;
	}
}

