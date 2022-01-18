package org.openl.rules.rest.common;

import java.util.Map;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class SysInfoController {

    @GetMapping(value = "/public/info/sys.json")
    public Map<String, Object> getSysInfo() {
        return SysInfo.get();
    }

    @GetMapping(value = "/public/info/openl.json")
    public Map<String, String> getOpenLInfo() {
        return OpenLVersion.get();
    }
}
