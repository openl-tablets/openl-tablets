/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class Discount{
  protected boolean showInPolicy;

  protected double value;

  protected java.lang.String type;



public Discount() {
    super();
}

public Discount(String type, double value, boolean showInPolicy) {
    super();
    this.type = type;
    this.value = value;
    this.showInPolicy = showInPolicy;
}
  public boolean getShowInPolicy() {
   return showInPolicy;
}
  public void setShowInPolicy(boolean showInPolicy) {
   this.showInPolicy = showInPolicy;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Discount)) {;
        return false;
    }
    Discount another = (Discount)obj;
    builder.append(another.getType(),getType());
    builder.append(another.getValue(),getValue());
    builder.append(another.getShowInPolicy(),getShowInPolicy());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Discount {");
    builder.append(" type=");
    builder.append(getType());
    builder.append(" value=");
    builder.append(getValue());
    builder.append(" showInPolicy=");
    builder.append(getShowInPolicy());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getType());
    builder.append(getValue());
    builder.append(getShowInPolicy());
    return builder.toHashCode();
}
  public double getValue() {
   return value;
}
  public java.lang.String getType() {
   return type;
}
  public void setValue(double value) {
   this.value = value;
}
  public void setType(java.lang.String type) {
   this.type = type;
}

}