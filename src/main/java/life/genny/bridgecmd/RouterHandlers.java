package life.genny.bridgecmd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channels.EBCHandlers;
import life.genny.channels.EBProducers;
import life.genny.security.SecureResources;
import life.genny.utils.PropertiesJsonDeserializer;;

public class RouterHandlers {

  private static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");
  private static String hostIP =
      System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : "127.0.0.1";

  private static final Logger logger = LoggerFactory.getLogger(EBCHandlers.class);
  


  public static CorsHandler cors() {
    return CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER")
        .allowedHeader("Content-Type").allowedHeader("X-Requested-With");
  }

  public static void apiGetInitHandler(final RoutingContext routingContext) {
    routingContext.request().bodyHandler(body -> {
      final String fullurl = routingContext.request().getParam("url");
      // System.out.println("init json=" + fullurl);
      URL aURL = null;
      try {
        aURL = new URL(fullurl);
        final String url = aURL.getHost();
         String key = url+".json";
        final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(key);
        if (keycloakJsonText != null) {
          final JsonObject retInit = new JsonObject(keycloakJsonText);
          retInit.put("vertx_url", vertxUrl);
          final String kcUrl = retInit.getString("auth-server-url");
          retInit.put("url", kcUrl);
          final String kcClientId = retInit.getString("resource");
          retInit.put("clientId", kcClientId);
          System.out.println("received get url:" + url+" sending : "+kcUrl+" "+kcClientId);

          routingContext.response().putHeader("Content-Type", "application/json");
          routingContext.response().end(retInit.toString());
        } else {
        	  System.out.println(key+ " NOT FOUND IN KEYCLOAK-JSON-MAP");
          routingContext.response().end();
        }
      } catch (final MalformedURLException e) {
        routingContext.response().end();
      } ;
    });
  }

  public static void apiInitHandler(final RoutingContext routingContext) {

    routingContext.request().bodyHandler(body -> {

      System.out.println("init json=" + body);
      final String bodyString = body.toString();
      final JsonObject j = new JsonObject(bodyString);
      logger.info("url init:" + j);
      final String fullurl = j.getString("url");
      URL aURL = null;
      try {
        aURL = new URL(fullurl);
        final String url = aURL.getHost();
        System.out.println("received post url:" + url);

        final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(url);
        if (keycloakJsonText != null) {

          final JsonObject retInit = new JsonObject(keycloakJsonText);
          retInit.put("vertx_url", vertxUrl);
          String kcUrl = retInit.getString("auth-server-url");
          if (kcUrl.contains("localhost")) {
            kcUrl = kcUrl.replaceAll("localhost", hostIP);
          }
          retInit.put("url", kcUrl);
          final String kcClientId = retInit.getString("resource");
          retInit.put("clientId", kcClientId);
          System.out.println("Sending back :" + retInit.toString());
          routingContext.response().putHeader("Content-Type", "application/json");

          routingContext.response().end(retInit.toString());
        } else {
          routingContext.response().end();
        }
      } catch (final MalformedURLException e) {
        routingContext.response().end();
      } ;
    });
  }

  public static void apiServiceHandler(final RoutingContext routingContext) {
//    final String token = routingContext.request().getParam("token");
    routingContext.request().bodyHandler(body -> {
      final JsonObject j = body.toJsonObject();
//      j.put("token", token);
      logger.info("API Service Handler:" + j);
      if (j.getString("msg_type").equals("EVT_MSG"))
        EBProducers.getToEvents().write(j);
      if (j.getString("msg_type").equals("CMD_MSG"))
          EBProducers.getToCmds().write(j);
      if (j.getString("msg_type").equals("MSG_MESSAGE")) {
    	  final String token = routingContext.request().getParam("token");
    	  j.put("token", token);
    	  EBProducers.getToMessages().write(j);
      }
          
    });
    routingContext.response().end();
  }

  public static void apiHandler(final RoutingContext routingContext) {
    routingContext.request().bodyHandler(body -> {
      logger.info("KEYCLOAK:" + body.toJsonObject());
      if (body.toJsonObject().getString("msg_type").equals("CMD_MSG"))
        EBProducers.getToClientOutbound().write(body.toJsonObject());
      if (body.toJsonObject().getString("msg_type").equals("DATA_MSG"))
        EBProducers.getToData().write(body.toJsonObject());
    });
    routingContext.response().end();
  }
  
 
}
