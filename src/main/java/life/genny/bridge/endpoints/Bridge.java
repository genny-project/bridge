package life.genny.bridge.endpoints;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.bridge.exception.BridgeException;
import life.genny.bridge.model.InitProperties;

/**
 * Bridge ---Endpoints consisting in providing model data from the model life.genny.bridge.model
 * also there are some endpoints that handle the data that has been blacklisted
 *
 * @author    hello@gada.io
 *
 */
@Path("/")
public class Bridge {

    private static final Logger LOG = Logger.getLogger(Bridge.class);
    @Inject BlackListInfo blackList;
    @Context UriInfo uriInfo;

    /**
     * The entrypoint for external clients who wants to establish a connection 
     * with the backend. The client will need be informed after calling this 
     * endpoint with all the protocol and information for subsequent calls
     *
     * @param url The url passed as a query parameter in the url path. it will be 
     * used to retrieve information in cache and verify there is a realm 
     * associated with the url
     *
     * @return InitProperties object will all required information so the clients
     * gets informed about the protocol for future communication 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/api/events/init")
    public Response configObject() {
        try {
            return Response.ok(new InitProperties(uriInfo.getBaseUri().toString())).build();
        } catch (BridgeException e) {
            LOG.error("The configuration does not exist or cannot be find please check the ENVs");
            e.printStackTrace();
            return Response.status(404).build();
        }
    }

    /**
     * Receives a post request from an external client with a token so a session id 
     * can be extracted from the payload after decoding it. Then it gets registered
     * in the event bus and it is used to pusblish messages to the external channel 
     * 
     * @param auth Authorization header with bearer token
     *
     * @return a confirmation result 
     */

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user"})
    @Path("/api/events/init")
    @Deprecated(since = "9.9.0", forRemoval = true)
    public JsonObject initChannelSession(@HeaderParam("Authorization") String auth) {
        return new JsonObject().put("result","confirmed");
    }

    /**
     * A request with a DELETE verb will delete all the blacklisted records.
     * This can only happen if the user normally admin or sec have the right
     * permissions.
     *
     * @return 200 
     */
    @DELETE
    @RolesAllowed({"ptest,test"})
    @Path("/admin/blacklist")
    public Response deleteAllBlackListedRecords() {
        LOG.warn("Deleting all blacklisted records");
        blackList.deleteAll();
        return Response.ok().build();
    }

    /**
     * A request with a DELETE verb will delete only the blacklisted record associated
     * to the parameter uuid. This can only happen if the user normally admin or sec 
     * have the right permissions.
     *
     * @return 200 
     */
    @DELETE
    @RolesAllowed({"ptest,test"})
    @Path("/admin/blacklist/{uuid}")
    public Response deleteBlackListedRecord(@PathParam UUID uuid) {
        LOG.warn("Deleting blacklisted record {"+uuid+"}");
        blackList.deleteRecord(uuid);
        return Response.ok().build();
    }

    /**
     * A request with a PUT verb will add, delete all or delete just a record depending on 
     * the protocol specified in the parameter. The protocol consist of the following:
     *      - Just a dash/minus (-)
     *      - A dash/minus appended with a {@link UUID} (-UUID.toString())
     *      - A {@link UUID} (UUID.toString())
     * This can only happen if the user normally admin or sec 
     * have the right permissions.
     *
     * @return 200 
     */

    @PUT
    @RolesAllowed({"ptest","test","admin"})
    @Path("/admin/blacklist/{protocol}")
    public Response addBlackListedRecord(@PathParam String protocol) {
        LOG.warn("Received a protocol {"+protocol+"} the blacklist map will be handled" +
               " accordingly");
        blackList.onReceived(protocol);
        return Response.ok().build();
    }

    /**
     * 
     * A GET request to get all the blacklisted UUIDS that are currently registered
     *
     *
     * @return An array of uniques UUIDs
     */
    @GET
    @RolesAllowed({"service,test"})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/admin/blacklists")
    public Set<String> getBlackListedRecords() {
        LOG.warn("Getting all blacklisted records");
        return blackList.getBlackListedUUIDs().stream()
            .map(d ->d.toString())
            .collect(Collectors.toSet());
    }
    /**
     * 
     * A GET request to get all the blacklisted UUIDS that are currently registered
     *
     *
     * @return An array of uniques UUIDs
     */
    @GET
    @RolesAllowed({"user"})
    @Produces(MediaType.APPLICATION_JSON)
    //@Path("/admin/blacklists")
    public Set<String> getB2BHandler() {
        LOG.warn("Getting all blacklisted records");
        return blackList.getBlackListedUUIDs().stream()
            .map(d ->d.toString())
            .collect(Collectors.toSet());
    }
}

