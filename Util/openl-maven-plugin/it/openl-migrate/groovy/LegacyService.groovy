import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
interface LegacyService {
    @GET
    @Path("/echo/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @XmlElement
    String echo(@PathParam("name") String name)
}
