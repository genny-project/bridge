package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.core.shareddata.AsyncMap;
import io.vertx.rxjava.core.shareddata.SharedData;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channels.ClusterMap;
import life.genny.channels.EBProducers;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.security.SecureResources;
import life.genny.utils.VertxUtils;;

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

					// Treat Inbound api call as a WEB SITE!!

					final JsonObject retInit = new JsonObject();
					retInit.put("realm", "www");
					retInit.put("vertx_url", vertxUrl);
					log.info("WEB API GETWWW >> SETUP REQ:" + url + " sending : WWW");
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
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
			JSONObject tokenJSON = KeycloakUtils.getDecodedToken(tokenSt);
			String sessionState = tokenJSON.getString("session_state");
			String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
			String userCode = "PER_" + uname.toUpperCase();

			String sessionStates = VertxUtils.getObject("SessionStates", userCode, String.class);
			if (sessionStates == null) {
				sessionStates = "";
			}
			sessionStates += sessionState+",";
			VertxUtils.putObject("SessionStates", userCode, sessionStates);
		
			log.info("RECEIVING FROM SESSION:" + sessionState + " for user " + userCode);
	
			routingContext.response().end();

		});
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

	public static void apiMapPutHandler(final RoutingContext context) {
		ClusterMap.mapDTT(context);
	}

	public static void apiMapGetHandler(final RoutingContext context) {
		final HttpServerRequest req = context.request();
		String param1 = req.getParam("param1");

		SharedData sd = Vertx.currentContext().owner().sharedData();

		if (System.getenv("GENNY_DEV") == null) {

			sd.getClusterWideMap("shared_data", (AsyncResult<AsyncMap<String, String>> res) -> {
				if (res.failed()) {
					JsonObject err = new JsonObject().put("status", "error");
					req.response().headers().set("Content-Type", "application/json");
					req.response().end(err.encode());
				} else {
					AsyncMap<String, String> amap = res.result();
					amap.get(param1, (AsyncResult<String> comp) -> {
						if (comp.failed()) {
							JsonObject err = new JsonObject().put("status", "error").put("description", "write failed");
							req.response().headers().set("Content-Type", "application/json");
							req.response().end(err.encode());
						} else {
							JsonObject err = new JsonObject().put("status", "ok").put("value", comp.result());
							req.response().headers().set("Content-Type", "application/json");
							req.response().end(err.encode());
						}
					});
				}
			});
		} else {
			String result = (String) sd.getLocalMap("shared_data").get(param1);
			JsonObject err = new JsonObject().put("status", "ok").put("value", result);
			req.response().headers().set("Content-Type", "application/json");
			req.response().end(err.encode());
		}

	}

}
