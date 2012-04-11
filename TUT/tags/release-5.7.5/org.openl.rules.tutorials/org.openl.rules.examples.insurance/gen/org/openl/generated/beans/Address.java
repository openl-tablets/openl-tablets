/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class Address{
  protected java.lang.String street1;

  protected java.lang.String street2;

  protected java.lang.String city;

  protected java.lang.String zip;

  protected java.lang.String state;



public Address() {
    super();
}

public Address(String street1, String street2, String city, String state, String zip) {
    super();
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.state = state;
    this.zip = zip;
}
  public java.lang.String getStreet1() {
   return street1;
}
  public java.lang.String getStreet2() {
   return street2;
}
  public java.lang.String getCity() {
   return city;
}
  public java.lang.String getZip() {
   return zip;
}
  public void setStreet1(java.lang.String street1) {
   this.street1 = street1;
}
  public void setStreet2(java.lang.String street2) {
   this.street2 = street2;
}
  public void setCity(java.lang.String city) {
   this.city = city;
}
  public void setZip(java.lang.String zip) {
   this.zip = zip;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Address)) {;
        return false;
    }
    Address another = (Address)obj;
    builder.append(another.getStreet1(),getStreet1());
    builder.append(another.getStreet2(),getStreet2());
    builder.append(another.getCity(),getCity());
    builder.append(another.getState(),getState());
    builder.append(another.getZip(),getZip());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Address {");
    builder.append(" street1=");
    builder.append(getStreet1());
    builder.append(" street2=");
    builder.append(getStreet2());
    builder.append(" city=");
    builder.append(getCity());
    builder.append(" state=");
    builder.append(getState());
    builder.append(" zip=");
    builder.append(getZip());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getStreet1());
    builder.append(getStreet2());
    builder.append(getCity());
    builder.append(getState());
    builder.append(getZip());
    return builder.toHashCode();
}
  public java.lang.String getState() {
   return state;
}
  public void setState(java.lang.String state) {
   this.state = state;
}

}