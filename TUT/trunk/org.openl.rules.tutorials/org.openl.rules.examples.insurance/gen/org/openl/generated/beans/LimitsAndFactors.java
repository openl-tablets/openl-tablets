/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class LimitsAndFactors{
  protected java.lang.String limit;

  protected double increasedFactor;



public LimitsAndFactors() {
    super();
}

public LimitsAndFactors(String limit, double increasedFactor) {
    super();
    this.limit = limit;
    this.increasedFactor = increasedFactor;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(limit);
    builder.append(increasedFactor);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof LimitsAndFactors)) {;
        return false;
    }
    LimitsAndFactors another = (LimitsAndFactors)obj;
    builder.append(another.limit,limit);
    builder.append(another.increasedFactor,increasedFactor);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LimitsAndFactors {");
    builder.append(" limit=");
    builder.append(limit);
    builder.append(" increasedFactor=");
    builder.append(increasedFactor);
    builder.append(" }");
    return builder.toString();
}
  public java.lang.String getLimit() {
   return limit;
}
  public void setLimit(java.lang.String limit) {
   this.limit = limit;
}
  public double getIncreasedFactor() {
   return increasedFactor;
}
  public void setIncreasedFactor(double increasedFactor) {
   this.increasedFactor = increasedFactor;
}

}