package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channels.EBProducers;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.security.SecureResources;;

public class RouterHandlers {

	private static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");
	private static String hostIP = System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : "127.0.0.1";

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static CorsHandler cors() {
		return CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type")
				.allowedHeader("X-Requested-With");
	}

	public static void apiGetInitHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(bodyy -> {
			final String fullurl = routingContext.request().getParam("url");
			final String token = routingContext.request().getParam("token");
			final String tokenAuth = routingContext.request().getParam("Authorization");
			final String tokenAuthorize = routingContext.request().getParam("Authorize");
			// routingContext.request().params().names().stream().forEach(params ->
			// System.out.println(params));
			// System.out.println(routingContext.get);
			// final RoutingContext test = routingContext;
			// routingContext.request().headers().names().forEach(param
			// ->System.out.println(routingContext.request().headers().get(param)));
			// System.out.println(bodyy.toString()+"fdfdfdf"+token);
			// System.out.println("dklflsdfsdjflkjdsfjds "+body);
			// System.out.println("init json=" + fullurl);
			// JSONObject tokenJSON = KeycloakUtils.getDecodedToken(token);
			//
			// final MessageProducer<JsonObject> toSession =
			// Vertx.currentContext().owner().eventBus().publisher(tokenJSON.get("session_state").toString());
			// EBProducers.setToSession(toSession);
			URL aURL = null;
			try {
				aURL = new URL(fullurl);
				final String url = aURL.getHost();
				String key = url + ".json";
				final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(key);
				if (keycloakJsonText != null) {
					final JsonObject retInit = new JsonObject(keycloakJsonText);
					retInit.put("vertx_url", vertxUrl);
					final String kcUrl = retInit.getString("auth-server-url");
					retInit.put("url", kcUrl);
					final String kcClientId = retInit.getString("resource");
					retInit.put("clientId", kcClientId);
					log.info("WEB API GET    >> SETUP REQ:" + url + " sending : " + kcUrl + " " + kcClientId);
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
				} else {
					System.out.println(key + " NOT FOUND IN KEYCLOAK-JSON-MAP");
					routingContext.response().end();
				}
			} catch (final MalformedURLException e) {
				routingContext.response().end();
			}
			;
		});
	}

	public static void apiInitHandler(final RoutingContext routingContext) {

		routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			final JsonObject j = new JsonObject(bodyString);
			log.info("WEB API POST   >> SESSION_INIT:"
					+ j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1]);
			String tokenSt = j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];
			// Map<String, EventBus> channelSessionList = new HashMap<String, EventBus>();
			JSONObject tokenJSON = KeycloakUtils.getDecodedToken(tokenSt);
			System.out.println(tokenJSON.get("email"));
			String sessionState = tokenJSON.getString("session_state");
			String email = "";//(String) tokenJSON.get("email");
			log.info(sessionState + "   " + email);
			final MessageProducer<JsonObject> toSessionChannel = Vertx.currentContext().owner().eventBus()
					.publisher(sessionState);
			EBProducers.getChannelSessionList().put(email + sessionState, toSessionChannel);
			routingContext.response().end();

		});
	}

	public static void apiSession(final RoutingContext routingContext) {
		final String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiWWQ0SzNtcW4yeHpGenZsRG12dUNJdjVwNjlfTFZyd0c2bFFkUEpSTkU0In0.eyJqdGkiOiJjMDk1Mzg0NC0zNDZjLTRiNTQtOGYwNy04ZjFmYzYyNDU1OWYiLCJleHAiOjE1MTMxNDcwMDEsIm5iZiI6MCwiaWF0IjoxNTEzMTQ2NzAxLCJpc3MiOiJodHRwOi8vMTAuMS4xMjAuNjA6ODE4MC9hdXRoL3JlYWxtcy9nZW5ueSIsImF1ZCI6Imdlbm55Iiwic3ViIjoiMjYxM2FiM2MtNzI3OS00N2MxLWIzZGYtMzQxYWZiMmMwZWNiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZ2VubnkiLCJub25jZSI6IjM1MjczN2E1LTc2YTUtNDcxMS1iMGM3LTg4NzJmMzdmYjA1YSIsImF1dGhfdGltZSI6MTUxMzE0NTMxNiwic2Vzc2lvbl9zdGF0ZSI6IjgwMTBmMmFkLWExMGUtNGNmMi1hNjA5LWQ2NWY0NTU2YzJiYyIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9odHRwOi8vMTAuMS4xMjAuNjA6ODE4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODI4MCIsImh0dHA6Ly9odHRwOi8vMTAuMS4xMjAuNjA6ODE4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6NTAwMCIsImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidW1hX2F1dGhvcml6YXRpb24iLCJ1c2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IkFseXNvbiBHZW5ueSIsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXIxIiwiZ2l2ZW5fbmFtZSI6IkFseXNvbiIsImZhbWlseV9uYW1lIjoiR2VubnkiLCJlbWFpbCI6Imdlbm55QGdlbm55LmxpdmUifQ.yBTTlZuRn7Fb-yEzelxAYvboye8txY0mUHLB7kJgJ3KhfSfGeoxXxoPoteQTqqfNdUv5YcaCMa7psMLKCYl4xdCPEjSJFAXykEdlAhMDRcPyHY5bRhhW0PC_sWX_FkZwnmQeq9WJkq6giLjFFMoYvYjH48XNtUud8lx86lDememHn2xBnAz7t1YhC16ZPDR7AYQFZ0IDwhwPpBi-ePpjttXIE97XlHsGWNdOtPUUobYhFHHZAAqtet4D-IEMIFaPi7NAxW4QdvnTWi9Kk4Hx4BqxKstHtENlkhZpGS2oEY7imYd-IOIe964nVFbVN1Pb5AGVVrc6XpXd7OITFrA33w";
		JSONObject decodedToken = KeycloakUtils.getDecodedToken(token);
		JsonObject session = new JsonObject().put("session", decodedToken.get("session_state"));
		HttpServerResponse response = routingContext.response();
		response.setChunked(true);
		response.putHeader("content-type", "application/json");
		response.write(session.encodePrettily()).end();
	}

	public static void apiServiceHandler(final RoutingContext routingContext) {
		String token = routingContext.request().getParam("token");
		routingContext.request().bodyHandler(body -> {
			String localToken = null;
			final JsonObject j = body.toJsonObject();
			if (token == null) {
				MultiMap headerMap = routingContext.request().headers();
				localToken = headerMap.get("Authorization");
				if (localToken == null) {
					log.error("NULL TOKEN!");
				} else {
					localToken = localToken.substring(7); // To remove initial [Bearer ]
				}
			} else {
				localToken = token;
			}
			// j.put("token", token);
			if (j.getString("msg_type").equals("EVT_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS EVENT:" + j);
				j.put("token", localToken);
				final DeliveryOptions options = new DeliveryOptions();
				options.addHeader("Authorization", "Bearer " + localToken);
				EBProducers.getToEvents().deliveryOptions(options);
				EBProducers.getToEvents().write(j);
			} else

			if (j.getString("msg_type").equals("CMD_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS CMD  :" + j);
				j.put("token", localToken);
				EBProducers.getToCmds().write(j);
			} else if (j.getString("msg_type").equals("MSG_MESSAGE")) {
				log.info("CMD API POST   >> EVENT-BUS MSG DATA :" + j);
				j.put("token", localToken);
				EBProducers.getToMessages().write(j);
			} else if (j.getString("msg_type").equals("DATA_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS DATA :" + j);
				j.put("token", localToken);
				EBProducers.getToData().write(j);
			}

		});
		routingContext.response().end();
	}

	public static void apiHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			if (body.toJsonObject().getString("msg_type").equals("CMD_MSG"))
				log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + body.toJsonObject());
			EBProducers.getToClientOutbound().write(body.toJsonObject());
			if (body.toJsonObject().getString("msg_type").equals("DATA_MSG"))
				log.info("EVENT-BUS DATA >> WEBSOCKET DATA:" + body.toJsonObject());
			EBProducers.getToData().write(body.toJsonObject());
		});
		routingContext.response().end();
	}

}
