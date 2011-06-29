package org.openl.rules.tablets.tutorial4.client.jsf;

import org.openl.rules.tablets.tutorial4.client.WebServiceTemplate;
import org.openl.rules.tablets.tutorial4.client.WebServiceCallback;
import org.openl.rules.tablets.tutorial4.client.Tutorial4ClientInterface;

import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

public abstract class AbstractBean<T> {
	public <T> T getResult() {
		try {
			return (T) WebServiceTemplate.getInstance().run(new WebServiceCallback() {
				public Object doAction(Tutorial4ClientInterface client) {
					return perform(client);
				}
			});
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
			return null;
		}
	}

	public abstract <T> T perform(Tutorial4ClientInterface client);
}
