package life.genny;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.bridge.exception.BridgeException;
import life.genny.bridge.model.InitProperties;

/**
 * Bridge --- Custom exception to identified in-house commun 
 * issues which were faced before, known issues or expected problems that 
 * can be documented 
 *
 * @author    hello@gada.io
 *
 */
@Path("/")
public class Bridge {

    private static final Logger LOG = Logger.getLogger(Bridge.class);
    @Inject BlackListInfo blackList;

    /**
     * The entrypoint for external clients who wants to establish a connection 
     * with the backend. The client will need be informed after calling this 
     * endpoint with all the protocol and information for subsequent calls
     *
     * @param url The url passed as a query parameter in the url path. it will be 
     * used to retrieve information in cache and verify there is a realm 
     * associated with the url
     *
     * @return [TODO:description]
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/api/events/init")
    public Response configObject(@QueryParam("url") String url) {
        try {
            return Response.ok(new InitProperties()).build();
        } catch (BridgeException e) {
            LOG.error("The configuration does not exist or cannot be find please check the ENVs");
            e.printStackTrace();
            return Response.status(404).build();
        }
    }

    /**
     * [TODO:description]
     *
     * @param auth [TODO:description]
     *
     * @return [TODO:description]
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user"})
    @Path("/api/events/init")
    public JsonObject initChannelSession(@HeaderParam("Authorization") String auth) {
        return new JsonObject().put("result","confirmed");
    }

    /**
     * [TODO:description]
     *
     * @return [TODO:description]
     */
    @DELETE
    @RolesAllowed({"admin"})
    @Path("/admin/blacklist")
    public Response deleteAllBlackListedRecords() {
        LOG.warn("Deleting all blacklisted records");
        blackList.deleteAll();
        return Response.ok().build();
    }

    /**
     * [TODO:description]
     *
     * @param uuid [TODO:description]
     *
     * @return [TODO:description]
     */
    @DELETE
    @RolesAllowed({"admin"})
    @Path("/admin/blacklist/{uuid}")
    public Response deleteBlackListedRecord(@PathParam UUID uuid) {
        LOG.warn("Deleting blacklisted record {"+uuid+"}");
        blackList.deleteRecord(uuid);
        return Response.ok().build();
    }

    /**
     * 
     *
     * @param uuid [TODO:description]
     *
     * @return [TODO:description]
     */
    @PUT
    @RolesAllowed({"admin"})
    @Path("/admin/blacklist/{uuid}")
    public Response addBlackListedRecord(@PathParam String uuid) {
        LOG.warn("Adding a new record {"+uuid+"} blacklisted record");
        blackList.onReceived(uuid);
        return Response.ok().build();
    }

    /**
     * 
     *
     * @param uuid [TODO:description]
     *
     * @return [TODO:description]
     */
    @GET
    @RolesAllowed({"admin"})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ops/blacklists")
    public Set<String> getBlackListedRecords(@PathParam String uuid) {
        LOG.warn("Getting all blacklisted records");
        return blackList.getBlackListedUUIDs().stream()
            .map(d ->d.toString())
            .collect(Collectors.toSet());
    }
}

