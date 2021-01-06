package org.openl.rules.model.scaffolding;

import java.util.List;

public interface MethodModel extends Model {

    PathInfo getPathInfo();
    String getType();
    List<InputParameter> getParameters();
}
