package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "table-ranges")
public class TableRanges {
    private Range verticalConditionsRange;
    private Range horizontalConditionsRange;
    private Range returnValuesRange;

    @XmlElement(name = "vertical-conditions-range")
    public Range getVerticalConditionsRange() {
        return verticalConditionsRange;
    }

    public void setVerticalConditionsRange(Range verticalConditionsRange) {
        this.verticalConditionsRange = verticalConditionsRange;
    }

    @XmlElement(name = "horizontal-conditions-range")
    public Range getHorizontalConditionsRange() {
        return horizontalConditionsRange;
    }

    public void setHorizontalConditionsRange(Range horizontalConditionsRange) {
        this.horizontalConditionsRange = horizontalConditionsRange;
    }

    @XmlElement(name = "return-values-range")
    public Range getReturnValuesRange() {
        return returnValuesRange;
    }

    public void setReturnValuesRange(Range returnValuesRange) {
        this.returnValuesRange = returnValuesRange;
    }
}
