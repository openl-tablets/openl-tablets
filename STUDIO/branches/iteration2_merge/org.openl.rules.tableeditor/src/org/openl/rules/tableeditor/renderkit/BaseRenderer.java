package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.openl.rules.tableeditor.util.WebUtil;

public abstract class BaseRenderer extends Renderer {

    protected void encodeJS(UIComponent component, ResponseWriter writer,
            String jsPath) throws IOException {
        writer.startElement("script", component);
        writer.writeAttribute("language", "JavaScript", null);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeAttribute("src", WebUtil.internalPath(jsPath), null);
        writer.endElement("script");
    }

    protected void encodeCSS(UIComponent component, ResponseWriter writer,
            String cssPath) throws IOException {
        writer.startElement("link", component);
        writer.writeAttribute("rel", "stylesheet", null);
        writer.writeAttribute("type", "text/css", null);
        writer.writeAttribute("href", WebUtil.internalPath(cssPath), null);
        writer.endElement("link");
    }

}
