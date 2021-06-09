package org.openl.rules.model.scaffolding;

import java.util.List;

public interface MethodModel extends Model {

    PathInfo getPathInfo();
    String getType();
    List<InputParameter> getParameters();
    boolean isInclude();
    void setInclude(boolean include);

    default String getMethodFilterPattern() {
        StringBuilder builder = new StringBuilder(".+ ")
                .append(getPathInfo().getFormattedPath())
                .append("\\(");
        if (!getParameters().isEmpty()) {
            builder.append(".+");
        } else if (getPathInfo().getRuntimeContextParameter() != null) {
            builder.append(".*");
        }
        builder.append("\\)");

        return builder.toString();
    }
}
