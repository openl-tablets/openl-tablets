package org.openl.rules.table.xls.formatters;

import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.formatters.IFormatter;

public class SegmentFormatter {
    
    private IFormatter formatter;   

    private short[] color;

    private double multiplier = 1;    

    private int alignment = ICellStyle.ALIGN_GENERAL;
    
    public SegmentFormatter() {
        // TODO Auto-generated constructor stub
    }

    public SegmentFormatter(IFormatter format, short[] color) {
        this.formatter = format;
        this.color = color;
    }

    public SegmentFormatter(IFormatter format, short[] color, int alignment) {
        this.formatter = format;
        this.color = color;
        this.alignment = alignment;
    }

    public Object parse(String value) {
        return formatter.parse(value);
    }
    
    public void setFormatter(IFormatter format) {
        this.formatter = format;
    }
    
    public IFormatter getFormatter() {
        return formatter;
    }
    
    public void setColor(short[] color) {
        this.color = color;
    }
    
    public short[] getColor() {
        return color;
    }
    
    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
    
    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}