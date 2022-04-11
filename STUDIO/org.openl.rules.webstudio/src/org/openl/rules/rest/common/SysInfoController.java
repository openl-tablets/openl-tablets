package org.openl.rules.rest.common;

import java.util.Map;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Info")
public class SysInfoController {

    @Operation(summary = "info.get-sys-info.summary", description = "info.get-sys-info.desc")
    @GetMapping(value = "/public/info/sys.json")
    public Map<String, Object> getSysInfo() {
        return SysInfo.get();
    }

    @Operation(summary = "info.get-openl-info.summary", description = "info.get-openl-info.desc")
    @GetMapping(value = "/public/info/openl.json")
    public Map<String, String> getOpenLInfo() {
        return OpenLVersion.get();
    }

    @Operation(summary = "info.get-build-info.summary", description = "info.get-build-info.desc")
    @GetMapping(value = "/public/info/build.json")
    public Map<Object, Object> getBuildInfo() {
        return OpenLVersion.getBuildInfo();
    }
}
