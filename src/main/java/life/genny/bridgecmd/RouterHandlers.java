package life.genny.bridgecmd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channels.EBCHandlers;
import life.genny.channels.EBProducers;
import life.genny.qwandautils.KeycloakUtils;
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
          System.out.println("received get url:" + url + " sending : " + kcUrl + " " + kcClientId);
          routingContext.response().putHeader("Content-Type", "application/json");
          routingContext.response().end(retInit.toString());
        } else {
          System.out.println(key + " NOT FOUND IN KEYCLOAK-JSON-MAP");
          routingContext.response().end();
        }
      } catch (final MalformedURLException e) {
        routingContext.response().end();
      } ;
    });
  }


  public static void apiInitHandler(final RoutingContext routingContext) {
 
    routingContext.request().bodyHandler(body -> {
      final String bodyString = body.toString();
      final JsonObject j = new JsonObject(bodyString);
      logger.info(
          "url init:" + j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1]);
      String tokenSt = j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];
      // Map<String, EventBus> channelSessionList = new HashMap<String, EventBus>();
      JSONObject tokenJSON = KeycloakUtils.getDecodedToken(tokenSt);
      System.out.println(tokenJSON.get("email"));
      String sessionState =tokenJSON.getString("session_state");
      String email = (String) tokenJSON.get("email");
      
      final MessageProducer<JsonObject> toSessionChannel =
          Vertx.currentContext().owner().eventBus().publisher(sessionState);
      EBProducers.getChannelSessionList().put(email, toSessionChannel);
      routingContext.response().end();
      
    });
  }

  public static void apiSession(final RoutingContext routingContext) {
    final String token =
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiWWQ0SzNtcW4yeHpGenZsRG12dUNJdjVwNjlfTFZyd0c2bFFkUEpSTkU0In0.eyJqdGkiOiJjMDk1Mzg0NC0zNDZjLTRiNTQtOGYwNy04ZjFmYzYyNDU1OWYiLCJleHAiOjE1MTMxNDcwMDEsIm5iZiI6MCwiaWF0IjoxNTEzMTQ2NzAxLCJpc3MiOiJodHRwOi8vMTAuMS4xMjAuNjA6ODE4MC9hdXRoL3JlYWxtcy9nZW5ueSIsImF1ZCI6Imdlbm55Iiwic3ViIjoiMjYxM2FiM2MtNzI3OS00N2MxLWIzZGYtMzQxYWZiMmMwZWNiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZ2VubnkiLCJub25jZSI6IjM1MjczN2E1LTc2YTUtNDcxMS1iMGM3LTg4NzJmMzdmYjA1YSIsImF1dGhfdGltZSI6MTUxMzE0NTMxNiwic2Vzc2lvbl9zdGF0ZSI6IjgwMTBmMmFkLWExMGUtNGNmMi1hNjA5LWQ2NWY0NTU2YzJiYyIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9odHRwOi8vMTAuMS4xMjAuNjA6ODE4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODI4MCIsImh0dHA6Ly9odHRwOi8vMTAuMS4xMjAuNjA6ODE4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6NTAwMCIsImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidW1hX2F1dGhvcml6YXRpb24iLCJ1c2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IkFseXNvbiBHZW5ueSIsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXIxIiwiZ2l2ZW5fbmFtZSI6IkFseXNvbiIsImZhbWlseV9uYW1lIjoiR2VubnkiLCJlbWFpbCI6Imdlbm55QGdlbm55LmxpdmUifQ.yBTTlZuRn7Fb-yEzelxAYvboye8txY0mUHLB7kJgJ3KhfSfGeoxXxoPoteQTqqfNdUv5YcaCMa7psMLKCYl4xdCPEjSJFAXykEdlAhMDRcPyHY5bRhhW0PC_sWX_FkZwnmQeq9WJkq6giLjFFMoYvYjH48XNtUud8lx86lDememHn2xBnAz7t1YhC16ZPDR7AYQFZ0IDwhwPpBi-ePpjttXIE97XlHsGWNdOtPUUobYhFHHZAAqtet4D-IEMIFaPi7NAxW4QdvnTWi9Kk4Hx4BqxKstHtENlkhZpGS2oEY7imYd-IOIe964nVFbVN1Pb5AGVVrc6XpXd7OITFrA33w";
    JSONObject decodedToken = KeycloakUtils.getDecodedToken(token);
    JsonObject session = new JsonObject().put("session", decodedToken.get("session_state"));
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    response.putHeader("content-type", "application/json");
    response.write(session.encodePrettily()).end();
  }

  public static void apiServiceHandler(final RoutingContext routingContext) {
    // final String token = routingContext.request().getParam("token");
    routingContext.request().bodyHandler(body -> {
      final JsonObject j = body.toJsonObject();
      // j.put("token", token);
      logger.info("API Service Handler:" + j);
      if (j.getString("msg_type").equals("EVT_MSG"))
        EBProducers.getToEvents().write(j);
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
