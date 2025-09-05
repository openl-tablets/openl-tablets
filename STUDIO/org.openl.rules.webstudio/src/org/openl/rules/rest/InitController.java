package org.openl.rules.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.ui.WebStudio;

/**
 * This controller is used to initialize the WebStudio with the OpenL project context.
 *
 * @deprecated This controller is deprecated due it is an internal API and will be changed in the future.
 */
@RestController
@Hidden
@Deprecated
public class InitController {

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping("/init")
    public void init(@RequestParam(value = "repositoryId", required = false) String repositoryId,
                     @RequestParam(value = "branch", required = false) String branchName,
                     @RequestParam(value = "project", required = false) String projectName,
                     @RequestParam(value = "module", required = false) String moduleName) {
        getWebStudio().init(repositoryId, branchName, projectName, moduleName);
    }
}
