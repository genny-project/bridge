package life.genny.bridge.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

/**
 * DummyVersion --- An endpoint normally used by public facing clients
 * such as alyson to retrieve information about this project. 
 *
 * @author    hello@gada.io
 *
 */
@Path("/api/version")
public class DummyVersion {
	
    /**
     * The endpoint returns information specific to this project 
     * information such as commit hash, last user who commit and 
     * version number of this project are included
     *
     *
     * @return GitApplicationProperties 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject dummy() {
        return new JsonObject().put("version", "Not supported");
    }
}
