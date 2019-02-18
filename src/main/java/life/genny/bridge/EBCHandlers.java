package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.channel.Consumer;
import life.genny.qwanda.message.QBulkPullMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;



public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	private static Boolean bulkPull = (System.getenv("BULKPULL") != null) ? "TRUE".equalsIgnoreCase(System.getenv("BULKPULL")) : false;

	public static void registerHandlers() {

		Consumer.getFromWebCmds().subscribe(arg -> {
			// log.info("EVENT-BUS CMD >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();

			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingCmd, true);
			}
		});

		Consumer.getFromWebData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			log.info("EVENT-BUS DATA >> WEBSOCKET DATA2:" + json.getString("data_type") + ": size->" +json.size());

			if (!incomingData.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingData, false);
			}
		});
	}

	/**
	 * @param incomingCmd
	 */
	public static void sendToClientSessions(String incomingCmd, boolean sessionOnly) {
		// ugly, but remove the outer array
		if (incomingCmd.startsWith("[")) {
			incomingCmd = incomingCmd.replaceFirst("\\[", "");
			incomingCmd = incomingCmd.substring(0, incomingCmd.length() - 1);
		}

		final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
		log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + json.getString("cmd_type") + ":" + json.getString("code"));

		if (json.getString("token") != null) {
			// check token
			JsonArray recipientJsonArray = null;
			JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
			String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
			String realm = tokenJSON.getString("azp");
			
			String userCode = "PER_" + uname.toUpperCase();
			
			if ((!json.containsKey("recipientCodeArray")) || (json.getJsonArray("recipientCodeArray").isEmpty())) {
				recipientJsonArray = new JsonArray();

				recipientJsonArray.add(userCode);
			} else {
				recipientJsonArray = json.getJsonArray("recipientCodeArray");
			}

			json.remove("token"); // do not show the token
			json.remove("recipientCodeArray"); // do not show the other recipients
			JsonObject cleanJson = null; //
			
			cleanJson =json; // removePrivates(json, tokenJSON, sessionOnly, userCode);
			if (cleanJson == null) {
				log.error("null json");
			}
			if (bulkPull) {
				QBulkPullMessage msg = BaseEntityUtils.createQBulkPullMessage(cleanJson);
				cleanJson = new JsonObject(JsonUtils.toJson(msg));
			} 

			if (sessionOnly) {
				String sessionState = tokenJSON.getString("session_state");
				MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
				if (msgProducer != null) {
						msgProducer.write(cleanJson).end();
				}
			} else {
				for (int i = 0; i < recipientJsonArray.size(); i++) {
					String recipientCode = recipientJsonArray.getString(i);
					// Get all the sessionStates for this user
					log.info("GET SET realm="+realm+" userCode="+recipientCode);

					Set<String> sessionStates = VertxUtils.getSetString(realm, "SessionStates", recipientCode);

					if (((sessionStates != null) && (!sessionStates.isEmpty()))) {

					//	sessionStates.add(tokenJSON.getString("session_state")); // commenting this one, since current
																					// user was getting added to the
																					// toast recipients
						log.info("User:" + recipientCode + " with " + sessionStates.size() + " sessions");
						for (String sessionState : sessionStates) {

							MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
							// final MessageProducer<JsonObject> msgProducer =
							// Vertx.currentContext().owner().eventBus().publisher(sessionState);
							if (msgProducer != null) {
								log.info("Sending to "+sessionState);

									msgProducer.write(cleanJson).end();

							}

						}
					} else {
						// no sessions for this user!
						// need to remove them from subscriptions ...
						log.error("Remove "+recipientCode+" from subscriptions , they have no sessions");
					}
				}
			}

		} else {
			log.error("Cmd with Unauthorised cmd recieved");
		}
	}



}
