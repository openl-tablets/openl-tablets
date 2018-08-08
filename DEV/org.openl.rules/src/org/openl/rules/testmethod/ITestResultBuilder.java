package org.openl.rules.testmethod;

public interface ITestResultBuilder {

    ITestUnit build(TestDescription test, Object res, Throwable error, long executionTime);

}
