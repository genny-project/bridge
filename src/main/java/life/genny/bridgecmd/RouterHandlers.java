package life.genny.bridgecmd;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channels.EBCHandlers;
import life.genny.channels.EBProducers;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.buffer.Buffer;
import life.genny.security.SecureResources;;

public class RouterHandlers {

	private static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");
	private static String hostIP = System.getenv("HOSTIP")!=null?System.getenv("HOSTIP"):"127.0.0.1";

	
	private Buffer data = null;
	private static final Logger logger = LoggerFactory.getLogger(EBCHandlers.class);

	public static CorsHandler cors() {
		return CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type");
	}
	
	public static void apiGetInitHandler(RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			String fullurl = routingContext.request().getParam("url");
			System.out.println("init json=" + fullurl);
			URL aURL = null;
			try {
				aURL = new URL(fullurl);
				String url = aURL.getHost();
				System.out.println("received get url:" + url);

				String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(url);
				if (keycloakJsonText != null) {
					JsonObject retInit = new JsonObject(keycloakJsonText);
					retInit.put("vertx_url", vertxUrl);
					String kcUrl = retInit.getString("auth-server-url");
					retInit.put("url", kcUrl);
					String kcClientId = retInit.getString("resource");
					retInit.put("clientId", kcClientId);
					routingContext.response().end(retInit.toString());
				} else {
					routingContext.response().end();
				}
			} catch (MalformedURLException e) {
				routingContext.response().end();
			}
			;
		});
	}

	public static void apiInitHandler(RoutingContext routingContext) {

		routingContext.request().bodyHandler(body -> {

			System.out.println("init json=" + body);
			String bodyString = body.toString();
			JsonObject j = new JsonObject(bodyString);
			logger.info("url init:" + j);
			String fullurl = j.getString("url");
			URL aURL = null;
			try {
				aURL = new URL(fullurl);
				String url = aURL.getHost();
				System.out.println("received post url:" + url);

				String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(url);
				if (keycloakJsonText != null) {

					JsonObject retInit = new JsonObject(keycloakJsonText);
					retInit.put("vertx_url", vertxUrl);
					String kcUrl = retInit.getString("auth-server-url");
					if (kcUrl.contains("localhost")) {
						kcUrl = kcUrl.replaceAll("localhost", hostIP);
					}
					retInit.put("url", kcUrl);
					String kcClientId = retInit.getString("resource");
					retInit.put("clientId", kcClientId);
					System.out.println("Sending back :" + retInit.toString());
					routingContext.response().end(retInit.toString());
				} else {
					routingContext.response().end();
				}
			} catch (MalformedURLException e) {
				routingContext.response().end();
			}
			;
		});
	}
	
	public static void apiServiceHandler(RoutingContext routingContext) {
		final String token = routingContext.request().getParam("token");
		routingContext.request().bodyHandler(body -> {
			JsonObject j = body.toJsonObject();
			j.put("token", token); 
			logger.info("KEYCLOAK:" + j);
			if (j.getString("msg_type").equals("EVT_MSG")) 
				EBProducers.getToEvents().write(j);
		});
		routingContext.response().end();
	}

	public static void main(String...strings){
		System.out.println(SecureResources.getKeycloakJsonMap()+"sdf");
		Future<Void> l = Future.future();
		Future<Void> o = Future.future();
//		l.future(p->{
//			
//			System.out.println("fdsfsd");
//			p.complete();
//			
//		}).compose(mapper->{
//			System.out.println(+"sdfd");
//			o.complete();
//		},o);
		SecureResources.setKeycloakJsonMap(Vertx.vertx()).compose(p->{
			System.out.println(SecureResources.getKeycloakJsonMap());
			 o.complete();
		},o);
	}
	
}
