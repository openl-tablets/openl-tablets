package org.openl.rules.model.scaffolding.data;

import java.util.Objects;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.Model;
import org.openl.rules.model.scaffolding.PathInfo;

public class DataModel implements Model {

    private final String name;
    private final String type;
    private final PathInfo info;
    private final DatatypeModel datatypeModel;

    public DataModel(String name, String type, PathInfo info, DatatypeModel dataType) {
        this.name = name;
        this.type = type;
        this.info = info;
        this.datatypeModel = dataType;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public PathInfo getInfo() {
        return info;
    }

    public DatatypeModel getDatatypeModel() {
        return datatypeModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataModel dataModel = (DataModel) o;

        if (!Objects.equals(name, dataModel.name)) {
            return false;
        }
        if (!Objects.equals(type, dataModel.type)) {
            return false;
        }
        if (!Objects.equals(info, dataModel.info)) {
            return false;
        }
        return Objects.equals(datatypeModel, dataModel.datatypeModel);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        result = 31 * result + (datatypeModel != null ? datatypeModel.hashCode() : 0);
        return result;
    }
}
