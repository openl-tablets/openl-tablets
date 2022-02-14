import org.apache.cxf.jaxrs.ext.multipart.Multipart
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType

interface Service {

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ServiceExtraMethod(CsvFileInterceptor.class)
    Response importDataFromCsvFile(@Multipart(value = "json", type = MediaType.APPLICATION_JSON) Request json,
                                   @Multipart(value = "csv", type = "text/csv") InputStream csv)
}
