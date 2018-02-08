package org.openl.rules.ruleservice.databinding;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 - 2014 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

public class JAXRSArgumentWrapperGenerator {

	Map<String, Class<?>> props = new HashMap<String, Class<?>>();
	String xmlTypeName;
	String xmlTypeNamespace;

	public JAXRSArgumentWrapperGenerator() {
	}

	public JAXRSArgumentWrapperGenerator(String xmlTypeName, String namespace) {
		this.xmlTypeName = xmlTypeName;
		this.xmlTypeNamespace = namespace;
	}

	public String getXmlTypeName() {
		return xmlTypeName;
	}

	public String getXmlTypeNamespace() {
		return xmlTypeNamespace;
	}

	public void setXmlTypeName(String xmlTypeName) {
		this.xmlTypeName = xmlTypeName;
	}

	public void setXmlTypeNamespace(String xmlTypeNamespace) {
		this.xmlTypeNamespace = xmlTypeNamespace;
	}

	public void addProperty(String name, Class<?> type) {
		props.put(name, type);
	}

	public Class<?> generateClass() throws Exception {
		return generateClass(Thread.currentThread().getContextClassLoader());
	}

	public Class<?> generateClass(ClassLoader classLoader) throws Exception {
		BeanGeneratorWithJAXBAnnotations beanGenerator = new BeanGeneratorWithJAXBAnnotations();
		for (String name : props.keySet()) {
			beanGenerator.addProperty(name, props.get(name));
		}
		beanGenerator.setClassLoader(classLoader);
		beanGenerator.setXmlTypeName(xmlTypeName);
		beanGenerator.setXmlTypeNamespace(xmlTypeNamespace);

		Class<?> generatedClass = (Class<?>) beanGenerator.createClass();
		return generatedClass;
	}
}
