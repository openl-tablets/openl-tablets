package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "return-row")
public class ReturnRow {
    private List<ExpressionImpl> list = new ArrayList<ExpressionImpl>();

    @XmlElementWrapper(name="expressions", required = true)
    @XmlElement(name = "expression")
    public List<ExpressionImpl> getList() {
        return list;
    }

    public void setList(List<ExpressionImpl> list) {
        this.list = list;
    }
}
