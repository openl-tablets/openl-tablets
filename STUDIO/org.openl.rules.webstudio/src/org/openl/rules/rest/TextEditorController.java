package org.openl.rules.rest;

import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.TextEditorService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.*;

@RestController
@RequestMapping(value = "/text-editor/")
public class TextEditorController {

    private final TextEditorService textEditorService;

    @Autowired
    public TextEditorController(TextEditorService textEditorService) {
        this.textEditorService = textEditorService;
    }

    @GetMapping(value = "file", produces = "text/plain;charset=UTF-8")
    public String getFile(HttpSession session) throws IOException {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        try {
            return textEditorService.readFile(webStudio);
        } catch (FileNotFoundException fileNotFoundException) {
            throw new NotFoundException("text-editor.message", webStudio.getCurrentProjectDescriptor().getFileName());
        }
    }

    @PostMapping(value = "file", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void saveFile(@RequestBody String content, HttpSession session) throws IOException {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        textEditorService.saveFile(content, webStudio);
    }
}
