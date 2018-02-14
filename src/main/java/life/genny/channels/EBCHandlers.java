package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.VertxUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static void registerHandlers() {

		EBConsumers.getFromCmds().subscribe(arg -> {
			// log.info("EVENT-BUS CMD >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();

			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingCmd);
			}
		});

		EBConsumers.getFromData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			log.info("EVENT-BUS DATA >> WEBSOCKET DATA2:" + json.getString("data_type") + ":");

			if (!incomingData.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingData);
			}
		});	
	}

	/**
	 * @param incomingCmd
	 */
	private static void sendToClientSessions(String incomingCmd) {
		// ugly, but remove the outer array
		if (incomingCmd.startsWith("[")) {
			incomingCmd = incomingCmd.replaceFirst("\\[", "");
			incomingCmd = incomingCmd.substring(0, incomingCmd.length() - 1);
		}

		final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
		log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + json.getString("cmd_type") + ":"
				+ json.getString("code"));

		if (json.getString("token") != null) {
			// check token
			JsonArray recipientJsonArray = null;

			if (!json.containsKey("recipientCodeArray")) {
				recipientJsonArray = new JsonArray();
				JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
				String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
				String userCode = "PER_" + uname.toUpperCase();

				recipientJsonArray.add(userCode);
			} else {
				recipientJsonArray = json.getJsonArray("recipientCodeArray");
			}

			json.remove("token");
			// removePrivates(json);

			for (int i = 0; i < recipientJsonArray.size(); i++) {
				String recipientCode = recipientJsonArray.getString(i);
				// Get all the sessionStates for this user
				String sessionStates = VertxUtils.getObject("MSG", recipientCode, String.class);
				for (String sessionState : sessionStates.split(",")) {

					final MessageProducer<JsonObject> toSession = Vertx.currentContext().owner().eventBus()
							.publisher(sessionState);
					toSession.write(json);
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
					JsonArray items = json.getJsonArray("items");

					for (int i = 0; i < items.size(); i++) {
						JsonObject mJsonObject = (JsonObject) items.getJsonObject(i);
						// Now go through the attributes
						JsonArray attributes = mJsonObject.getJsonArray("baseEntityAttributes");
						JsonObject mAttribute = new JsonObject();
						JsonArray non_privates = new JsonArray();
						for (Integer j = 0; j < attributes.size(); j++) {
							mJsonObject = (JsonObject) attributes.getJsonObject(j);
							Boolean privacyFlag = mJsonObject.getBoolean("privacyFlag");
							if (privacyFlag != null) {
								if (!privacyFlag) {
									non_privates.add(mJsonObject);
								}
							}
						}
						mJsonObject.put("baseEntityAttributes", non_privates);
					}
				}
			}
		}
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
