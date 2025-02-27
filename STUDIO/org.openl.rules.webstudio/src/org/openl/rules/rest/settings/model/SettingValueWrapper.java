package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class SettingValueWrapper {

    @Parameter(description = "Setting value")
    public Object value;

    @Parameter(description = "Is setting read-only. If true, the setting set by the system env and cannot be changed.")
    public boolean readOnly;

}
