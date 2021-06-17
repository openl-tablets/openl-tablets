package org.openl.rules.webstudio.web.admin;

import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.service.TagTemplateService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class TagTemplateBean {
    private final TagTemplateService tagTemplateService;
    private String templates;

    public TagTemplateBean(TagTemplateService tagTemplateService) {
        this.tagTemplateService = tagTemplateService;
        final List<String> templates = tagTemplateService.getTemplates();
        this.templates = StringUtils.join(templates.toArray(new String[0]), "\n");
    }

    public String getTemplates() {
        return templates;
    }

    public void setTemplates(String templates) {
        this.templates = templates;
    }

    public void templateValidator(FacesContext context, UIComponent component, Object value) {
        final String[] templates = StringUtils.toLines((String) value);
        if (templates == null) {
            return;
        }
        for (String template : templates) {
            if (StringUtils.isBlank(template)) {
                continue;
            }
            final String message = tagTemplateService.validate(template);
            WebStudioUtils.validate(StringUtils.isBlank(message), message);
        }
    }

    public void save() {
        String[] lines = StringUtils.toLines(templates);
        if (lines == null) {
            lines = new String[0];
        }
        tagTemplateService.save(Arrays.asList(lines));

        WebStudioUtils.addInfoMessage("Project name template was saved successfully.");
    }
}
