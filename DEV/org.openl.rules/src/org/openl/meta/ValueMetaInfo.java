package org.openl.meta;

import lombok.Getter;
import lombok.Setter;

import org.openl.source.IOpenSourceCodeModule;

public class ValueMetaInfo implements IMetaInfo {

    @Getter
    @Setter
    private String shortName;
    @Getter
    @Setter
    private String fullName;
    @Getter
    @Setter
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

    @Override
    public String getSourceUrl() {
        if (source != null) {
            return source.getUri();
        } else {
            return null;
        }
    }
}
