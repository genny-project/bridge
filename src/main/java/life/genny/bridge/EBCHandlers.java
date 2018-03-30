package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.channel.Consumer;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.VertxUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static void registerHandlers() {

		Consumer.getFromCmds().subscribe(arg -> {
			// log.info("EVENT-BUS CMD >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();

			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingCmd, true);
			}
		});

		Consumer.getFromData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			log.info("EVENT-BUS DATA >> WEBSOCKET DATA2:" + json.getString("data_type") + ":");

			if (!incomingData.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingData,false);
			}
		});
	}

	/**
	 * @param incomingCmd
	 */
	private static void sendToClientSessions(String incomingCmd, boolean sessionOnly) {
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

			if ((!json.containsKey("recipientCodeArray"))||(json.getJsonArray("recipientCodeArray").isEmpty())) {
				recipientJsonArray = new JsonArray();
				String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
				String userCode = "PER_" + uname.toUpperCase();

				recipientJsonArray.add(userCode);
			} else {
				recipientJsonArray = json.getJsonArray("recipientCodeArray");
			}

			json.remove("token");  // do not show the token
			json.remove("recipientCodeArray"); // do not show the other recipients
			JsonObject cleanJson = json; // TODOremovePrivates(json);

			for (int i = 0; i < recipientJsonArray.size(); i++) {
				String recipientCode = recipientJsonArray.getString(i);
				// Get all the sessionStates for this user

				Set<String> sessionStates = VertxUtils.getSetString("", "SessionStates", recipientCode);
				if ((sessionStates != null)&&(!sessionOnly)) {
					for (String sessionState : sessionStates) {

//						final MessageProducer<JsonObject> toSession = VertxUtils.getMessageProducer(sessionState);
//						toSession.write(json);
//					  System.out.println("12345678"+sessionState);
					  MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
					  msgProducer.write(cleanJson);
					}
				} else {
					String sessionState = tokenJSON.getString("session_state");
//					final MessageProducer<JsonObject> toSession = VertxUtils.getMessageProducer(sessionState);
//					toSession.write(json);
//					System.out.println("12345"+sessionState);
					MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
					msgProducer.write(cleanJson);
				}
			}

		} else {
			log.error("Cmd with Unauthorised cmd recieved");
		}
	}

	/**
	 * @param json
	 */
	private static JsonObject removePrivates(JsonObject json) {
		// TODO: Very ugly, but remove any Attributes with privateFlag
		
		if (json.containsKey("data_type")) {
			if ("BaseEntity".equals(json.getString("data_type"))) {
				if (json.containsKey("items")) {
					JsonArray newItems = new JsonArray();
					
					JsonArray items = json.getJsonArray("items");
					
					// For every BaseEntity
					for (int i = 0; i < items.size(); i++) {
						JsonObject mJsonObject = (JsonObject) items.getJsonObject(i);
						if (mJsonObject == null) continue;
						JsonObject newJson = new JsonObject();
						newJson.put("code", mJsonObject.getString("code"));
						newJson.put("index", mJsonObject.getInteger("index"));
						newJson.put("name", mJsonObject.getString("name"));
						newJson.put("links", mJsonObject.getJsonArray("links"));
						newJson.put("weight", mJsonObject.getDouble("weight"));
						newJson.put("id", mJsonObject.getLong("id"));
						newJson.put("created", mJsonObject.getString("created"));
						JsonArray non_privateAttributes = new JsonArray();

						// Now go through the attributes
						JsonArray attributes = mJsonObject.getJsonArray("baseEntityAttributes");
						if (attributes == null) {
							return json;
						}
						for (Integer j = 0; j < attributes.size(); j++) {
							mJsonObject = (JsonObject) attributes.getJsonObject(j);
							Boolean privacyFlag = mJsonObject.getBoolean("privacyFlag");
							if (privacyFlag != null) {
								if (!privacyFlag) {
									non_privateAttributes.add(mJsonObject);
								}
							} else {
								non_privateAttributes.add(mJsonObject);
							}
						}
						newJson.put("baseEntityAttributes", non_privateAttributes);
						newItems.add(newJson);
					}
					json.put("items", newItems);
					return json;
				} else
					return json;
			} else
				return json;
		} else 
		return json;
	}

	/**
	 * @param json
	 */
	private static String removePrivates2(JsonObject jsonVertx) {
		// TODO: Very ugly, but remove any Attributes with privateFlag
		com.google.gson.JsonObject json = JsonUtils.fromJson(jsonVertx.toString(), com.google.gson.JsonObject.class);
		if (json.has("data_type")) {
			if ("BaseEntity".equals(json.get("data_type").getAsString())) {
				if (json.has("items")) {
					com.google.gson.JsonArray items = json.getAsJsonArray("items");

					for (int i = 0; i < items.size(); i++) {
						com.google.gson.JsonObject mJsonObject = (com.google.gson.JsonObject) items.get(i)
								.getAsJsonObject();
						// Now go through the attributes
						com.google.gson.JsonArray attributes = mJsonObject.getAsJsonArray("baseEntityAttributes");
						com.google.gson.JsonArray non_privates = new com.google.gson.JsonArray();
						for (Integer j = 0; j < attributes.size(); j++) {
							mJsonObject = (com.google.gson.JsonObject) attributes.get(j).getAsJsonObject();
							Boolean privacyFlag = mJsonObject.get("privacyFlag").getAsBoolean();
							if (privacyFlag != null) {
								if (!privacyFlag) {
									non_privates.add(mJsonObject);
								}
							}
						}
						mJsonObject.remove("baseEntityAttributes");
						mJsonObject.add("baseEntityAttributes", non_privates);

					}
				}
			}
		}
		return json.toString();
	}
}
