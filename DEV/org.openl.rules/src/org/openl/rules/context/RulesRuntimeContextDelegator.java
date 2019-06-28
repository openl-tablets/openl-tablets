package org.openl.rules.context;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

/**
 * Runtime context delegator.
 *
 * @author PUdalau
 */
public class RulesRuntimeContextDelegator extends DefaultRulesRuntimeContext {

    private static final long serialVersionUID = -2172865302513540686L;
    
    private IRulesRuntimeContext delegate;

    public RulesRuntimeContextDelegator(IRulesRuntimeContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getValue(String name) {
        Object value = super.getValue(name);
        if (value == null) {
            value = delegate.getValue(name);
        }
        return value;
    }

    @Override
    public synchronized String toString() {
        return super.toString() + "Delegated context:" + delegate.toString();
    }

    @Override
    public IOpenMethod getMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher) {
        if (openMethodDispatcher instanceof IRulesRuntimeContextOptimizationForOpenMethodDispatcher) {
            return ((IRulesRuntimeContextOptimizationForOpenMethodDispatcher) delegate)
                .getMethodForOpenMethodDispatcher(openMethodDispatcher);
        }
        return null;
    }

    @Override
    public void putMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher, IOpenMethod method) {
        if (openMethodDispatcher instanceof IRulesRuntimeContextOptimizationForOpenMethodDispatcher) {
            ((IRulesRuntimeContextOptimizationForOpenMethodDispatcher) delegate)
                .putMethodForOpenMethodDispatcher(openMethodDispatcher, method);
        }
    }

    // <<< INSERT >>>
    @Override
    public java.util.Date getCurrentDate() {
        if (super.getCurrentDate() == null) {
            return delegate.getCurrentDate();
        }
        return super.getCurrentDate();
    }
    @Override
    public java.util.Date getRequestDate() {
        if (super.getRequestDate() == null) {
            return delegate.getRequestDate();
        }
        return super.getRequestDate();
    }
    @Override
    public java.lang.String getLob() {
        if (super.getLob() == null) {
            return delegate.getLob();
        }
        return super.getLob();
    }
    @Override
    public java.lang.String getNature() {
        if (super.getNature() == null) {
            return delegate.getNature();
        }
        return super.getNature();
    }
    @Override
    public org.openl.rules.enumeration.UsStatesEnum getUsState() {
        if (super.getUsState() == null) {
            return delegate.getUsState();
        }
        return super.getUsState();
    }
    @Override
    public org.openl.rules.enumeration.CountriesEnum getCountry() {
        if (super.getCountry() == null) {
            return delegate.getCountry();
        }
        return super.getCountry();
    }
    @Override
    public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
        if (super.getUsRegion() == null) {
            return delegate.getUsRegion();
        }
        return super.getUsRegion();
    }
    @Override
    public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
        if (super.getCurrency() == null) {
            return delegate.getCurrency();
        }
        return super.getCurrency();
    }
    @Override
    public org.openl.rules.enumeration.LanguagesEnum getLang() {
        if (super.getLang() == null) {
            return delegate.getLang();
        }
        return super.getLang();
    }
    @Override
    public org.openl.rules.enumeration.RegionsEnum getRegion() {
        if (super.getRegion() == null) {
            return delegate.getRegion();
        }
        return super.getRegion();
    }
    @Override
    public org.openl.rules.enumeration.CaProvincesEnum getCaProvince() {
        if (super.getCaProvince() == null) {
            return delegate.getCaProvince();
        }
        return super.getCaProvince();
    }
    @Override
    public org.openl.rules.enumeration.CaRegionsEnum getCaRegion() {
        if (super.getCaRegion() == null) {
            return delegate.getCaRegion();
        }
        return super.getCaRegion();
    }
// <<< END INSERT >>>

}
