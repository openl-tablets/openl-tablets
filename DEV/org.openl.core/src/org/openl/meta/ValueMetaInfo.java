package org.openl.meta;

import org.openl.source.IOpenSourceCodeModule;

public class ValueMetaInfo implements IMetaInfo {

    private String shortName;
    private String fullName;
    private IOpenSourceCodeModule source;

    public ValueMetaInfo() {
    }

    public ValueMetaInfo(String shortName, String fullName, IOpenSourceCodeModule source) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.source = source;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.meta.IMetaInfo#getDisplayValue(int)
     */
    @Override
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

    @Override
    public String getSourceUrl() {
        if (source != null) {
            return source.getUri();
        } else {
            return null;
        }
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setSource(IOpenSourceCodeModule source) {
        this.source = source;
    }

    public IOpenSourceCodeModule getSource() {
        return source;
    }
}
