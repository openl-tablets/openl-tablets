package org.openl.meta;

public class ValueMetaInfo implements IMetaInfo {

    String shortName;
    String fullName;
    String sourceUrl;

    public ValueMetaInfo() {
    }

    public ValueMetaInfo(String shortName, String fullName, String sourceUrl) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.sourceUrl = sourceUrl;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.meta.IMetaInfo#getDisplayValue(int)
     */
    public String getDisplayName(int mode) {
        switch (mode) {
            case SHORT:
            case REGULAR:
                return shortName;
            case LONG:
            default:
                return fullName == null ? shortName : fullName;

        }
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

}
