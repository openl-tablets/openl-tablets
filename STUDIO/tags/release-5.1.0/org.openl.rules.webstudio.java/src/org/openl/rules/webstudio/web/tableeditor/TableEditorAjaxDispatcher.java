package org.openl.rules.webstudio.web.tableeditor;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

public class TableEditorAjaxDispatcher implements PhaseListener {
	private static final long serialVersionUID = 3584912417430412375L;
	
	private static String PREFIX = null;

	public void afterPhase(PhaseEvent event) {
	}

	public void beforePhase(PhaseEvent event) {
		if (PREFIX == null)
			PREFIX = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
					  .getContextPath() +  "/faces/ajax/";
		
		if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			HttpServletRequest request = ((HttpServletRequest) event.getFacesContext().getExternalContext().getRequest());
			if (request.getRequestURI().startsWith(PREFIX)) {
				FacesContext context = event.getFacesContext();

				MethodBinding methodBinding = context.getApplication()
						.createMethodBinding(makeMehtodBindingString(request.getRequestURI().substring(PREFIX.length())),
									 new Class[0]);

				NavigationHandler nh = context.getApplication().getNavigationHandler();
				nh.handleNavigation(context, "", (String) methodBinding.invoke(context, new Object[0]));
			}
		}
	}

	private String makeMehtodBindingString(String request) {
		int pos = request.indexOf('?');
		if (pos >= 0) request = request.substring(0, pos);
		return new StringBuilder("#{tableEditorController.").append(request).append('}').toString();
	}

	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}
}

