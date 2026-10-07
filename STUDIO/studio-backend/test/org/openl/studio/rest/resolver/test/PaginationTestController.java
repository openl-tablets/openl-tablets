package org.openl.studio.rest.resolver.test;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.repository.api.Offset;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;
import org.openl.studio.rest.resolver.PaginationDefault;

@RestController
@RequestMapping("/pagination-test")
@RequiredArgsConstructor
public class PaginationTestController {

    private final Consumer<Pageable> pageableConsumer;

    @GetMapping("/pageOrOffset")
    public void getPage(Pageable page) {
        pageableConsumer.accept(page);
    }

    @GetMapping("/pageWithDefault")
    public void getPageWithDefaults(@PaginationDefault(page = 1, size = 50) Page page) {
        pageableConsumer.accept(page);
    }

    @GetMapping("/offsetWithDefault")
    public void getOffsetWithDefaults(@PaginationDefault(offset = 4, size = 50) Offset page) {
        pageableConsumer.accept(page);
    }

    @GetMapping("/pageableWithDefault")
    public void getPageableWithDefaults(@PaginationDefault(page = 2, size = 30) Pageable page) {
        pageableConsumer.accept(page);
    }

}
