/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
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
  public java.lang.String getLimit() {
   return limit;
}
  public double getIncreasedFactor() {
   return increasedFactor;
}
  public void setLimit(java.lang.String limit) {
   this.limit = limit;
}
  public void setIncreasedFactor(double increasedFactor) {
   this.increasedFactor = increasedFactor;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof LimitsAndFactors)) {;
        return false;
    }
    LimitsAndFactors another = (LimitsAndFactors)obj;
    builder.append(another.getLimit(),getLimit());
    builder.append(another.getIncreasedFactor(),getIncreasedFactor());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LimitsAndFactors {");
    builder.append(" limit=");
    builder.append(getLimit());
    builder.append(" increasedFactor=");
    builder.append(getIncreasedFactor());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getLimit());
    builder.append(getIncreasedFactor());
    return builder.toHashCode();
}

}