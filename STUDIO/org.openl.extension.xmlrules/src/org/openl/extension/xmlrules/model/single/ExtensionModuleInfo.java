package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="model")
@XmlType(name = "model")
public class ExtensionModuleInfo {
    private String formatVersion;
    private List<WorkbookInfo> workbooks = new ArrayList<WorkbookInfo>();

    @XmlElement(name = "formatVersion", required = true)
    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    @XmlElement(name = "workbook")
    public List<WorkbookInfo> getWorkbooks() {
        return workbooks;
    }

    public void setWorkbooks(List<WorkbookInfo> workbooks) {
        this.workbooks = workbooks;
    }
}
