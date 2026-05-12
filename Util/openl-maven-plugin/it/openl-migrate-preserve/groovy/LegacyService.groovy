import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
interface LegacyService {
    @GET
    @Path("/echo/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @XmlElement
    String echo(@PathParam("name") String name)
}
