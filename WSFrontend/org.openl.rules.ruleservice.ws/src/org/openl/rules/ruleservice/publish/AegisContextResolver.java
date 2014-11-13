package org.openl.rules.ruleservice.publish;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.databinding.AegisDatabinding;

@Provider
@Consumes({ MediaType.APPLICATION_JSON, "application/*+json", "application/xml", "application/*+xml", "text/xml" })
@Produces({ MediaType.APPLICATION_JSON, "application/*+json", "application/xml", "application/*+xml", "text/xml" })
public class AegisContextResolver implements ContextResolver<AegisContext> {

    @Override
    public AegisContext getContext(Class<?> type) {
        return aegisDatabinding.getAegisContext();
    }

    private AegisDatabinding aegisDatabinding;

    public AegisContextResolver(AegisDatabinding databinding) {
        this.aegisDatabinding = databinding;
    }
}
