package org.openl.rules.ui.repository.beans;

import java.util.Date;
import java.util.List;

public interface Entity {

    public abstract String getName();

    public abstract String getVersion();

    public abstract Date getLastModified();

    public abstract String getLastModifiedBy();

    public abstract List<VersionBean> getVersionHistory();

    public abstract Date getCreated();

    public abstract List<AbstractEntityBean> getElements();

    public abstract void delete();

}