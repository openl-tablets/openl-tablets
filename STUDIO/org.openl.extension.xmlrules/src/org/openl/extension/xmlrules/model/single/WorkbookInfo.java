package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="workbook")
@XmlType(name = "workbook")
public class WorkbookInfo {
    private String xlsFileName;
    private List<SheetInfo> sheets = new ArrayList<SheetInfo>();

    @XmlElement(name = "xls-file", required = true)
    public String getXlsFileName() {
        return xlsFileName;
    }

    public void setXlsFileName(String xlsFileName) {
        this.xlsFileName = xlsFileName;
    }

    @XmlElement(name = "sheet")
    public List<SheetInfo> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetInfo> sheets) {
        this.sheets = sheets;
    }
}
