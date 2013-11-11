package org.openl.rules.demo.webservice.client.jsf;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.openl.rules.demo.webservice.client.WebServiceTemplate;

@ManagedBean
@RequestScoped
public class WSBean {

    private String[] result;
	private String methodName;

	private int age = 20;
	private String gender = "Male";

	private int numDUI;
	private int numAccidents;
	private int numMovingViolations;

	public int getNumDUI() {
        return numDUI;
    }
	
	public int getNumAccidents() {
        return numAccidents;
    }
	
	public int getNumMovingViolations() {
        return numMovingViolations;
    }
	
	public void setNumDUI(int numDUI) {
        this.numDUI = numDUI;
    }
	
	public void setNumAccidents(int numAccidents) {
        this.numAccidents = numAccidents;
    }
	
	public void setNumMovingViolations(int numMovingViolations) {
        this.numMovingViolations = numMovingViolations;
    }
	
	public String[] getResult() {
		return result;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getAge() {
        return age;
    }
	
	public String getGender() {
        return gender;
    }
	
	public void setAge(int age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() throws Exception {
        return WebServiceTemplate.getInstance().getAddress();
    }

    public void driverRisk() {
        methodName = "DriverRisk"; 
        String ret = (String) invoke(methodName,
                new Object[] {null, new Integer(numDUI), new Integer(numAccidents), new Integer(numMovingViolations) });
        result = new String[] { ret };
    }

    public void accidentPremium() {
        methodName = "AccidentPremium";
        Double ret = (Double) invoke(methodName, new Object[] { null });
        result = new String[] { ret != null ? String.valueOf(ret) : null };
    }

    public void driverAgeType() {
        methodName = "DriverAgeType";
        String ret = (String) invoke(methodName, new Object[] { null, gender, age });
        result = new String[] { ret };
    }

    public Object invoke(String method, Object[] params) {
        try {
            WebServiceTemplate ws = WebServiceTemplate.getInstance();
            return ws.getClientInterface().invoke(method, params)[0];
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
    }

}
