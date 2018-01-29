package org.openl.rules.ruleservice.publish.jaxrs;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.core.Response;

public class JAXRSInvocationHandler implements InvocationHandler {

	private Object target;
	private Map<Method, Method> methodMap;
	private Map<Method, PropertyDescriptor[]> methodMapToPropertyDescriptors;

	public JAXRSInvocationHandler(Object target, Map<Method, Method> methodMap,
			Map<Method, PropertyDescriptor[]> methodMapToPropertyDescriptors) {
		if (target == null) {
			throw new IllegalArgumentException("target argument must not be null!");
		}
		if (methodMap == null) {
			throw new IllegalArgumentException("methodMap argument must not be null!");
		}
		if (methodMapToPropertyDescriptors == null) {
			throw new IllegalArgumentException("methodMapToPropertyDescriptors argument must not be null!");
		}
		this.target = target;
		this.methodMap = methodMap;
		this.methodMapToPropertyDescriptors = methodMapToPropertyDescriptors;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Method m = methodMap.get(method);
		if (m == null) {
			throw new IllegalStateException("Method is not found in the map of methods.");
		}
		PropertyDescriptor[] propertyDescriptors = methodMapToPropertyDescriptors.get(method);
		if (args != null && args.length == 1 && propertyDescriptors != null) {
			// Wrapped argument process;
			Object[] arguments = new Object[propertyDescriptors.length];
			int i = 0;
			if (args[0] != null) {
				for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
					arguments[i] = propertyDescriptor.getReadMethod().invoke(args[0]);
					i++;
				}
			}
			args = arguments;
		}

		Object o = m.invoke(target, args);
		if (o instanceof Response) {
			return o;
		} else {
			return Response.status(Response.Status.OK).entity(o).build();
		}
	}
}