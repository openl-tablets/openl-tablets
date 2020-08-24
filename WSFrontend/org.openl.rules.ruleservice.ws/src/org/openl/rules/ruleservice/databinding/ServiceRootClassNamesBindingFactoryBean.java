package org.openl.rules.ruleservice.databinding;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.model.RulesDeployHelper;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.serialization.ObjectMapperConfigurationParsingException;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceRootClassNamesBindingFactoryBean extends AbstractFactoryBean<Set<String>> {

    private OpenLService openLService;
    private ServiceDescription serviceDescription;
    private Set<String> rootClassNames;

    public void setOpenLService(OpenLService openLService) {
        this.openLService = openLService;
    }

    public Set<String> getRootClassNames() {
        return rootClassNames;
    }

    public void setRootClassNames(Set<String> rootClassNames) {
        this.rootClassNames = rootClassNames;
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    public final OpenLService getOpenLService() throws ServiceConfigurationException {
        if (openLService == null) {
            openLService = OpenLServiceHolder.getInstance().get();
            if (openLService == null) {
                throw new ServiceConfigurationException("Failed to locate a service.");
            }
        }
        return openLService;
    }

    public final ServiceDescription getServiceDescription() throws ServiceConfigurationException {
        if (serviceDescription == null) {
            serviceDescription = ServiceDescriptionHolder.getInstance().get();
            if (serviceDescription == null) {
                throw new ServiceConfigurationException("Failed to locate a service description.");
            }
        }
        return serviceDescription;
    }

    public void setOverrideTypesAsString(String overrideTypes) {
        this.rootClassNames = new HashSet<>(RulesDeployHelper.splitRootClassNamesBindingClasses(overrideTypes));
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        OpenLService openLService = getOpenLService();
        Set<String> ret = new HashSet<>(getRootClassNames());

        if (getServiceDescription().getConfiguration() != null) {
            Map<String, Object> configuration = getServiceDescription().getConfiguration();
            Object rootClassNamesBinding = configuration
                .get(ProjectJacksonObjectMapperFactoryBean.ROOT_CLASS_NAMES_BINDING);
            if (rootClassNamesBinding instanceof String) {
                ret.addAll(RulesDeployHelper.splitRootClassNamesBindingClasses((String) rootClassNamesBinding));
            } else {
                if (rootClassNamesBinding != null) {
                    throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                            ProjectJacksonObjectMapperFactoryBean.ROOT_CLASS_NAMES_BINDING,
                            openLService.getName()));
                }
            }
        }

        XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openLService.getOpenClass();
        if (xlsModuleOpenClass != null) {
            boolean found = false;
            Set<String> sprBeanClassNames = new HashSet<>();
            Class<?> serviceClass = openLService.getServiceClass();
            for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
                if (type instanceof DatatypeOpenClass) {
                    ret.add(type.getInstanceClass().getName());
                }
                if (type instanceof CustomSpreadsheetResultOpenClass) {
                    Class<?> beanClass = ((CustomSpreadsheetResultOpenClass) type).getBeanClass();
                    sprBeanClassNames.add(beanClass.getName());
                    if (!found) {
                        for (Method method : serviceClass.getMethods()) {
                            if (!found && ClassUtils.isAssignable(beanClass, method.getReturnType())) {
                                found = true;
                            }
                        }
                    }
                }
            }
            Class<?> spreadsheetResultBeanClass = xlsModuleOpenClass
                .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                .toCustomSpreadsheetResultOpenClass()
                .getBeanClass();
            sprBeanClassNames.add(spreadsheetResultBeanClass.getName());
            if (!found) {
                for (Method method : serviceClass.getMethods()) {
                    if (!found && ClassUtils.isAssignable(spreadsheetResultBeanClass, method.getReturnType())) {
                        found = true;
                    }
                }
            }
            if (found) {
                ret.addAll(sprBeanClassNames);
            }
        }
        return ret;
    }
}
