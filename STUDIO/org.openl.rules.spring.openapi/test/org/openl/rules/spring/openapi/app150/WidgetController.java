package org.openl.rules.spring.openapi.app150;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/widgets")
public class WidgetController {

    @GetMapping
    @JsonView(Widget.View.Base.class)
    public List<Widget> list() {
        return null;
    }

    @GetMapping("/{id}")
    @JsonView(Widget.View.Extended.class)
    public Widget get(@PathVariable Long id) {
        return null;
    }
}
