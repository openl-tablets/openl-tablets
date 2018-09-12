package org.openl.itest.rules;

import org.openl.generated.beans.Username;

public interface TestHttpStatusService {

    void throwUserException();

    Double throwOpenLException();

    String throwValidationException();

    String hello(Username username);

    void throwNPE();

    Integer throwNFE();

}
