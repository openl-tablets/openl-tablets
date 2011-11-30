package org.openl.rules.ui;

import javax.faces.component.html.HtmlOutputText;

import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.util.EnumUtils;

/**
 * Helper to display enum values on UI as combobox or multiselect editors. 
 *
 */
public class EnumValuesUIHelper {
    
    private HtmlOutputText enumOutput;
    private HtmlOutputText enumArrayOutput;
    
    public EnumValuesUIHelper() {}
    
    public void setEnumOutput(HtmlOutputText enumOutput) {
        this.enumOutput = enumOutput;
    }

    public HtmlOutputText getEnumOutput() {
        return enumOutput;
    }

    public void setEnumArrayOutput(HtmlOutputText enumArrayOutput) {
        this.enumArrayOutput = enumArrayOutput;
    }

    public HtmlOutputText getEnumArrayOutput() {
        return enumArrayOutput;
    }

    public String getEnumValue() {
        String componentId = enumOutput.getId();
        TableProperty property = (TableProperty) enumOutput.getAttributes().get("property");

        return getEnumSelectComponentCode(componentId, property);
    }

    public String getEnumArrayValue() {
        String componentId = enumArrayOutput.getId();
        TableProperty property = (TableProperty) enumArrayOutput.getAttributes().get("property");

        return getEnumMultiSelectComponentCode(componentId, property);
    }

    private String getEnumSelectComponentCode(String componentId, TableProperty tableProperty) {
        Class<?> instanceClass = tableProperty.getType();
        String value = tableProperty.getStringValue();

        String[] values = EnumUtils.getNames(instanceClass);
        String[] displayValues = EnumUtils.getValues(instanceClass);

        String id = String.format("%s:%s:enumSelect", componentId, tableProperty.getName());

        String componentCode = new HTMLRenderer().getSingleSelectComponentCode(id, values, displayValues, value);

        return getEditorHTMLCode(id, componentCode);
    }

    private String getEnumMultiSelectComponentCode(String componentId, TableProperty tableProperty) {

        Class<?> instanceClass = tableProperty.getType().getComponentType();

        String valueString = tableProperty.getStringValue();

        String[] values = EnumUtils.getNames(instanceClass);
        String[] displayValues = EnumUtils.getValues(instanceClass);

        String id = String.format("%s:%s:enumArraySelect", componentId, tableProperty.getName());

        String componentCode = new HTMLRenderer().getMultiSelectComponentCode(id, values, displayValues, valueString);

        return getEditorHTMLCode(id, componentCode);
    }

    private String getEditorHTMLCode(String id, String editorCode) {
        return String.format(
                  "<div id='%1$s'></div>"
                + "<script>"
                + "var editor = %2$s;"
                // editor value setter code
                + "editor.input.onblur=function(){var newValue = this.getValue();"
                + "$j('#%3$s').next('input[type=hidden][name!=id]').val(newValue);return false;};"
                + "</script>", id, editorCode, id.replaceAll(":", "\\\\\\\\:"));
    }

}
