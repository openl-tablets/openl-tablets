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

import org.apache.commons.lang3.StringUtils;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

public class JAXRSArgumentWrapperGenerator {

	Map<String, Class<?>> props = new HashMap<String, Class<?>>();
	String xmlTypeName;
	String xmlTypeNamespace;
	String prefix;

	public JAXRSArgumentWrapperGenerator() {
	}

	public JAXRSArgumentWrapperGenerator(String xmlTypeName, String namespace) {
		this(xmlTypeName, namespace, null);
	}

	public JAXRSArgumentWrapperGenerator(String xmlTypeName, String namespace, String prefix) {
		this.xmlTypeName = xmlTypeName;
		this.xmlTypeNamespace = namespace;
		this.prefix = prefix;
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
		beanGenerator.setNamingPolicy(new JAXRSArgumentWrapperGeneratorNamingPolicy(prefix));
		for (String name : props.keySet()) {
			beanGenerator.addProperty(name, props.get(name));
		}
		beanGenerator.setClassLoader(classLoader);
		beanGenerator.setXmlTypeName(xmlTypeName);
		beanGenerator.setXmlTypeNamespace(xmlTypeNamespace);

		Class<?> generatedClass = (Class<?>) beanGenerator.createClass();
		return generatedClass;
	}

	public static class JAXRSArgumentWrapperGeneratorNamingPolicy implements NamingPolicy {

		private String methodPrefix;

		public JAXRSArgumentWrapperGeneratorNamingPolicy(String methodPrefix) {
			this.methodPrefix = methodPrefix;
		}

		public String getClassName(String prefix, String source, Object key, Predicate names) {
			if (methodPrefix == null) {
				prefix = "Request";
			} else {
				prefix = StringUtils.capitalize(methodPrefix) + "Request";
			}
			String base = prefix + "$$" + Integer.toHexString(key.hashCode());
			String attempt = base;
			int index = 2;
			while (names.evaluate(attempt))
				attempt = base + "_" + index++;
			return attempt;
		}
	}

}
