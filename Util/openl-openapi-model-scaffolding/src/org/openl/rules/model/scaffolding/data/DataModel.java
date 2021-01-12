package org.openl.rules.model.scaffolding.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.MethodModel;
import org.openl.rules.model.scaffolding.PathInfo;

public class DataModel implements MethodModel {

    private final String name;
    private final String type;
    private final PathInfo pathInfo;
    private final DatatypeModel datatypeModel;
    private boolean include;

    public DataModel(String name, String type, PathInfo info, DatatypeModel dataType) {
        this.name = name;
        this.type = type;
        this.pathInfo = info;
        this.datatypeModel = dataType;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public PathInfo getPathInfo() {
        return pathInfo;
    }

    public DatatypeModel getDatatypeModel() {
        return datatypeModel;
    }

    @Override
    public List<InputParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public boolean isInclude() {
        return include;
    }

    @Override
    public void setInclude(boolean include) {
        this.include = include;
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
