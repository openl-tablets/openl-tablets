package org.openl.meta;

public interface IMetaInfo {

    int SHORT = 0;
    int REGULAR = 1;
    int LONG = 2;

    String getDisplayName(int mode);

    String getSourceUrl();

}
