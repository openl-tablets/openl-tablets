import org.apache.cxf.jaxrs.ext.multipart.Multipart
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType

interface Service {

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ServiceExtraMethod(CsvFileInterceptor.class)
    Response importDataFromCsvFile(@Multipart(value = "json", type = MediaType.APPLICATION_JSON) Request json,
                                   @Multipart(value = "csv", type = "text/csv") InputStream csv)
}
