package org.openl.internal.verify.lib;

import javax.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import java.util.Date;
import org.apache.cxf.jaxrs.utils.HttpUtils;

@Provider
public class Foo {

    private ApplicationContext applicationContext;

    private String bar;

    public Foo() {
    }

    public Foo(String bar) {
        this.bar = bar;
    }

    public String getBar() {
        return bar + " " + getDate();
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    private String getDate() {
        return HttpUtils.toHttpDate(new Date());
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}