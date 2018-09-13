package org.openl.itest.project1;

import org.openl.generated.my1.Policy;
import org.openl.rules.calc.SpreadsheetResult;

public interface Project1Service {

    SpreadsheetResult calculation(Policy myPolicy);

    org.openl.generated.my2.Policy getProject2FirstPolicy();
    Policy getProject1FirstPolicy();

}
