package org.openl.rules.project.model;

public final class Property {
	private String name;
	private String value;
	
	public Property(){}

	public Property(String name, String value) {
		if (name == null)
			throw new IllegalArgumentException("name argument can't be null");
		if (value == null)
			throw new IllegalArgumentException("value argument can't be null");
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Property) {
			Property property = (Property) obj;
			return this.name.equals(property.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

}
