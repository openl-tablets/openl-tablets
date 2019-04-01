package org.openl.rules.lang.xls.types;

import java.util.Objects;

import org.openl.meta.IMetaInfo;

/**
 * Implementation of {@link IMetaInfo} for datatypes. First of all to handle the url to source. Display name is
 * implemented to return the same name for all modes. Should be updated if needed.
 *
 * @author DLiauchuk TODO: Replace with org.openl.meta.TableMetaInfo
 */
public class DatatypeMetaInfo implements IMetaInfo {

    private String displayName;
    private String sourceUrl;

    public DatatypeMetaInfo(String displayName, String sourceUrl) {
        this.displayName = displayName;
        this.sourceUrl = sourceUrl;
    }

    @Override
    public String getDisplayName(int mode) {
        /*
         * Default implementation. Don`t know if we need any displayName for Datatype.
         *
         * @author DLiauchuk
         */
        return displayName;
    }

    public String getDisplayName() {
        return getDisplayName(0);
    }

    @Override
    public String getSourceUrl() {
        return sourceUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, sourceUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DatatypeMetaInfo other = (DatatypeMetaInfo) obj;

        return Objects.equals(displayName, other.getDisplayName()) && Objects.equals(sourceUrl, other.getSourceUrl());
    }

}
