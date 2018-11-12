package org.openl.itest.epbds7947.project;

import org.openl.generated.beans.Policy;

public interface MainService {

    String checkValidation(Policy policy);
    String checkArrayValidation(Policy[] policies);
    String checkArrayValidationFromParent(Policy[] policies);
    String getGender(String gender);

}
