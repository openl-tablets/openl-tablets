/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class Address{
  protected java.lang.String state;

  protected java.lang.String street1;

  protected java.lang.String street2;

  protected java.lang.String city;

  protected java.lang.String zip;



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

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(street1);
    builder.append(street2);
    builder.append(city);
    builder.append(state);
    builder.append(zip);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Address)) {;
        return false;
    }
    Address another = (Address)obj;
    builder.append(another.street1,street1);
    builder.append(another.street2,street2);
    builder.append(another.city,city);
    builder.append(another.state,state);
    builder.append(another.zip,zip);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Address {");
    builder.append(" street1=");
    builder.append(street1);
    builder.append(" street2=");
    builder.append(street2);
    builder.append(" city=");
    builder.append(city);
    builder.append(" state=");
    builder.append(state);
    builder.append(" zip=");
    builder.append(zip);
    builder.append(" }");
    return builder.toString();
}
  public java.lang.String getState() {
   return state;
}
  public void setState(java.lang.String state) {
   this.state = state;
}
  public java.lang.String getStreet1() {
   return street1;
}
  public void setStreet1(java.lang.String street1) {
   this.street1 = street1;
}
  public java.lang.String getStreet2() {
   return street2;
}
  public void setStreet2(java.lang.String street2) {
   this.street2 = street2;
}
  public java.lang.String getCity() {
   return city;
}
  public void setCity(java.lang.String city) {
   this.city = city;
}
  public java.lang.String getZip() {
   return zip;
}
  public void setZip(java.lang.String zip) {
   this.zip = zip;
}

}