package org.openl.meta;



public class DoubleValueCompatibility  {

    public static Double round(Double d, double p) {

        int scale;
        double preRoundedValue;

        if (p == 0) {
            scale = 0;
            preRoundedValue = d.doubleValue();
        } else {
            scale = (int) org.apache.commons.math.util.MathUtils.round(-Math.log10(p),
                0,
                java.math.BigDecimal.ROUND_HALF_UP);
            preRoundedValue = d.doubleValue();
            // preRoundedValue = Math.round(d.doubleValue() / p.doubleValue()) *
            // p.doubleValue();
        }

        double roundedValue = org.apache.commons.math.util.MathUtils.round(preRoundedValue, scale);

        return roundedValue;
    }
    
    
    static public Double Constructor(double value, IMetaInfo metaInfo, String format) {
        return  value;
    }


    
    public void setFormat(Double d, String format) {
    }	
    
    
    public double getValue(Double d) {
        return d;
    }

}
