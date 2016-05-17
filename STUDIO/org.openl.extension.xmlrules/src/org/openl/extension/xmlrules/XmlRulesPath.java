package org.openl.extension.xmlrules;

public final class XmlRulesPath {
    private final String workbook;
    private final String sheet;

    public XmlRulesPath(String workbook, String sheet) {
        this.workbook = workbook;
        this.sheet = sheet;
    }

    public String getWorkbook() {
        return workbook;
    }

    public String getSheet() {
        return sheet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        XmlRulesPath that = (XmlRulesPath) o;

        if (!workbook.equals(that.workbook))
            return false;
        return sheet.equals(that.sheet);

    }

    @Override
    public int hashCode() {
        int result = workbook.hashCode();
        result = 31 * result + sheet.hashCode();
        return result;
    }
}
