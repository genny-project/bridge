package life.genny.bridge;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
//import life.genny.channel.Producer;
import life.genny.cluster.CurrentVtxCtx;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.AttributeCodeValueString;
import life.genny.qwanda.GennyItem;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QDataB2BMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.GitUtils;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.TokenIntrospection;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;

@ApplicationScoped
public class BridgeRouterHandlers {

	@Inject
	Producer producer;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

//	protected static final io.vertx.core.logging.Logger log = LoggerFactory.getLogger(BridgeRouterHandlers.class);

	public static final String GIT_VERSION_PROPERTIES = "GitVersion.properties";

	public static final String PROJECT_DEPENDENCIES = "project_dependencies";

	private static final List<String> roles;

	static {

		roles = TokenIntrospection.setRoles("user");
	}

	private static final List<String> b2broles;
	static {
		b2broles = TokenIntrospection.setRoles("dev","b2b");
	}

	private static final List<String> testroles;
	static {

		testroles = TokenIntrospection.setRoles("test");
	}

	static public Vertx avertx;

	public static CorsHandler cors() {
		return CorsHandler.create(
        "http://localhost:\\d\\d|"+
        "https://localhost:\\d\\d|"+
        "http://localhost:\\d\\d\\d\\d|"+
        "https://localhost:\\d\\d\\d\\d|"+
        "https://.*.genny.life|https://.*.gada.io|"+
				System.getenv("CORS_URLS")+
        GennySettings.projectUrl).allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.PUT)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type").allowedHeader("Authorization")
				.allowedHeader("Accept")
				.allowedHeader("X-Requested-With");
	}

	public static void apiGetInitHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(bodyy -> {
      log.info("Get init handler being called apiGetInitHandler");
      log.info("Genny realm is as follows:::" + GennySettings.GENNY_REALM);
			final String fullurl = routingContext.request().getParam("url");
			String format = routingContext.request().getParam("format");
			if (format == null) {
				format = "json";
			}
			URL aURL = null;
			try {
				aURL = new URL(fullurl);
				final String url = aURL.getHost();
				JsonObject retInit = null;
				String token = null;
				// Fetch Project BE
				JsonObject jsonObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, url.toUpperCase());
				BaseEntity projectBe = null;
				if ((jsonObj == null) || ("error".equals(jsonObj.getString("status")))) {
					log.error(url.toUpperCase() + " not found in cache");

				} else {
					String value = jsonObj.getString("value");
					projectBe = JsonUtils.fromJson(value.toString(), BaseEntity.class);
					JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM,
							"TOKEN" + url.toUpperCase());  // TODO Only read this from intenal
					token = tokenObj.getString("value");

					// log.info("Init Request for "+url+" identified as --> "+projectBe.getRealm());
				}

				if ((projectBe != null) && ("json".equalsIgnoreCase(format))) {
					retInit = new JsonObject(projectBe.getValue("ENV_KEYCLOAK_JSON", "NO JSON"));
					String realm = projectBe.getRealm();
					String serviceToken = projectBe.getValue("ENV_SERVICE_TOKEN", "DUMMY");
					retInit.put("vertx_url", fullurl+"/frontend"/*GennySettings.vertxUrl*/);
					retInit.put("api_url", fullurl/*GennySettings.qwandaServiceUrl*/);
					final String kcUrl = retInit.getString("auth-server-url");
					retInit.put("url", kcUrl);
					final String kcClientId = retInit.getString("resource");
					retInit.put("clientId", kcClientId);
					retInit.put("ENV_GENNY_HOST",
							fetchSetting(realm, "ENV_GENNY_HOST", serviceToken,  fullurl.substring(0,fullurl.lastIndexOf(':')) + ":" + GennySettings.apiPort)); // The
																															// web
																															// bfrontend
																															// already
                                                                                                                            
                    Optional<JsonObject> jsonObject = System.getenv().entrySet().stream()
                     .filter(d -> d.getKey().startsWith("mobile_version_"))
                     .map(d -> new JsonObject().put(d.getKey().substring(15), new JsonObject().put("version",d.getValue())))
                     .reduce((acc,pre) ->{
                       Map a = acc.getMap();
                       a.putAll(pre.getMap());
                       JsonObject.mapFrom(a);
                       return acc;

                     });

                    if(jsonObject.isPresent())
                        retInit.put("mobile_conf", jsonObject.get());
                                                                                                                        
					// knows this url on port
					// 8088 (It uses it's own
					// url with a port 8088)
					retInit.put("ENV_GENNY_INITURL", fullurl); // the web frontend knows this url. It passed it to us,
																// but the mobile may not know
					retInit.put("ENV_GENNY_BRIDGE_PORT",
							fetchSetting(realm, "ENV_GENNY_BRIDGE_PORT", serviceToken, GennySettings.apiPort));

					retInit.put("ENV_GENNY_BRIDGE_VERTEX", "/frontend");
					retInit.put("ENV_MEDIA_PROXY_URL", GennySettings.mediaProxyUrl);
					retInit.put("ENV_GENNY_BRIDGE_SERVICE", "/api/service");
					retInit.put("ENV_GENNY_BRIDGE_EVENTS", "/api/events");

					retInit.put("ENV_GOOGLE_MAPS_APIKEY",
							fetchSetting(realm, "ENV_GOOGLE_MAPS_APIKEY", serviceToken, "NO_GOOGLE_MAPS_APIKEY"));
					retInit.put("PRI_FAVICON",
							fetchSetting(realm, "PRI_FAVICON", serviceToken, "NO_FAVICON"));
					retInit.put("PRI_NAME",
							fetchSetting(realm, "PRI_NAME", serviceToken, realm));

					retInit.put("ENV_GOOGLE_MAPS_APIURL",
							fetchSetting(realm, "ENV_GOOGLE_MAPS_APIURL", serviceToken, "NO_GOOGLE_MAPS_APIURL"));
					retInit.put("ENV_UPPY_URL",
							fetchSetting(realm, "ENV_UPPY_URL", serviceToken, "http://uppy.genny.life"));
					retInit.put("ENV_KEYCLOAK_REDIRECTURI", kcUrl);
					retInit.put("ENV_APPCENTER_ANDROID_SECRET", fetchSetting(realm, "ENV_APPCENTER_ANDROID_SECRET",
							serviceToken, "NO_APPCENTER_ANDROID_SECRET"));
					retInit.put("ENV_APPCENTER_IOS_SECRET",
							fetchSetting(realm, "ENV_APPCENTER_IOS_SECRET", serviceToken, "NO_APPCENTER_IOS_SECRET"));
					retInit.put("ENV_ANDROID_CODEPUSH_KEY",
							fetchSetting(realm, "ENV_ANDROID_CODEPUSH_KEY", serviceToken, "NO_ANDROID_CODEPUSH_KEY"));
					retInit.put("ENV_LAYOUT_PUBLICURL", fetchSetting(realm, "ENV_LAYOUT_PUBLICURL", serviceToken,
							"http://layout-cache.genny.life"));
					retInit.put("ENV_GUEST_USERNAME", fetchSetting(realm, "ENV_GUEST_USERNAME", serviceToken, "guest"));
					retInit.put("ENV_GUEST_PASSWORD",
							fetchSetting(realm, "ENV_GUEST_PASSWORD", serviceToken, "asdf1234"));
					retInit.put("ENV_SIGNATURE_URL",
							fetchSetting(realm, "ENV_SIGNATURE_URL", serviceToken, "http://signature.genny.life"));
					retInit.put("ENV_USE_CUSTOM_AUTH_LAYOUTS",
							fetchSetting(realm, "ENV_USE_CUSTOM_AUTH_LAYOUTS", serviceToken, "FALSE"));

					// To handle a quirky layout directory setting that is in format <realm>-new we
					// hack this bit...
					String layout_query_dir = fetchSetting(realm, "ENV_LAYOUT_QUERY_DIRECTORY", serviceToken,
							"NO_LAYOUT_QUERY_DIRECTORY");
					String devrealm = GennySettings.mainrealm;
					if (devrealm == null) {
						devrealm = realm;
					}
					layout_query_dir = layout_query_dir.replaceAll("genny", devrealm.toLowerCase().trim());
					retInit.put("ENV_LAYOUT_QUERY_DIRECTORY", layout_query_dir);

					log.info("WEB API GET    >> SETUP REQ:" + url + " sending : " + kcUrl + " " + kcClientId);
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
				} else if ((projectBe != null) && ("env".equalsIgnoreCase(format))) {
					String env = "";
					retInit = new JsonObject();
					String realm = projectBe.getRealm();
					String serviceToken = projectBe.getValue("ENV_SERVICE_TOKEN", "DUMMY");
					env = "realm=" + realm + "\n";
					env += "vertx_url=" + fullurl+"/frontend" /*GennySettings.vertxUrl*/ + "\n";
					env += "api_url=" + fullurl/*GennySettings.qwandaServiceUrl*/ + "\n";
					final String kcUrl = retInit.getString("auth-server-url");
					env += "url=" + kcUrl + "\n";
					final String kcClientId = retInit.getString("resource");
					env += "clientId=" + kcClientId + "\n";

                    Optional<JsonObject> jsonObject = System.getenv().entrySet().stream()
                     .filter(d -> d.getKey().startsWith("mobile_version_"))
                     .map(d -> new JsonObject().put(d.getKey().substring(15), new JsonObject().put("version",d.getValue())))
                     .reduce((acc,pre) ->{
                       Map a = acc.getMap();
                       a.putAll(pre.getMap());
                       JsonObject.mapFrom(a);
                       return acc;

                     });

                    if(jsonObject.isPresent())
                        env += "mobile_conf=" + jsonObject.get()+"\n";

					env += "ENV_GENNY_INITURL=" + fullurl + "\n"; // the web frontend knows this url. It passed it to
																	// us, but the mobile may not know

					env +="ENV_MEDIA_PROXY_URL"+ GennySettings.mediaProxyUrl;
					env += "ENV_GENNY_HOST="
							+ fetchSetting(realm, "ENV_GENNY_HOST", serviceToken, fullurl.substring(0,fullurl.lastIndexOf(':')) + ":" + GennySettings.apiPort)
							+ "\n";
					env += "ENV_GENNY_BRIDGE_PORT="
							+ fetchSetting(realm, "ENV_GENNY_BRIDGE_PORT", serviceToken, GennySettings.apiPort) + "\n";
					env += "ENV_GENNY_BRIDGE_VERTEX=" + "/frontend" + "\n";
					env += "ENV_GENNY_BRIDGE_SERVICE=" + "/api/service" + "\n";
					env += "ENV_GENNY_BRIDGE_EVENTS=" + "/api/events" + "\n";
					env += "ENV_GOOGLE_MAPS_APIKEY="
							+ fetchSetting(realm, "ENV_GOOGLE_MAPS_APIKEY", serviceToken, "NO_GOOGLE_MAPS_APIKEY")
							+ "\n";
					env += "PRI_FAVICON="
							+ fetchSetting(realm, "PRI_FAVICON", serviceToken, "NO_FAVICON")
							+ "\n";
					env += "PRI_NAME="
							+ fetchSetting(realm, "PRI_NAME", serviceToken, realm)
							+ "\n";
					env += "ENV_GOOGLE_MAPS_APIURL="
							+ fetchSetting(realm, "ENV_GOOGLE_MAPS_APIURL", serviceToken, "NO_GOOGLE_MAPS_APIURL")
							+ "\n";
					env += "ENV_UPPY_URL=" + fetchSetting(realm, "ENV_UPPY_URL", serviceToken, "http://uppy.genny.life")
							+ "\n";
					env += "ENV_KEYCLOAK_REDIRECTURI=" + kcUrl + "\n";
					env += "ENV_APPCENTER_ANDROID_SECRET=" + fetchSetting(realm, "ENV_APPCENTER_ANDROID_SECRET",
							serviceToken, "NO_APPCENTER_ANDROID_SECRET") + "\n";
					env += "ENV_APPCENTER_IOS_SECRET="
							+ fetchSetting(realm, "ENV_APPCENTER_IOS_SECRET", serviceToken, "NO_APPCENTER_IOS_SECRET")
							+ "\n";
					env += "ENV_ANDROID_CODEPUSH_KEY="
							+ fetchSetting(realm, "ENV_ANDROID_CODEPUSH_KEY", serviceToken, "NO_ANDROID_CODEPUSH_KEY")
							+ "\n";
					env += "ENV_LAYOUT_PUBLICURL=" + fetchSetting(realm, "ENV_LAYOUT_PUBLICURL", serviceToken,
							"http://layout-cache.genny.life:2224") + "\n";
					env += "ENV_LAYOUT_QUERY_DIRECTORY=" + fetchSetting(realm, "ENV_LAYOUT_QUERY_DIRECTORY",
							serviceToken, "NO_LAYOUT_QUERY_DIRECTORY") + "\n";
					env += "ENV_GUEST_USERNAME=" + fetchSetting(realm, "ENV_GUEST_USERNAME", serviceToken, "guest")
							+ "\n";
					env += "ENV_GUEST_PASSWORD=" + fetchSetting(realm, "ENV_GUEST_PASSWORD", serviceToken, "asdf1234")
							+ "\n";
					env += "ENV_SIGNATURE_URL="
							+ fetchSetting(realm, "ENV_SIGNATURE_URL", serviceToken, "http://signature.genny.life")
							+ "\n";
					env += "ENV_USE_CUSTOM_AUTH_LAYOUTS="
							+ fetchSetting(realm, "ENV_USE_CUSTOM_AUTH_LAYOUTS", serviceToken, "FALSE") + "\n";

					log.info("WEB API GET ENV   >> SETUP REQ:" + url + " sending : " + kcUrl + " " + kcClientId);
					routingContext.response().putHeader("Content-Type", "text/plain");
					routingContext.response().end(env);
				} else {

					log.error("KEYCLOAK JSON NOT FOUND");

					// Treat Inbound api call as a WEB SITE!!

					retInit = new JsonObject();
					retInit.put("status", "error");
					retInit.put("description", "keycloak json not found for " + url.toUpperCase());
					retInit.put("realm", "www");
					retInit.put("vertx_url", GennySettings.vertxUrl);
					log.info("WEB API GETWWW >> SETUP REQ:" + url + " sending : WWW");
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
				}
			} catch (MalformedURLException e) {
				routingContext.response().end();
			}
			;
		});
	}

	private static String fetchSetting(final String realm, final String key, final String serviceToken,
			final String defaultValue) {
		String project_code = "PRJ_" + realm.toUpperCase();
		String retValue = null;

		// First look at the system env
		retValue = System.getenv(realm.toUpperCase() + "_" + key.toUpperCase());

		if (retValue == null) {
			retValue = System.getenv(key.toUpperCase()); // try a common one
		} else {
			log.warn(realm+":"+key+" IS BEING OVERRIDDEN BY SYSTEM_ENV!!!! using ENV="+realm.toUpperCase() + "_" + key.toUpperCase());
		}
		// else look at the project setting
		if (retValue == null) {
			BaseEntity project = VertxUtils.readFromDDT(realm, project_code, serviceToken);
			if (project == null) {
				log.warn("Error: no Project BE Cached for " + key + " , ensure PRJ_" + realm.toUpperCase()
						+ " exists in cache for " + project_code+" returning default Value of "+defaultValue);
				return defaultValue;
			}
			Optional<EntityAttribute> entityAttribute = project.findEntityAttribute(key.toUpperCase());
			if (entityAttribute.isPresent()) {

				retValue = entityAttribute.get().getValueString();
				if (retValue == null) {
					log.warn(realm + " Bridge has " + key + " which is returning null so returning " + defaultValue);
					return defaultValue;
				} else {
					return retValue;
				}

			} else {
				log.warn("Error: no Project Setting for " + key + " , ensure PRJ_" + realm.toUpperCase()
						+ " has entityAttribute value for " + key.toUpperCase() + " returning default:" + defaultValue);
				return defaultValue;
			}
		} else {
			return retValue;
		}

	}

	
    //public static void virtualEventBusHandler(final RoutingContext routingContext){
    public void virtualEventBusHandler(final RoutingContext routingContext){


			routingContext.request().bodyHandler(body -> {


				final String bodyString = body.toString();
				final JsonObject rawMessage = new JsonObject(bodyString);

				try{

					producer.getToDataWithReply().send(rawMessage, json ->{
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().end(json.toString());

					});
				}catch(WebApplicationException e){
					log.error("A error has ocurred in rules");
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().setStatusCode(500).end();
				}
			});
    }
    
    public void apiSync2Handler(final RoutingContext routingContext){

			routingContext.request().bodyHandler(body -> {
				log.info("API SYNC 2 !!! ");
				final String bodyString = new String(body.getBytes());
				JsonObject rawMessage = null ;
				try {
					log.error("Message to decode :::: " + bodyString);
					rawMessage = new JsonObject(bodyString);
				} catch (Exception e) {
				
					log.error(e.getMessage());
					JsonObject err = new JsonObject().put("status", "error");
					routingContext.request().response().headers().set("Content-Type", "application/json");
					routingContext.request().response().end(err.encode());
					return;
				}

				String token  = routingContext.request().getHeader("authorization").split("Bearer ")[1];//rawMessage.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

				if (token != null/* && TokenIntrospection.checkAuthForRoles(avertx,roles, token) */) { // do not allow empty
					// tokens
					rawMessage.put("token", token);


					DeliveryOptions doptions = new DeliveryOptions();
					doptions.setSendTimeout(120000);

					try{

						producer.getToDataWithReply().send(rawMessage, json ->{
							routingContext.response().putHeader("Content-Type", "application/json");
							routingContext.response().end(json.toString());
						});
					}catch(WebApplicationException e){
						log.error("A error has ocurred in rules " + e.getMessage());
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().setStatusCode(500).end();
					}
				} else {
					log.warn("TOKEN NOT ALLOWED " + token);
				}
				
			});
     
		}    
    
		//public static void apiSyncHandler(final RoutingContext routingContext){
		public void apiSyncHandler(final RoutingContext routingContext){

			routingContext.request().bodyHandler(body -> {
				final String bodyString = body.toString();
				final JsonObject rawMessage = new JsonObject(bodyString);
				String token  = routingContext.request().getHeader("authorization").split("Bearer ")[1];//rawMessage.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

				if (token != null/* && TokenIntrospection.checkAuthForRoles(avertx,roles, token) */) { // do not allow empty
					rawMessage.put("token", token);

					try{
						producer.getToDataWithReply().send(rawMessage, json ->{
							routingContext.response().putHeader("Content-Type", "application/json");
							routingContext.response().end(json.toString());

						});

					}catch(WebApplicationException e){
						log.error("A error has ocurred in rules " + e.getMessage());
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().setStatusCode(500).end();
					}
				} else {
					log.warn("TOKEN NOT ALLOWED " + token);
				}
			});
		}    

	public static void apiInitHandler(final RoutingContext routingContext) {

		routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			final JsonObject j = new JsonObject(bodyString);

			// + j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1]);
			String token = j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

			if (token != null && TokenIntrospection.checkAuthForRoles(avertx,roles, token) ) { // do not allow empty
																									// tokens

				GennyToken gennyToken = new GennyToken(token);

				String sessionState = gennyToken.getString("session_state");

				String realm = gennyToken.getRealm();
				String userCode = gennyToken.getUserCode();
				log.info("API POST >> SESSN_INIT  :" + sessionState + ":" + realm + ":" + ":" + userCode);
				if (gennyToken.hasRole("test")) {
					VertxUtils.writeCachedJson(realm, "TOKEN:" + userCode, token, token, 28800); // 8 hours expiry, TODO
																									// use token expiry
				}

				Set<String> sessionStates = VertxUtils.getSetString(realm, "SessionStates", userCode);
			//	if (!sessionStates.contains(sessionState)) {
					sessionStates.add(sessionState);
					VertxUtils.putSetString(realm, "SessionStates", userCode, sessionStates);
					final MessageProducer<JsonObject> toSessionChannel = Vertx.currentContext().owner().eventBus()
							.publisher(sessionState);
					VertxUtils.putMessageProducer(sessionState, toSessionChannel);
			//	}
			} else {
				log.warn("TOKEN NOT ALLOWED " + token);
			}
			routingContext.response().end();

		});
	}

	//public static void apiServiceHandler(final RoutingContext routingContext) {
	public void apiServiceHandler(final RoutingContext routingContext) {

    String token = routingContext.request().getParam("token");
    String channel = routingContext.request().getParam("channel");
    String singleSessionStr = routingContext.request().getParam("singleSession");
    final Boolean	singleSession = "TRUE".equalsIgnoreCase(singleSessionStr)?true:false;
    log.info("Service Call! "+channel);

    routingContext.request().bodyHandler(body -> {
      // log.info("Service Call bodyHandler! " + channel);
      String localToken = null;
      JsonObject j = null;

      log.info("Here in bodyhandler");
      try {
        j = body.toJsonObject();
        log.info("Printing body here :::"+ j);
        if (token == null) {
          MultiMap headerMap = routingContext.request().headers();
          localToken = headerMap.get("Authorization");
          if (localToken == null) {
            log.error("In api/service NULL TOKEN!");
          } else {
            localToken = localToken.substring(7); // To remove initial [Bearer ]
            log.error("We get a localtoken");
          }
        } else {
          log.error("Else we put token to localtoken");
          localToken = token;
        }

        if (localToken != null && TokenIntrospection.checkAuthForRoles(avertx, testroles, localToken)) { // do not
          // allow
          // empty
          // tokens
          log.error("localtoken is not null");
          GennyToken userToken = new GennyToken(localToken);

          final DeliveryOptions options = new DeliveryOptions();
          options.addHeader("Authorization", "Bearer " + localToken);

          if ("EVT_MSG".equals(j.getString("msg_type")) || "events".equals(channel) || "event".equals(channel)) {
            log.info("EVT API POST   >> EVENT-BUS EVENT:");
            log.error("we got into event message");
            j.put("token", localToken);
            producer.getToEvents().send(j.toString());

          } else if ( "webcmds".equals(channel)) {
            log.info("WEBCMD API POST   >> WEB CMDS :" + j);
            log.error("we got into web cmd");
            j.put("token", localToken);
            EBCHandlers.sendToClientSessions(userToken, j, false);
          } else if ("webdata".equals(channel)) {
            log.info("WEBDATA API POST   >> WEB DATA :" + j);
            log.error("we got into web data");
            j.put("token", localToken);
            j.put("token", localToken);
            EBCHandlers.sendToClientSessions(userToken, j, singleSession);

          } else if (j.getString("msg_type").equals("CMD_MSG") || "cmds".equals(channel)) {
            log.info("CMD API POST   >> EVENT-BUS CMD  :" + j);
            log.error("we got into cmd_msg");

            j.put("token", localToken);
            producer.getToCmds().send(j.toString());
          } else if (j.getString("msg_type").equals("MSG_MESSAGE") || "messages".equals(channel)) {
            log.info("MESSAGES API POST   >> EVENT-BUS MSG DATA :");
            log.error("we got into MSG_MESSAGE");
            j.put("token", localToken);
            producer.getToMessages().send(j.toString());
          } else if ("DATA_MSG".equals(j.getString("msg_type")) || "answer".equals(channel)) {
              log.info("ANSWER API POST   >> EVENT-BUS MSG ANSWER :");
              log.error("we got into MSG_ANSWER2");
              QDataAnswerMessage msg = JsonUtils.fromJson(j.toString(), QDataAnswerMessage.class);
              Answer ans = msg.getItems()[0]; // TODO assume at least one answer
				JsonObject json = new JsonObject(JsonUtils.toJson(ans));
				json.put("token", localToken);
//             j.put("token", localToken);
              log.info("Answer Message:<<"+json.toString()+">>");
              producer.getToAnswer().send(json.toString());

          } else if (j.getString("msg_type").equals("DATA_MSG") || "data".equals(channel)) {
            log.info("CMD API POST   >> EVENT-BUS DATA :");
            log.error("we got into DATA_MSG sending to data");
            j.put("token", localToken);
            if ("Rule".equals(j.getString("data_type"))) {
              log.info("INCOMING RULE !");
            }
            producer.getToData().send(j.toString());
          }
        } else {
          log.warn("TOKEN NOT ALLOWED");
        }
      } catch (Exception e) {
        log.error("Error:"+ e.toString() + ", Body is:" + body.toString());
      }
      finally {
        routingContext.response().end();
      }
    });

	}

	public void apiDevicesHandler(final RoutingContext routingContext) {


		routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			final JsonObject rawMessage = new JsonObject(bodyString);
			String token  = routingContext.request().getHeader("authorization").split("Bearer ")[1];//rawMessage.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

			if (token != null && TokenIntrospection.checkAuthForRoles(avertx,roles, token) ) { // do not allow empty
				// tokens
				rawMessage.put("token", token);
				GennyToken userToken  = null;
				try {
					userToken = new GennyToken(token);
				} catch (Exception e1) {
					JsonObject err = new JsonObject().put("status", "error");
					routingContext.request().response().headers().set("Content-Type", "application/json");
					routingContext.request().response().end(err.encode());
					return;
				}

				final DeliveryOptions options = new DeliveryOptions();
				options.addHeader("Authorization", "Bearer " + token);

				log.info("Device Code:"+rawMessage.getString("code"));
				log.info("Device Type:"+rawMessage.getString("type"));
				log.info("Device Version:"+rawMessage.getString("version"));

				List<Answer> answers = new ArrayList<Answer>();
				answers.add(new Answer(userToken.getUserCode(),userToken.getUserCode(),"PRI_DEVICE_CODE",rawMessage.getString("code")));
				answers.add(new Answer(userToken.getUserCode(),userToken.getUserCode(),"PRI_DEVICE_TYPE",rawMessage.getString("type")));
				answers.add(new Answer(userToken.getUserCode(),userToken.getUserCode(),"PRI_DEVICE_VERSION",rawMessage.getString("version")));

				QDataAnswerMessage dataMsg = new QDataAnswerMessage(answers);
				dataMsg.setToken(token);
				dataMsg.setAliasCode("STATELESS");



				try{

					producer.getToDataWithReply().send(JsonUtils.toJson(dataMsg), json ->{
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().end(json.toString());
					});
				}catch(WebApplicationException e){
					log.error("A error has ocurred in rules " + e.getMessage());
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().setStatusCode(500).end();
				}
			} else {
				log.warn("TOKEN NOT ALLOWED " + token);
				JsonObject err = new JsonObject().put("status", "error");
				routingContext.request().response().headers().set("Content-Type", "application/json");
				routingContext.request().response().end(err.encode());
			}
		});
	}
	
	public void apiHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			if (body.toJsonObject().getString("msg_type").equals("CMD_MSG")) {
				log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :");
				//Producer.getToClientOutbound().send(body.toJsonObject());
			}
			if (body.toJsonObject().getString("msg_type").equals("DATA_MSG"))
				log.info("EVENT-BUS DATA >> WEBSOCKET DATA:");
			//producer.getToData().send(body.toJsonObject());
			producer.getToData().send(body.toJsonObject().toString());
		});
		routingContext.response().end();
	}

	public static void apiGetVersionHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			Properties properties = new Properties();
			String versionString = "";
			try {
				properties.load(Thread.currentThread().getContextClassLoader().getResource(GIT_VERSION_PROPERTIES)
						.openStream());
				String projectDependencies = properties.getProperty(PROJECT_DEPENDENCIES);
				versionString = GitUtils.getGitVersionString(projectDependencies);
			} catch (IOException e) {
				log.error("Error reading GitVersion.properties", e);
			}
			routingContext.response().putHeader("Content-Type", "application/json");
			routingContext.response().end(versionString);
		});

	}

	public static void apiGetHealthHandler(final RoutingContext context) {
		String token = context.request().getParam("token");

		context.request().bodyHandler(rc -> {
			String localToken = null;
			JsonObject testMessage = new JsonObject();
			if (token == null) {
				MultiMap headerMap = context.request().headers();
				localToken = headerMap.get("Authorization");
				if (localToken == null) {
					log.error("NULL TOKEN!");
				} else {
					localToken = localToken.substring(7); // To remove initial [Bearer ]
				}
			} else {
				localToken = token;
			}

			final DeliveryOptions options = new DeliveryOptions();
			options.addHeader("Authorization", "Bearer " + localToken);

			testMessage.put("token", localToken);

			Vertx.vertx().eventBus().<JsonObject>send("health", testMessage, ar -> {
//	                     Producer.getToHealth().send("health", testMessage, ar ->{

				if (ar.succeeded()) {

					JsonObject result = ar.result().body();

					context.request().response().end(result.encode());

				} else {

					// Do some fail - this is not good to return exception from server :D better
					// some error code

					// rc.fail(ar.cause());
					JsonObject ret = new JsonObject().put("status", "error");
					context.request().response().end(ret.encode());

				}

			});

		});

	}

	public boolean hasRole(Map<String, Object> decodedTokenMap, final String role) {

		if (decodedTokenMap == null) {
			return false;
		}

		LinkedHashMap rolesMap = (LinkedHashMap) decodedTokenMap.get("realm_access");
		if (rolesMap != null) {

			try {

				Object rolesObj = rolesMap.get("roles");
				if (rolesObj != null) {
					ArrayList roles = (ArrayList) rolesObj;
					if (roles.contains(role)) {
						return true;
					}
				}
			} catch (Exception e) {
			}
		}

		return false;
	}
	
	public static void apiGetPullHandler(final RoutingContext context) {
		log.info("PONTOON API BEING CALLED:");

		final HttpServerRequest req = context.request();
		String key = req.getParam("key");
		log.info("PONTOON API BEING CALLED KEY=:"+key);
		String token = context.request().getParam("token");
		String realm = null;
		if (token == null) {
			MultiMap headerMap = context.request().headers();
			token = headerMap.get("Authorization");
			if (token == null) {
				log.error("NULL TOKEN!");
			} else {
				token = token.substring(7); // To remove initial [Bearer ]
			}

		}

		if (token != null  && TokenIntrospection.checkAuthForRoles(avertx,roles, token) ) { // do not allow empty
																								// tokens

			if ("DUMMY".equals(token)) {
				realm = "jenny"; // force
			} 
			
			GennyToken gToken = new GennyToken(token);
			realm = gToken.getRealm();

			// for testig and debugging, if a user has a role test then put the token into a
			// cache entry so that the test can access it
			//// JSONObject realm_access = tokenJSON.getJSONObject("realm_access");
			// JSONArray roles = realm_access.getJSONArray("roles");
			// List<Object> roleList = roles.toList();

			// if ((roleList.contains("test")) || (roleList.contains("dev"))) {

		  log.info("PONTOON API BEING CALLED and the realm is :::" + realm);
			try {
				// a JsonObject wraps a map and it exposes type-aware getters
				JsonObject cachedJsonObject = VertxUtils.readCachedJson(realm, "PONTOON_"+key.toUpperCase(), token);
				log.info("PONTOON:"+key.toUpperCase());
				String value = "ERROR";
				if (cachedJsonObject != null) {
					value = cachedJsonObject.getString("value");

					String value2 = value.replaceAll(Pattern.quote("\\\""),
							Matcher.quoteReplacement("\""));
					String value3 = value2.replaceAll(Pattern.quote("\\n"),
							Matcher.quoteReplacement("\n"));
					String value4 = value3.replaceAll(Pattern.quote("\\\n"),
							Matcher.quoteReplacement("\n"));
					String value5 = value4.replaceAll(Pattern.quote("\"{"),
							Matcher.quoteReplacement("{"));
					String value6 = value5.replaceAll(Pattern.quote("}\""), Matcher.quoteReplacement("}"));
					context.request().response().headers().set("Content-Type", "application/json");
					context.request().response().end(value6);
					log.info("Good Pontoon!");
				}  else {
					log.info("Bad Pontoon!");
					JsonObject err = new JsonObject().put("status", "error");
					context.request().response().headers().set("Content-Type", "application/json");
					context.request().response().end(err.encode());

				}
			} catch (Exception e) {
				JsonObject err = new JsonObject().put("status", "error");
				context.request().response().headers().set("Content-Type", "application/json");
				context.request().response().end(err.encode());

			}
		} else {
			log.warn("PONTOON TOKEN NOT GOOD!");
		}
		// }

	}

	public void apiB2BHandlerGet(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			String token  = routingContext.request().getHeader("authorization").split("Bearer ")[1];//rawMessage.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

			if (token != null && TokenIntrospection.checkAuthForRoles(avertx,b2broles, token) ) { // do not allow empty
				// tokens
				GennyToken userToken  = null;
				try {
					userToken = new GennyToken(token);
				} catch (Exception e1) {
					JsonObject err = new JsonObject().put("status", "error");
					routingContext.request().response().headers().set("Content-Type", "application/json");
					routingContext.request().response().end(err.encode());
					return;
				}

				final DeliveryOptions options = new DeliveryOptions();
				options.addHeader("Authorization", "Bearer " + token);


				List<GennyItem> gennyItems = new ArrayList<GennyItem>();

				// go through query parameters and add them to a GennyItem
				GennyItem gennyItem = new GennyItem();
				MultiMap paramMap = routingContext.request().params();
				for (Entry<String,String> qparam :  paramMap.entries()) {
					String key = qparam.getKey();
					String value = qparam.getValue();
					
					key = key.trim();
					if (StringUtils.isBlank(value)) {
						continue;
					}
					value = value.trim();
					// hack for common keynames
					if ("firstname".equalsIgnoreCase(key)) {
						key = "PRI_FIRSTNAME";
					} else 	if ("lastname".equalsIgnoreCase(key)) {
						key = "PRI_LASTNAME";
					} else 	if ("surname".equalsIgnoreCase(key)) {
						key = "PRI_LASTNAME";
					} else 	if ("email".equalsIgnoreCase(key)) {
						key = "PRI_EMAIL";
					} else 	if ("mobile".equalsIgnoreCase(key)) {
						key = "PRI_MOBILE";
					}
					AttributeCodeValueString attCodevs = new AttributeCodeValueString(key,value);
					gennyItem.addB2B(attCodevs);
				}
				
				AttributeCodeValueString attCodevs = new AttributeCodeValueString("PRI_USERNAME",userToken.getUsername());
				gennyItem.addB2B(attCodevs);
				attCodevs = new AttributeCodeValueString("PRI_USERCODE",userToken.getUserCode());
				gennyItem.addB2B(attCodevs);
			
				gennyItems.add(gennyItem);
				
				QDataB2BMessage dataMsg = new QDataB2BMessage(gennyItems.toArray(new GennyItem[0]));
				dataMsg.setToken(token);
				dataMsg.setAliasCode("B2B");


				try{

					producer.getToDataWithReply().send(JsonUtils.toJson(dataMsg), json ->{
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().end(json.toString());
					});
				}catch(WebApplicationException e){
					log.error("A error has ocurred in B2B " + e.getMessage());
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().setStatusCode(500).end();
				}
			} else {
				log.warn("TOKEN NOT ALLOWED " + token);
				JsonObject err = new JsonObject().put("status", "error");
				routingContext.request().response().headers().set("Content-Type", "application/json");
				routingContext.request().response().end(err.encode());
			}
		});	
		}
	public void apiB2BHandlerPost(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			String token  = routingContext.request().getHeader("authorization").split("Bearer ")[1];//rawMessage.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];

			if (token != null && TokenIntrospection.checkAuthForRoles(avertx,b2broles, token) ) { // do not allow empty
				// tokens
				GennyToken userToken  = null;
				try {
					userToken = new GennyToken(token);
				} catch (Exception e1) {
					JsonObject err = new JsonObject().put("status", "error");
					routingContext.request().response().headers().set("Content-Type", "application/json");
					routingContext.request().response().end(err.encode());
					return;
				}

				final DeliveryOptions options = new DeliveryOptions();
				options.addHeader("Authorization", "Bearer " + token);


				List<GennyItem> gennyItems = new ArrayList<GennyItem>();

				QDataB2BMessage dataMsg = JsonUtils.fromJson(bodyString, QDataB2BMessage.class);
				dataMsg.setToken(token);
				dataMsg.setAliasCode("B2B");


				try{

					producer.getToDataWithReply().send(JsonUtils.toJson(dataMsg), json ->{
						routingContext.response().putHeader("Content-Type", "application/json");
						routingContext.response().end(json.toString());
					});
				}catch(WebApplicationException e){
					log.error("A error has ocurred in B2B " + e.getMessage());
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().setStatusCode(500).end();
				}
			} else {
				log.warn("TOKEN NOT ALLOWED " + token);
				JsonObject err = new JsonObject().put("status", "error");
				routingContext.request().response().headers().set("Content-Type", "application/json");
				routingContext.request().response().end(err.encode());
			}
		});	
		}

	
	public static void apiSearchHandler(final RoutingContext context) {
		log.info("PBRIDGE SEARCH API BEING CALLED:");

		final HttpServerRequest req = context.request();
		String key = req.getParam("key");
		log.info("SEARCH API BEING CALLED KEY=:"+key);
		String token = context.request().getParam("token");
		String realm = null;
		if (token == null) {
			MultiMap headerMap = context.request().headers();
			token = headerMap.get("Authorization");
			if (token == null) {
				log.error("NULL TOKEN!");
			} else {
				token = token.substring(7); // To remove initial [Bearer ]
			}

		}

		if (token != null  && TokenIntrospection.checkAuthForRoles(avertx,roles, token) ) { // do not allow empty
																								// tokens

			if ("DUMMY".equals(token)) {
				realm = "jenny"; // force
			} 
			
			GennyToken gToken = new GennyToken(token);
			realm = gToken.getRealm();

			// for testig and debugging, if a user has a role test then put the token into a
			// cache entry so that the test can access it
			//// JSONObject realm_access = tokenJSON.getJSONObject("realm_access");
			// JSONArray roles = realm_access.getJSONArray("roles");
			// List<Object> roleList = roles.toList();

			// if ((roleList.contains("test")) || (roleList.contains("dev"))) {

			try {
				// a JsonObject wraps a map and it exposes type-aware getters
		        SearchEntity searchBE = new SearchEntity("JOBS","Web Jobs")
		         	     .addSort("PRI_NAME","Name",SearchEntity.Sort.ASC)
		         	     .addFilter("PRI_CODE",SearchEntity.StringFilter.LIKE,"BEG_%") 
		         	     .addFilter("PRI_NAME",SearchEntity.StringFilter.LIKE,"%") 
		       					.addColumn("PRI_NAME", "Name")
		       					.addColumn("PRI_INDUSTRY", "Industry")
		       					.addColumn("PRI_ASSOC_INDUSTRY", "Assoc Industry")
		       					.addColumn("PRI_ADDRESS_FULL","Address")
		         	     .setPageStart(0)
		         	     .setPageSize(GennySettings.defaultPageSize);


		         	     searchBE.setRealm(gToken.getRealm());
		         	     BaseEntityUtils beUtils = new BaseEntityUtils(gToken);
		         	    List<BaseEntity>results =  beUtils.getBaseEntitys(searchBE);
		         	    QDataBaseEntityMessage msg = new QDataBaseEntityMessage(results);
		         	    String resultsStr = JsonUtils.toJson(msg);
						context.request().response().headers().set("Content-Type", "application/json");
						context.request().response().end(resultsStr);

				
			} catch (Exception e) {
				JsonObject err = new JsonObject().put("status", "error");
				context.request().response().headers().set("Content-Type", "application/json");
				context.request().response().end(err.encode());

			}
		} else {
			log.warn("SEARCHTOKEN NOT GOOD!");
		}
		// }

	}
	
}
