package org.openl.rules.rest.exception;

/**
 * Base of the test REST exceptions.
 *
 * <p>The catch-all handler of a test advice takes this type instead of {@link RuntimeException}. The OpenAPI
 * generator scans the Studio exception packages of the classpath. A handler of {@link RuntimeException} would also
 * collect the status codes of the real Studio exceptions found there and tie the expected document to them.
 *
 * @author Yury Molchan
 */
public class RestException extends RuntimeException {
}
