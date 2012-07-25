package org.openl.rules.project.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Configuration {
	private Collection<Property> properties = new ArrayList<Property>();

	public Collection<Property> getProperties() {
		return properties;
	}

	public void setProperties(Collection<Property> properties) {
		this.properties.addAll(properties);
	}

	public String getPropertyValue(String propertyName) {
		for (Property p : properties) {
			if (p.getName().equals(propertyName)) {
				return p.getValue();
			}
		}
		return null;
	}

	public void addProperty(String propertyName, String value) {
		removeProperty(propertyName);
		Property property = new Property(propertyName, value);
		properties.add(property);
	}

	public void removeProperty(String propertyName) {
		Iterator<Property> itr = properties.iterator();
		while (itr.hasNext()) {
			Property p = itr.next();
			if (p.getName().equals(propertyName)) {
				itr.remove();
				break;
			}
		}
	}

	public void clear() {
		properties.clear();
	}
}
