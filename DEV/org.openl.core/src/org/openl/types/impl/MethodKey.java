package org.openl.types.impl;

import java.util.Arrays;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 *
 * Key for IOpenMethod.
 *
 */
public final class MethodKey {
    private final String name;
    private final IOpenClass[] internalParameters;

    public MethodKey(IOpenMethod om) {
        IOpenClass[] pars = om.getSignature().getParameterTypes();
        this.internalParameters = getNormalizedParams(pars);
        this.name = om.isConstructor() ? "<init>" : om.getName();
    }

    public MethodKey(String name, IOpenClass[] pars) {
        this.name = name;
        this.internalParameters = getNormalizedParams(pars);
    }

    public MethodKey(IOpenClass[] pars) {
        this.name = "<init>";
        this.internalParameters = getNormalizedParams(pars);
    }

    public MethodKey(String name, IOpenClass[] parTypes, boolean doNotNormalizeParams) {
        this.name = name;
        this.internalParameters = doNotNormalizeParams ? parTypes : getNormalizedParams(parTypes);
    }

    /**
     * Normalizes types of method parameters. OpenL engine uses alias data types as internal types and they are used
     * only in OpenL. For java users alias data types are represented as appropriate java type. While method key usage
     * we should use underlying type of alias data type parameter as real type of parameter.
     *
     * @param originalParams parameters of method
     * @return normalized parameters
     */
    private IOpenClass[] getNormalizedParams(IOpenClass[] originalParams) {

        if (originalParams == null) {
            return null;
        }

        int firstParamToConvert = -1;
        for (int i = 0; i < originalParams.length; i++) {
            if (originalParams[i] instanceof JavaOpenClass) {
                continue;
            }
            firstParamToConvert = i;
            break;
        }

        if (firstParamToConvert == -1) {
            return originalParams;
        }

        IOpenClass[] normalizedParams = new IOpenClass[originalParams.length];
        if (firstParamToConvert > 0) {
            System.arraycopy(originalParams, 0, normalizedParams, 0, firstParamToConvert);
        }

        for (int i = firstParamToConvert; i < originalParams.length; i++) {
            IOpenClass param = originalParams[i];
            IOpenClass normParam = param;

            if (param instanceof DomainOpenClass || param instanceof AOpenClass && param.getInstanceClass() != null) {
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

        return Objects.equals(name, mk.name) && Arrays.equals(internalParameters,
            mk.internalParameters);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 17 + Arrays.hashCode(internalParameters);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");

        boolean first = true;

        for (IOpenClass c : internalParameters) {
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
