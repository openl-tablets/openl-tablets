package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.table.ui.ICellStyle;

public class SpreadsheetResultTableStyle extends DefaultTableStyle implements TableStyle {

    private ICellStyle stepHeaderStyle;
    private ICellStyle valueHeaderStyle;
    private String stepHeaderText;
    private String valueHeaderText;

    private ICellStyle stepNameStyle;
    private ICellStyle stepValueStyle;

    public SpreadsheetResultTableStyle(String headerTextTemplate,
            ICellStyle headerStyle,
            CellRangeSettings headerSettings,
            ICellStyle stepHeaderStyle,
            ICellStyle valueHeaderStyle,
            String stepHeaderText,
            String valueHeaderText,
            ICellStyle stepNameStyle,
            ICellStyle stepValueStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.stepHeaderStyle = stepHeaderStyle;
        this.valueHeaderStyle = valueHeaderStyle;
        this.stepHeaderText = stepHeaderText;
        this.valueHeaderText = valueHeaderText;
        this.stepNameStyle = stepNameStyle;
        this.stepValueStyle = stepValueStyle;
    }

    public ICellStyle getStepHeaderStyle() {
        return stepHeaderStyle;
    }

    public void setStepHeaderStyle(ICellStyle stepHeaderStyle) {
        this.stepHeaderStyle = stepHeaderStyle;
    }

    public ICellStyle getValueHeaderStyle() {
        return valueHeaderStyle;
    }

    public void setValueHeaderStyle(ICellStyle valueHeaderStyle) {
        this.valueHeaderStyle = valueHeaderStyle;
    }

    public String getStepHeaderText() {
        return stepHeaderText;
    }

    public void setStepHeaderText(String stepHeaderText) {
        this.stepHeaderText = stepHeaderText;
    }

    public String getValueHeaderText() {
        return valueHeaderText;
    }

    public void setValueHeaderText(String valueHeaderText) {
        this.valueHeaderText = valueHeaderText;
    }

    public ICellStyle getStepNameStyle() {
        return stepNameStyle;
    }

    public void setStepNameStyle(ICellStyle stepNameStyle) {
        this.stepNameStyle = stepNameStyle;
    }

    public ICellStyle getStepValueStyle() {
        return stepValueStyle;
    }

    public void setStepValueStyle(ICellStyle stepValueStyle) {
        this.stepValueStyle = stepValueStyle;
    }
}
