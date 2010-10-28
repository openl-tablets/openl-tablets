/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class Discount{
  protected double value;

  protected java.lang.String type;

  protected boolean showInPolicy;



public Discount() {
    super();
}

public Discount(String type, double value, boolean showInPolicy) {
    super();
    this.type = type;
    this.value = value;
    this.showInPolicy = showInPolicy;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(type);
    builder.append(value);
    builder.append(showInPolicy);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Discount)) {;
        return false;
    }
    Discount another = (Discount)obj;
    builder.append(another.type,type);
    builder.append(another.value,value);
    builder.append(another.showInPolicy,showInPolicy);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Discount {");
    builder.append(" type=");
    builder.append(type);
    builder.append(" value=");
    builder.append(value);
    builder.append(" showInPolicy=");
    builder.append(showInPolicy);
    builder.append(" }");
    return builder.toString();
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
  public boolean getShowInPolicy() {
   return showInPolicy;
}
  public void setShowInPolicy(boolean showInPolicy) {
   this.showInPolicy = showInPolicy;
}

}