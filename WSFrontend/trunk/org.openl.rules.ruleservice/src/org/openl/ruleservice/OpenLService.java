package org.openl.ruleservice;

import java.util.List;

import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;


public class OpenLService {
    private String name;
    private String url;
    private List<Module> modules;
    private String serviceClassName;
    private RulesInstantiationStrategy instantiationStrategy;
}
