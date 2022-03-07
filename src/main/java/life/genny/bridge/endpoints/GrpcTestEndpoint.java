package life.genny.bridge.endpoints;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.vertx.mutiny.core.http.HttpServerRequest;
import life.genny.bridge.live.data.ExternalConsumerService;
import life.genny.bridge.model.GennyToken;
import life.genny.bridge.model.grpc.Item;

/**
 * This is a test endpoint for working with GRPC.
 * DO NOT COMMIT TO THE FINAL VERSION
 */
@Path("/api/grpctest")
public class GrpcTestEndpoint {

    @Context
	UriInfo uriInfo;

    @Context
	HttpServerRequest request;

    @Inject
    ExternalConsumerService grpcService;

    @GET
    @Path("/{message}")
    public String sendGrpcData(@PathParam("message") String message) {

        GennyToken userToken = null;

		// Because of the RolesAllowed, this token checking code should always work
		String token = null;
		try {
			token = request.getHeader("authorization").split("Bearer ")[1];
			if (token != null) {
				userToken = new GennyToken(token);
			} else {
                return "Bad token";
            }
		} catch (Exception e) {
            return "Bad token (From exception)";
		}

        grpcService.send(token, Item.newBuilder().setBody(message).setToken(token).build());



        return "Sending!";
    }
    
}
