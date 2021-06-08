package life.genny.bridge;
import java.lang.invoke.MethodHandles;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.logging.log4j.Logger;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

@Path("/v7/api/service/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MobileResource {

	protected static final Logger log = org.apache.logging.log4j.LogManager
		.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	@Inject
	Producer producer;

	@POST
	public Response sync(String body) {

		final String bodyString = new String(body);
		JsonObject rawMessage = null ;
		try {
			rawMessage = new JsonObject(bodyString);
		} catch (Exception e) {
			JsonObject err = new JsonObject().put("status", "error");
			return Response.ok(err).build();
		}

		String token  = request.getHeader("authorization").split("Bearer ")[1];

		ResponseBuilder response = Response.noContent();
		if (token != null) { // do not allow empty

			rawMessage.put("token", token);

			try{
				producer.getToDataWithReply().send(rawMessage, json ->{
					log.info("Message received from rules ::: " + json.toString());
					response.entity(json.toString());
				});
			}catch(WebApplicationException e){
				Response.serverError().build();
			}
		} else {
			//return Response.ok(new JsonObject().put("status", "no token")).build();
			return Response.ok(new JsonObject().put("status", "no token")).build();
		}
		return response.build();
	}

}
