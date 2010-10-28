/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class CoverageBase{
  protected java.lang.String symbol;

  protected double baseBI;

  protected double basePD;

  protected double baseMP;



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

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(symbol);
    builder.append(baseBI);
    builder.append(basePD);
    builder.append(baseMP);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof CoverageBase)) {;
        return false;
    }
    CoverageBase another = (CoverageBase)obj;
    builder.append(another.symbol,symbol);
    builder.append(another.baseBI,baseBI);
    builder.append(another.basePD,basePD);
    builder.append(another.baseMP,baseMP);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CoverageBase {");
    builder.append(" symbol=");
    builder.append(symbol);
    builder.append(" baseBI=");
    builder.append(baseBI);
    builder.append(" basePD=");
    builder.append(basePD);
    builder.append(" baseMP=");
    builder.append(baseMP);
    builder.append(" }");
    return builder.toString();
}
  public java.lang.String getSymbol() {
   return symbol;
}
  public void setSymbol(java.lang.String symbol) {
   this.symbol = symbol;
}
  public double getBaseBI() {
   return baseBI;
}
  public void setBaseBI(double baseBI) {
   this.baseBI = baseBI;
}
  public double getBasePD() {
   return basePD;
}
  public void setBasePD(double basePD) {
   this.basePD = basePD;
}
  public double getBaseMP() {
   return baseMP;
}
  public void setBaseMP(double baseMP) {
   this.baseMP = baseMP;
}

}