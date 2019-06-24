package org.openl.rules.webstudio.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

/**
 * Servlet filter to load web resources (images, html, etc). First, attempt is made to load resource from classpath and
 * then from web application root.
 *
 * @author Andrey Naumenko
 */
public class WebResourceFilter implements Filter {
    private static final String WEBRESOURCE_PREFIX = "/webresource";
    private static final Pattern JSESSION_ID_PATTERN = Pattern.compile("^(.+?);jsessionid=\\w+$",
        Pattern.CASE_INSENSITIVE);

    private FilterConfig filterConfig;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (path != null && path.contains(WEBRESOURCE_PREFIX)) {
            Matcher matcher = JSESSION_ID_PATTERN.matcher(path);
            if (matcher.matches()) {
                path = matcher.group(1);
            }

            // When "webresource/**" is requested from html page which was
            // loaded via "webresource/**" itself
            // the path will contain 2 "webresource" strings.
            // "lastIndexOf" cuts off all prefixes at once.
            path = path.substring(path.lastIndexOf(WEBRESOURCE_PREFIX) + WEBRESOURCE_PREFIX.length());

            String extension = FileUtils.getExtension(path);
            switch (extension) {
                case "css":
                    response.setContentType("text/css");
                    break;
                case "js":
                    response.setContentType("text/javascript");
                    break;
                case "gif":
                    response.setContentType("image/gif");
                    break;
                case "png":
                    response.setContentType("image/png");
                    break;
                default:
                    // Do not process other resources, such as .class or .properties
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;

            }

            try (InputStream stream = initializeInputStream(path)) {
                OutputStream out = response.getOutputStream();
                IOUtils.copy(stream, out);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    private InputStream initializeInputStream(String path) throws FileNotFoundException {
        InputStream stream = WebResourceFilter.class.getResourceAsStream(path);
        if (stream == null) {
            stream = new FileInputStream(new File(filterConfig.getServletContext().getRealPath(path)));
        }
        return stream;
    }
}
