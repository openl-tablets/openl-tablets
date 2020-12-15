package org.openl.rules.rest;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;
import org.springframework.stereotype.Service;

@Service
@Produces(MediaType.APPLICATION_JSON)
@Path("/public")
public class SysInfoService {

    @GET
    @Path("/info/sys.json")
    public Map<String, Object> getSysInfo() {
        return SysInfo.get();
    }

    @GET
    @Path("/info/openl.json")
    public Map<String, String> getOpenLInfo() {
        return OpenLVersion.get();
    }
}
