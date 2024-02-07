package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Array;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.rules.calc.AnySpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.rules.ruleservice.core.interceptors.RulesDeployAware;
import org.openl.rules.ruleservice.core.interceptors.ServiceClassLoaderAware;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public abstract class AbstractSPRToPlainConverterAdvice<T> extends AbstractServiceMethodAfterReturningAdvice<T> implements IOpenClassAware, IOpenMemberAware, RulesDeployAware, ServiceClassLoaderAware {

    private XlsModuleOpenClass module;
    private IOpenMember openMember;
    private volatile Pair<Class<?>, IOpenClass> convertToType;

    private RulesDeploy rulesDeploy;
    private ClassLoader serviceClassLoader;
    private SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy;
    private volatile boolean initialized = false;

    protected XlsModuleOpenClass getModule() {
        return module;
    }

    protected IOpenMember getOpenMember() {
        return openMember;
    }

    @Override
    public void setRulesDeploy(RulesDeploy rulesDeploy) {
        this.rulesDeploy = rulesDeploy;
    }

    @Override
    public void setServiceClassLoader(ClassLoader serviceClassLoader) {
        this.serviceClassLoader = serviceClassLoader;
    }

    protected SpreadsheetResultBeanPropertyNamingStrategy getSpreadsheetResultBeanPropertyNamingStrategy() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    PropertyNamingStrategy propertyNamingStrategy = ProjectJacksonObjectMapperFactoryBean
                            .extractPropertyNamingStrategy(rulesDeploy, serviceClassLoader);
                    if (propertyNamingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy) {
                        spreadsheetResultBeanPropertyNamingStrategy = (SpreadsheetResultBeanPropertyNamingStrategy) propertyNamingStrategy;
                    }
                    initialized = true;
                }
            }
        }
        return spreadsheetResultBeanPropertyNamingStrategy;
    }

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.module = (XlsModuleOpenClass) openClass;
    }

    @Override
    public void setIOpenMember(IOpenMember openMember) {
        this.openMember = openMember;
        this.convertToType = getConvertToType();
    }

    protected Pair<Class<?>, IOpenClass> getConvertToType() {
        if (convertToType == null) {
            synchronized (this) {
                if (convertToType == null) {
                    Pair<Class<?>, IOpenClass> convertToType1 = Pair.of(null, null);
                    IOpenClass openClass = openMember.getType();
                    int dim = 0;
                    while (openClass.isArray()) {
                        openClass = openClass.getComponentClass();
                        dim++;
                    }
                    if (openClass instanceof SpreadsheetResultOpenClass || openClass instanceof AnySpreadsheetResultOpenClass || openClass instanceof CustomSpreadsheetResultOpenClass) {
                        Class<?> t = Map.class;
                        if (openClass instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) openClass)
                                .getModule() != null) {
                            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) openClass;
                            t = spreadsheetResultOpenClass.getModule()
                                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                    .toCustomSpreadsheetResultOpenClass()
                                    .getBeanClass();
                        } else if (openClass instanceof CustomSpreadsheetResultOpenClass && ((CustomSpreadsheetResultOpenClass) openClass)
                                .isGenerateBeanClass()) {
                            t = ((CustomSpreadsheetResultOpenClass) openClass).getBeanClass();
                        }
                        if (dim > 0) {
                            t = Array.newInstance(t, dim).getClass();
                            openClass = openClass.getArrayType(dim);
                        }
                        convertToType1 = Pair.of(t, openClass);
                    }
                    convertToType = convertToType1;
                }
            }
        }
        return convertToType;
    }
}
