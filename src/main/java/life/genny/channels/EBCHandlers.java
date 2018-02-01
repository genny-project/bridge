package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static void registerHandlers() {

		EBConsumers.getFromCmds().subscribe(arg -> {
			// log.info("EVENT-BUS CMD >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();

			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				// ugly, but remove the outer array
				if (incomingCmd.startsWith("[")) {
					incomingCmd = incomingCmd.replaceFirst("\\[", "");
					incomingCmd = incomingCmd.substring(0, incomingCmd.length() - 1);
				}

				final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
				log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + json.getString("cmd_type") + ":"
						+ json.getString("code"));

				if (json.containsKey("recipientCodeArray")) {

					JsonArray recipientJsonArray = json.getJsonArray("recipientCodeArray");
					for (int i = 0; i < recipientJsonArray.size(); i++) {
						String recipientCode = recipientJsonArray.getString(i);
						Set<MessageProducer<JsonObject>> msgProducerList = EBProducers.getUserSessionMap()
								.get(recipientCode);
						if (msgProducerList != null) {
							// Send out to all the sessions for this userCode
							for (MessageProducer<JsonObject> msgProducer : msgProducerList) {
								if (msgProducer != null) {
									msgProducer.write(json);
									String address = msgProducer.address();
									System.out.println("SENDING TO SESSION:"+address+ " for user "+recipientCode);;
									Vertx.currentContext().owner().eventBus().publish(address, json);
								}
							}
						}
					}

				} else {

					final DeliveryOptions options = new DeliveryOptions();
					if (json.getString("token") != null) {
						JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
						String sessionState = tokenJSON.getString("session_state");
						String email = ""; // tokenJSON.getString("email");

						MessageProducer<JsonObject> msgProducer = EBProducers.getChannelSessionList()
								.get(email + sessionState);
						if (msgProducer != null) {
							msgProducer.write(json);
							Vertx.currentContext().owner().eventBus().publish(sessionState, json);
						}
					}
				}
				//
			} else {
				log.error("Cmd with Unauthorised cmd recieved");
			}
		});

		EBConsumers.getFromData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			JSONParser parser = new JSONParser();
			org.json.simple.JSONObject obj = null;
			try {
				obj = (org.json.simple.JSONObject) parser.parse(incomingData);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info("EVENT-BUS DATA >> WEBSOCKET DATA2:" + obj.get("data_type").toString() + ":"
					+ obj.get("recipientCodeArray"));

			if (!incomingData.contains("<body>Unauthorized</body>")) {
				// ugly, but remove the outer array
				if (incomingData.startsWith("[")) {
					incomingData = incomingData.replaceFirst("\\[", "");
					incomingData = incomingData.substring(0, incomingData.length() - 1);
				}

				final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();

				// Loop through the recipientCode Array to send this data
				String[] userCodeArray = (String[]) obj.get("recipientCodeArray");

				if (userCodeArray == null) {
					userCodeArray = new String[1]; // create the array
					String token = json.getString("token");
					JSONObject tokenJSON = KeycloakUtils.getDecodedToken(token);
					String username = tokenJSON.getString("preferred_username");
					String code = "PER_" + QwandaUtils.getNormalisedUsername(username).toUpperCase();
					userCodeArray[0] = code;

				}

				for (String userCode : userCodeArray) {
					// Find all user sessions
					for (MessageProducer<JsonObject> msgProducer : EBProducers.getUserSessionMap().get(userCode)) {
						String channel = msgProducer.address();
						msgProducer.write(json);
						Vertx.currentContext().owner().eventBus().publish(channel, json);
					}
				}
			} else {
				log.error("Cmd with Unauthorised data recieved");
			}

		});
	}

}
