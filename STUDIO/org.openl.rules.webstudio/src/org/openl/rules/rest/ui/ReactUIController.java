package org.openl.rules.rest.ui;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping({"/administration", "/claim", "/help"})
public class ReactUIController {

    @Lookup("reactUiRoot")
    public Supplier<String> getReactUiRoot() {
        throw new IllegalStateException();
    }

    @RequestMapping("/**")
    public ModelAndView handleRequests() {
        var view = new ModelAndView("react-ui");
        view.addObject("reactUiRoot", getReactUiRoot().get());
        return view;
    }

}
