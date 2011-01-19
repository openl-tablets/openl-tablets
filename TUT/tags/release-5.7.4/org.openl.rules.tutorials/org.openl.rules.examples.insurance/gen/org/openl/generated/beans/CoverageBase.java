/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class CoverageBase{
  protected double baseBI;

  protected double basePD;

  protected double baseMP;

  protected java.lang.String symbol;



public CoverageBase() {
    super();
}

public CoverageBase(String symbol, double baseBI, double basePD, double baseMP) {
    super();
    this.symbol = symbol;
    this.baseBI = baseBI;
    this.basePD = basePD;
    this.baseMP = baseMP;
}
  public double getBaseBI() {
   return baseBI;
}
  public double getBasePD() {
   return basePD;
}
  public double getBaseMP() {
   return baseMP;
}
  public void setSymbol(java.lang.String symbol) {
   this.symbol = symbol;
}
  public void setBaseBI(double baseBI) {
   this.baseBI = baseBI;
}
  public void setBasePD(double basePD) {
   this.basePD = basePD;
}
  public void setBaseMP(double baseMP) {
   this.baseMP = baseMP;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof CoverageBase)) {;
        return false;
    }
    CoverageBase another = (CoverageBase)obj;
    builder.append(another.getSymbol(),getSymbol());
    builder.append(another.getBaseBI(),getBaseBI());
    builder.append(another.getBasePD(),getBasePD());
    builder.append(another.getBaseMP(),getBaseMP());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CoverageBase {");
    builder.append(" symbol=");
    builder.append(getSymbol());
    builder.append(" baseBI=");
    builder.append(getBaseBI());
    builder.append(" basePD=");
    builder.append(getBasePD());
    builder.append(" baseMP=");
    builder.append(getBaseMP());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getSymbol());
    builder.append(getBaseBI());
    builder.append(getBasePD());
    builder.append(getBaseMP());
    return builder.toHashCode();
}
  public java.lang.String getSymbol() {
   return symbol;
}

}