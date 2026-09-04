package org.openl.rules.model.scaffolding.data;

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.MethodModel;
import org.openl.rules.model.scaffolding.PathInfo;

public class DataModel implements MethodModel {

    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private final PathInfo pathInfo;
    @Getter
    private final DatatypeModel datatypeModel;
    @Getter
    @Setter
    private boolean include;

    public DataModel(String name, String type, PathInfo info, DatatypeModel dataType) {
        this.name = name;
        this.type = type;
        this.pathInfo = info;
        this.datatypeModel = dataType;
    }

    @Override
    public List<InputParameter> getParameters() {
        return List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataModel dataModel)) {
            return false;
        }

        if (!Objects.equals(name, dataModel.name)) {
            return false;
        }
        if (!Objects.equals(type, dataModel.type)) {
            return false;
        }
        if (!Objects.equals(pathInfo, dataModel.pathInfo)) {
            return false;
        }
        return Objects.equals(datatypeModel, dataModel.datatypeModel);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (pathInfo != null ? pathInfo.hashCode() : 0);
        result = 31 * result + (datatypeModel != null ? datatypeModel.hashCode() : 0);
        return result;
    }
}
