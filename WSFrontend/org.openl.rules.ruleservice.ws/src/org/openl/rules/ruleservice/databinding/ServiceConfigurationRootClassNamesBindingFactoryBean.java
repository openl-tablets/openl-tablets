package org.openl.rules.ruleservice.databinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.model.RulesDeployHelper;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBeanHelper;
import org.openl.types.IOpenClass;

public class ServiceConfigurationRootClassNamesBindingFactoryBean extends ServiceConfigurationFactoryBean<Set<String>> {
    private static final String ROOT_CLASS_NAMES_BINDING = "rootClassNamesBinding";

    private Set<String> defaultAdditionalRootClassNames;

    public void setDefaultAdditionalRootClassNames(Set<String> defaultAdditionalRootClassNames) {
        this.defaultAdditionalRootClassNames = Objects.requireNonNull(defaultAdditionalRootClassNames,
            "defaultAdditionalRootClassNames cannot be null");
    }

    public Set<String> getDefaultAdditionalRootClassNames() {
        return defaultAdditionalRootClassNames;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        Set<String> ret = new HashSet<>(getDefaultAdditionalRootClassNames());
        ret.addAll(fromServiceDescription());
        ret.addAll(fromOpenLService());
        return Collections.unmodifiableSet(ret);
    }

    private Set<Class<?>> extractDatatypesClasses(IOpenClass moduleOpenClass) {
        Set<Class<?>> datatypeClasses = new HashSet<>();
        for (IOpenClass openClass : moduleOpenClass.getTypes()) {
            if (openClass instanceof DatatypeOpenClass) {
                datatypeClasses.add(openClass.getInstanceClass());
            }
        }
        return datatypeClasses;
    }

    private Set<String> fromOpenLService() throws ServiceConfigurationException {
        OpenLService openLService = getOpenLService();
        try {
            if (openLService.getOpenClass() != null) {
                Set<Class<?>> classes = new HashSet<>();
                classes.addAll(JacksonObjectMapperFactoryBeanHelper.extractSpreadsheetResultBeanClasses(
                    (XlsModuleOpenClass) openLService.getOpenClass(),
                    openLService.getServiceClass()));
                classes.addAll(extractDatatypesClasses(openLService.getOpenClass()));
                return classes.stream().map(Class::getName).collect(Collectors.toSet());
            }
            return Collections.emptySet();
        } catch (RuleServiceInstantiationException e) {
            throw new ServiceConfigurationException(e);
        }
    }

    private Set<String> fromServiceDescription() throws Exception {
        Set<String> ret = new HashSet<>();
        Object value = getValue(ROOT_CLASS_NAMES_BINDING);
        if (value instanceof String) {
            return RulesDeployHelper.splitRootClassNamesBindingClasses((String) value);
        } else {
            if (value != null) {
                throw new ServiceConfigurationException(
                    String.format("Expected string value for '%s' in the configuration for service '%s'.",
                        ROOT_CLASS_NAMES_BINDING,
                        getServiceDescription().getName()));
            }
        }
        return ret;
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.defaultAdditionalRootClassNames == null) {
            this.defaultAdditionalRootClassNames = new HashSet<>();
        }
    }
}
