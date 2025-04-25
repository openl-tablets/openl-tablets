package org.openl.rules.rest.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping({"/administration", "/claim"})
public class ReactUIController {

    @RequestMapping("/**")
    public ModelAndView handleRequests() {
        return new ModelAndView("react-ui");
    }

}
