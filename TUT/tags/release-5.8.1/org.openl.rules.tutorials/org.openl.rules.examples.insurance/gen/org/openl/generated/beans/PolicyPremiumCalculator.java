/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import java.util.Vector;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class PolicyPremiumCalculator{
  protected java.util.Vector discounts;

  protected java.util.Vector rejections;



public PolicyPremiumCalculator() {
    super();
}

public PolicyPremiumCalculator(Vector discounts, Vector rejections) {
    super();
    this.discounts = discounts;
    this.rejections = rejections;
}
  public java.util.Vector getDiscounts() {
   return discounts;
}
  public java.util.Vector getRejections() {
   return rejections;
}
  public void setDiscounts(java.util.Vector discounts) {
   this.discounts = discounts;
}
  public void setRejections(java.util.Vector rejections) {
   this.rejections = rejections;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof PolicyPremiumCalculator)) {;
        return false;
    }
    PolicyPremiumCalculator another = (PolicyPremiumCalculator)obj;
    builder.append(another.getDiscounts(),getDiscounts());
    builder.append(another.getRejections(),getRejections());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PolicyPremiumCalculator {");
    builder.append(" discounts=");
    builder.append(getDiscounts());
    builder.append(" rejections=");
    builder.append(getRejections());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getDiscounts());
    builder.append(getRejections());
    return builder.toHashCode();
}

}