package org.openl.rules.webstudio.web;

import lombok.Getter;
import lombok.Setter;

public class ModuleInfoDTO {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private String type;

    public ModuleInfoDTO(String name, String path, String type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }
}
