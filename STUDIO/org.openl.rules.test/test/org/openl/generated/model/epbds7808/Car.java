/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.model.epbds7808;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.ArrayUtils;
import java.lang.String;
import java.lang.Integer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace="http://epbds7808.model.generated.openl.org", name="Car")
@XmlType(namespace="http://epbds7808.model.generated.openl.org", name="Car")
public class Car implements Serializable {
  protected String name;

  protected Integer value;


  public Car() {
    super();
  }

  public Car(String name, Integer value) {
    super();
    this.name = name;
    this.value = value;
  }

  public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Car)) {;
        return false;
    }
    Car another = (Car)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getValue(),getValue());
    return builder.isEquals();
  }

  @XmlElement(name="name", nillable=true)
  public String getName() {
    return name;
  }

  @XmlElement(name="value", nillable=true)
  public Integer getValue() {
    return value;
  }

  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getName());
    builder.append(getValue());
    return builder.toHashCode();
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(Integer value) {
    this.value = value;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Car {");
    builder.append(" name=");
    builder.append(getName());
    builder.append(" value=");
    builder.append(getValue());
    builder.append(" }");
    return builder.toString();
  }

}