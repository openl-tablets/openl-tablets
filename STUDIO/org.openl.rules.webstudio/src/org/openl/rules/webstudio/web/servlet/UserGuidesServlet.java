package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Answers the addresses of the user guides OpenL Studio ships at {@code /docs}.
 *
 * <p>A file of the guides, a page or an image, is handed to the container, which sets its type and the date it was
 * last modified. A browser keeps the file and asks on every visit whether it changed. A file is public, as any other
 * static resource; a page of a guide is the application page, guarded as every other page.
 *
 * <p>{@code /docs/toc.json} answers with the table of contents. Every other address is a page of a guide, which the
 * application page draws, unless it ends with a file extension: it then names a file the guides do not hold, and is
 * not found.
 *
 * <p>The guides are read once, on the first request: the war holds the same files until it is replaced.
 *
 * @author Yury Molchan
 */
@WebServlet(UserGuidesServlet.PATH + "/*")
public class UserGuidesServlet extends FrontendPageServlet {

    /** The address OpenL Studio serves the guides at. */
    public static final String PATH = "/docs";

    /** Where the war keeps the guides: the {@code studio-docs} jar maps them under the web application root. */
    private static final String GUIDES = PATH + "/";

    /** The address of the table of contents, a name no file of the guides may take. */
    private static final String CONTENTS = "/toc.json";

    private static final ObjectMapper JSON = new ObjectMapper();

    private Set<String> files = Set.of();
    private byte[] contentsJson = new byte[0];

    public UserGuidesServlet() {
        super("/index.html", "/src/index.tsx");
    }

    @Override
    public void init() throws ServletException {
        super.init();
        var guides = UserGuides.read(getServletContext(), GUIDES);
        files = guides.files();
        try {
            contentsJson = JSON.writeValueAsBytes(guides.contents());
        } catch (JsonProcessingException e) {
            throw new ServletException("Failed to write the table of contents of the user guides", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        var path = requireNonNullElse(req.getPathInfo(), "/");
        if (path.equals(CONTENTS)) {
            writeContents(resp);
        } else if (files.contains(path)) {
            resp.setHeader("Cache-Control", "no-cache");
            StaticResourcesServlet.serve(getServletContext(), req, resp);
        } else if (namesFile(path)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            super.doGet(req, resp);
        }
    }

    /**
     * Whether an address below the guides names a file: its last part ends with an extension.
     *
     * <p>Such an address is answered with a file of the guides, the table of contents, or not found - never with the
     * application page.
     */
    public static boolean namesFile(String path) {
        return path.lastIndexOf('.') > path.lastIndexOf('/');
    }

    private void writeContents(HttpServletResponse resp) {
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // Asked again on every visit, so a newer war never shows the guides of an older one.
            resp.setHeader("Cache-Control", "no-cache");
            resp.getOutputStream().write(contentsJson);
        } catch (IOException e) {
            failed(resp, e);
        }
    }
}
