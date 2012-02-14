/**
 * 
 */
package org.openl.types.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * 
 * Key for IOpenMethod.
 * 
 */
public final class MethodKey {
	private String name;
	private IOpenClass[] pars;
	private IOpenClass[] internalParameters;

	public MethodKey(IOpenMethod om) {
		this.name = om.getName();
		this.pars = om.getSignature().getParameterTypes();
		this.internalParameters = getNormalizedParams(pars);
	}

	public MethodKey(String name, IOpenClass[] pars) {
		this.name = name;
		this.pars = pars;
		this.internalParameters = getNormalizedParams(pars);
	}

	/**
	 * Normalizes types of method parameters. OpenL engine uses alias data types
	 * as internal types and they are used only in OpenL. For java users alias
	 * data types are represented as appropriate java type. While method key
	 * usage we should use underlying type of alias data type parameter as real
	 * type of parameter.
	 * 
	 * @param originalParams
	 *            parameters of method
	 * @return normalized parameters
	 */
	private IOpenClass[] getNormalizedParams(IOpenClass[] originalParams) {

		if (originalParams == null) {
			return null;
		}

		IOpenClass[] normalizedParams = new IOpenClass[originalParams.length];

		for (int i = 0; i < originalParams.length; i++) {
			IOpenClass param = originalParams[i];
			IOpenClass normParam = param;

			if (param instanceof DomainOpenClass || (param instanceof AOpenClass && param.getInstanceClass() != null)) {
				normParam = JavaOpenClass.getOpenClass(param.getInstanceClass());
			}

			normalizedParams[i] = normParam;
		}

		return normalizedParams;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof MethodKey)) {
			return false;
		}
		
		MethodKey mk = (MethodKey) obj;

		return new EqualsBuilder()
			.append(name, mk.name)
			.append(internalParameters, mk.internalParameters)
			.isEquals();
	}

	@Override
	public int hashCode() {
		int hashCode = new HashCodeBuilder()
			.append(name)
			.append(internalParameters)
			.toHashCode();
		
		return hashCode;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
	
		boolean first = true;
		
		for (IOpenClass c : pars) {
			if (!first) {
				sb.append(",");
			}
			sb.append(c.getName());
			first = false;
		}
		sb.append(")");
	
		return sb.toString();
	}

}