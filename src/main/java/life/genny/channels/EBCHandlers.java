package life.genny.channels;

import java.lang.invoke.MethodHandles;
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

				if (json.getString("token") != null) {
					// check token
					JsonArray recipientJsonArray  = null;
					String payload = json.toString();
					
				if (!json.containsKey("recipientCodeArray")) {
					recipientJsonArray = new JsonArray();
					JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
					String username = tokenJSON.getString("preferred_username");
					String sessionState = tokenJSON.getString("session_state");
					String userCode = QwandaUtils.getUserCode(json.getString("token"));

					json.remove("token");

					MessageProducer<JsonObject> msgProducer = EBProducers.getChannelSessionList()
							.get(sessionState);
					if (msgProducer != null) {
						msgProducer.write(json);
						Vertx.currentContext().owner().eventBus().publish(sessionState, json);
					}
					recipientJsonArray.add(userCode);
				} else {
					recipientJsonArray = json.getJsonArray("recipientCodeArray");
				
					json.remove("token");
					
					removePrivates(json);

					
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
									System.out.println("SENDING TO SESSION:"+address+ " for user "+recipientCode+":"+msgProducer.hashCode());;
									Vertx.currentContext().owner().eventBus().publish(address, arg.body());
									
									
								}
							}
						}
					}
				}
//				} else {
//
//					final DeliveryOptions options = new DeliveryOptions();
//						JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
//						String sessionState = tokenJSON.getString("session_state");
//						String email = ""; // tokenJSON.getString("email");
//						json.remove("token");
//
//						MessageProducer<JsonObject> msgProducer = EBProducers.getChannelSessionList()
//								.get(email + sessionState);
//						if (msgProducer != null) {
//							msgProducer.write(json);
//							Vertx.currentContext().owner().eventBus().publish(sessionState, json);
//						}
//					
//				}
				//
			} else {
				log.error("Cmd with Unauthorised cmd recieved");
			}
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
				removePrivates(json);
				// Loop through the recipientCode Array to send this data
				String[] userCodeArray = (String[]) obj.get("recipientCodeArray");

				if (userCodeArray == null) {
					userCodeArray = new String[1]; // create the array
					String token = json.getString("token");
					String userCode = QwandaUtils.getUserCode(json.getString("token"));
					userCodeArray[0] = userCode;

				}

				String payload = json.toString();
				for (String userCode : userCodeArray) {
					// Find all user sessions
					if ( EBProducers.getUserSessionMap().get(userCode)!=null) {
					for (MessageProducer<JsonObject> msgProducer : EBProducers.getUserSessionMap().get(userCode)) {
						String channel = msgProducer.address();
						msgProducer.write(json);
						Vertx.currentContext().owner().eventBus().publish(channel, payload);
					}
					}
				}
			} else {
				log.error("Cmd with Unauthorised data recieved");
			}

		});
	}

	/**
	 * @param json
	 */
	private static void removePrivates(final JsonObject json) {
		// TODO: Very ugly, but remove any Attributes with privateFlag
		if (json.containsKey("data_type")) {
			if ("BaseEntity".equals(json.getString("data_type"))) {
		if (json.containsKey("items")) {
			JsonArray items = json.getJsonArray("items");

			for (int i = 0; i < items.size() ; i++)
			{
			    JsonObject mJsonObject = (JsonObject)items.getJsonObject(i);
			    // Now go through the attributes
			    JsonArray attributes = mJsonObject.getJsonArray("baseEntityAttributes");
			    JsonObject mAttribute = new JsonObject();
			    JsonArray non_privates = new JsonArray();
				for (Integer j = 0; j < attributes.size() ; j++)
				{
				    mJsonObject = (JsonObject)attributes.getJsonObject(j);
				    boolean privacyFlag = mAttribute.getBoolean("privacyFlag");
				    if (!privacyFlag) {
				    		non_privates.add(mJsonObject);
				    }
				}
				mJsonObject.put("baseEntityAttributes", non_privates);							
			}
		}
		}
		}
	}

}
