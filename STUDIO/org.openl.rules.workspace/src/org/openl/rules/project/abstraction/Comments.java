package org.openl.rules.project.abstraction;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Map;

import org.openl.util.StringUtils;

public final class Comments {
    private final String saveProjectTemplate;
    private final String createProjectTemplate;
    private final String archiveProjectTemplate;
    private final String restoreProjectTemplate;
    private final String eraseProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;

    public Comments(Map<String, Object> properties, String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix can't be null");
        }

        saveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.save")
                .toString()
                .replace("{project-name}", "{0}");
        createProjectTemplate = properties.get(prefix + "comment-template.user-message.default.create")
                .toString()
                .replace("{project-name}", "{0}");
        archiveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.archive")
                .toString()
                .replace("{project-name}", "{0}");
        restoreProjectTemplate = properties.get(prefix + "comment-template.user-message.default.restore")
                .toString()
                .replace("{project-name}", "{0}");
        eraseProjectTemplate = properties.get(prefix + "comment-template.user-message.default.erase")
                .toString()
                .replace("{project-name}", "{0}");
        copiedFromTemplate = properties.get(prefix + "comment-template.user-message.default.copied-from")
                .toString()
                .replace("{project-name}", "{0}");
        restoredFromTemplate = properties.get(prefix + "comment-template.user-message.default.restored-from")
                .toString()
                .replace("{revision}", "{0}");
    }

    public String saveProject(String projectName) {
        return MessageFormat.format(saveProjectTemplate, projectName);
    }

    public String createProject(String projectName) {
        return MessageFormat.format(createProjectTemplate, projectName);
    }

    public String archiveProject(String projectName) {
        return MessageFormat.format(archiveProjectTemplate, projectName);
    }

    public String restoreProject(String projectName) {
        return MessageFormat.format(restoreProjectTemplate, projectName);
    }

    public String eraseProject(String projectName) {
        return MessageFormat.format(eraseProjectTemplate, projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return MessageFormat.format(copiedFromTemplate, sourceProjectName);
    }

    public String parseSourceOfCopy(String comment) {
        if (StringUtils.isBlank(comment)) {
            return "";
        }

        Object[] parse;
        try {
            parse = new MessageFormat(copiedFromTemplate).parse(comment);
        } catch (ParseException e) {
            return "";
        }
        return parse.length > 0 ? parse[0].toString() : "";
    }

    public String restoredFrom(String revisionNum) {
        return MessageFormat.format(restoredFromTemplate, revisionNum);
    }
}
