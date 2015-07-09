package org.openl.extension.xmlrules.model.lazy;

import java.util.ArrayList;
import java.util.List;

public class ExtensionModuleInfo {
    private String formatVersion;
    private String xlsFileName;
    private List<String> sheetEntries = new ArrayList<String>();

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getXlsFileName() {
        return xlsFileName;
    }

    public void setXlsFileName(String xlsFileName) {
        this.xlsFileName = xlsFileName;
    }

    public List<String> getSheetEntries() {
        return sheetEntries;
    }

    public void setSheetEntries(List<String> sheetEntries) {
        this.sheetEntries = sheetEntries;
    }
}
