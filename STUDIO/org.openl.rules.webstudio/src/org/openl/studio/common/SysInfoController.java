package org.openl.studio.common;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;

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

    @Operation(summary = "info.get-http-info.summary", description = "info.get-http-info.desc")
    @RequestMapping(value = "/public/info/http.json")
    public Map<Object, Object> getHttpInfo(HttpServletRequest request,
                                           @Parameter(hidden = true) @RequestHeader HttpHeaders headers) {
        LinkedHashMap<Object, Object> info = new LinkedHashMap<>();

        info.put("Protocol", request.getProtocol());
        info.put("Method", request.getMethod());
        info.put("RequestURL", request.getRequestURL());

        info.put("Scheme", request.getScheme());
        info.put("Secure", request.isSecure());
        info.put("ServerName", request.getServerName());
        info.put("ServerPort", request.getServerPort());
        info.put("LocalName", request.getLocalName());
        info.put("LocalAddr", request.getLocalAddr());
        info.put("LocalPort", request.getLocalPort());
        info.put("RemoteHost", request.getRemoteHost());
        info.put("RemoteAddr", request.getRemoteAddr());
        info.put("RemotePort", request.getRemotePort());

        info.put("RequestURI", request.getRequestURI());
        info.put("ContextPath", request.getContextPath());
        info.put("ServletPath", request.getServletPath());
        info.put("PathInfo", request.getPathInfo());

        info.put("QueryString", request.getQueryString());
        info.put("Parameters", request.getParameterMap());

        ServletContext servletContext = request.getServletContext();
        LinkedHashMap<Object, Object> context = new LinkedHashMap<>();
        context.put("ContextPath", servletContext.getContextPath());
        context.put("ServerInfo", servletContext.getServerInfo());
        context.put("ServletContextName", servletContext.getServletContextName());
        context.put("VirtualServerName", servletContext.getVirtualServerName());
        info.put("ServletContext", context);

        info.put("RequestedSessionId", request.getRequestedSessionId());
        info.put("RequestedSessionIdValid", request.isRequestedSessionIdValid());
        info.put("RequestedSessionIdFromCookie", request.isRequestedSessionIdFromCookie());
        info.put("RequestedSessionIdFromURL", request.isRequestedSessionIdFromURL());
        info.put("Cookies", request.getCookies());

        info.put("CharacterEncoding", request.getCharacterEncoding());
        info.put("ContentType", request.getContentType());
        info.put("ContentLength", request.getContentLengthLong());
        info.put("Locale", request.getLocale());
        info.put("Locales", request.getLocales());

        info.put("RemoteUser", request.getRemoteUser());
        info.put("UserPrincipal", request.getUserPrincipal());
        info.put("AuthType", request.getAuthType());

        info.put("Headers", headers);

        return info;
    }
}
