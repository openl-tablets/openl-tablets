package org.openl.rules.webstudio.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Resolves the set of script and stylesheet URLs produced by the Vite build so that JSF pages can
 * include the React UI without knowing the hashed asset filenames in advance.
 *
 * <p>Production: reads the Vite manifest from the classpath (META-INF/resources/.vite/manifest.json)
 * and returns relative URLs that resolve against the page's {@code <base href>}.
 *
 * <p>Dev: when {@code _REACT_UI_ROOT_} is set (typically to {@code http://localhost:3100} for the
 * Vite dev server), returns the Vite client + TypeScript entry for HMR.
 */
@Component("viteAssets")
public class ViteAssetsBean {

    private static final String VITE_MANIFEST_RESOURCE = "/META-INF/resources/.vite/manifest.json";
    private static final String VITE_MANIFEST_ENTRY = "index.html";

    private final Environment environment;
    private final ObjectMapper objectMapper;

    private String html;

    public ViteAssetsBean(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        var devServerRoot = environment.getProperty("_REACT_UI_ROOT_");
        if (devServerRoot != null && !devServerRoot.isBlank()) {
            var root = devServerRoot.endsWith("/") ? devServerRoot.substring(0, devServerRoot.length() - 1) : devServerRoot;
            html = buildHtml("""
                    <script type="module">
                      import RefreshRuntime from '%s/@react-refresh'
                      RefreshRuntime.injectIntoGlobalHook(window)
                      window.$RefreshReg$ = () => {}
                      window.$RefreshSig$ = () => (type) => type
                      window.__vite_plugin_react_preamble_installed__ = true
                    </script>
                    """.formatted(root), List.of(), List.of(root + "/@vite/client", root + "/src/index.tsx"));
            return;
        }

        try (InputStream in = getClass().getResourceAsStream(VITE_MANIFEST_RESOURCE)) {
            if (in == null) {
                throw new BeanInitializationException(
                    "Vite manifest not found on classpath at " + VITE_MANIFEST_RESOURCE);
            }
            Map<String, ViteManifestEntry> manifest = objectMapper.readValue(in, new TypeReference<>() {});
            var entry = manifest.get(VITE_MANIFEST_ENTRY);
            if (entry == null || entry.file() == null) {
                throw new BeanInitializationException(
                    "Vite manifest is missing the '" + VITE_MANIFEST_ENTRY + "' entry");
            }
            var styles = entry.css() != null ? entry.css() : List.<String>of();
            html = buildHtml("", styles, List.of(entry.file()));
        } catch (IOException e) {
            throw new BeanInitializationException("Failed to read Vite manifest", e);
        }
    }

    public String getHtml() {
        return html;
    }

    private static String buildHtml(String html, List<String> styles, List<String> scripts) {
        var sb = new StringBuilder(html);
        for (var style : styles) {
            sb.append("<link rel=\"stylesheet\" href=\"").append(style).append("\" />");
        }
        for (var script : scripts) {
            sb.append("<script type=\"module\" src=\"").append(script).append("\"></script>");
        }
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViteManifestEntry(String file, List<String> css) {
    }
}
