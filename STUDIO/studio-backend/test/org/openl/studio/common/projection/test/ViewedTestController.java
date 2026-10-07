package org.openl.studio.common.projection.test;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller whose class-level {@code @JsonView} triggers
 * {@link org.openl.studio.common.JsonViewControllerAdvice}, so the test can verify that field projection
 * runs on top of an active serialization view.
 */
@RestController
@RequestMapping("/viewed")
@JsonView(Views.Public.class)
public class ViewedTestController {

    @GetMapping("/single")
    public ViewedTestView single() {
        return new ViewedTestView("1", "name-1", "OPENED");
    }
}
