package org.openl.rules.testmethod;

import org.openl.message.OpenLMessage;
import org.openl.rules.testmethod.result.ComparedResult;

import java.util.List;

public interface ITestUnit {

    String DEFAULT_DESCRIPTION = "No Description";

    Object getExpectedResult();
    Object getActualResult();
    long getExecutionTime();
    ParameterWithValueDeclaration getActualParam();
    ParameterWithValueDeclaration[] getContextParams(TestUnitsResults objTestResult);
    List<ComparedResult> getResultParams();
    String getDescription();
    List<ComparedResult> getComparisonResults();
    TestStatus getResultStatus();
    TestDescription getTest();
    List<OpenLMessage> getErrors();


}
