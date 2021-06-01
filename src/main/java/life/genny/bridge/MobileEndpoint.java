package life.genny.bridge;

import javax.inject.Inject;
//import javax.json.JsonObject;
//import javax.json.JsonValue;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

//import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
//import org.jose4j.json.internal.json_simple.JSONValue;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.eventbus.DeliveryOptions;

//import org.eclipse.microprofile.jwt.JsonWebToken;



@Path("/v7/api/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterForReflection
public class MobileEndpoint {

	private static final Logger log = Logger.getLogger(MobileEndpoint.class);

//	@Inject
//	JsonWebToken accessToken;
	

	@OPTIONS
	public Response opt() {
		return Response.ok().build();
	}

//	@POST
//	@Path("/sync2")
//	//@Retry(maxRetries = 4)
//	public Response mobileSync(@Context UriInfo uriInfo, @Valid JsonObject rawMessage
//		) {
//		
//	//	rawMessage.put("token", JsonValue..parse(accessToken.getRawToken()));
//
//		log.info(rawMessage);
//		DeliveryOptions doptions = new DeliveryOptions();
//		doptions.setSendTimeout(120000);
//
////		try{
////
////			producer.getToDataWithReply().send(rawMessage, json ->{
////				routingContext.response().putHeader("Content-Type", "application/json");
////				routingContext.response().end(json.toString());
////			});
////		}catch(WebApplicationException e){
////			log.error("A error has ocurred in rules " + e.getMessage());
////			routingContext.response().putHeader("Content-Type", "application/json");
////			routingContext.response().setStatusCode(500).end();
////		}
//
//
//		//GennyToken userToken = new GennyToken(accessToken.getRawToken());
//		return Response.ok().build();
//	}

}
