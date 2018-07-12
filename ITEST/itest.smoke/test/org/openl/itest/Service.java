package org.openl.itest;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(targetNamespace = "http://DefaultNamespace")
public interface Service {

    String ping();

    Integer twice(@WebParam(name="num")Integer num);

    Integer mul(@WebParam(name="x")int a, @WebParam(name="y")Integer b);

    void absent();
}
