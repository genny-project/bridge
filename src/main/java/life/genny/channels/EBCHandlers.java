package life.genny.channels;

import java.lang.invoke.MethodHandles;

import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.qwandautils.KeycloakUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static void registerHandlers() {

		EBConsumers.getFromCmds().subscribe(arg -> {
			//log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();
			log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :");
			
			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				// ugly, but remove the outer array
				if (incomingCmd.startsWith("[")) {
					incomingCmd = incomingCmd.replaceFirst("\\[", "");
					incomingCmd = incomingCmd.substring(0, incomingCmd.length() - 1);
				}

				final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
				final DeliveryOptions options = new DeliveryOptions();
				if (json.getString("token") != null) {
					JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
					String sessionState = tokenJSON.getString("session_state");
					String email = tokenJSON.getString("email");
					EBProducers.getChannelSessionList().get(email + sessionState).write(json);
					Vertx.currentContext().owner().eventBus().publish(tokenJSON.getString("session_state"), json);
				}
				// EBProducers.getToClientOutbound().deliveryOptions(options);
				// EBProducers.getToClientOutbound().write(json);
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
					+ StringUtils.abbreviateMiddle(obj.get("token").toString(), "...", 40));
			
			if (!incomingData.contains("<body>Unauthorized</body>")) {
				// ugly, but remove the outer array
				if (incomingData.startsWith("[")) {
					incomingData = incomingData.replaceFirst("\\[", "");
					incomingData = incomingData.substring(0, incomingData.length() - 1);
				}

				final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
				final DeliveryOptions options = new DeliveryOptions();
				// if (json.getString("token")!=null) {
				for (MessageProducer<JsonObject> msgProducer : EBProducers.getChannelSessionList().values()) {
					String channel = msgProducer.address();
					msgProducer.write(json);
					Vertx.currentContext().owner().eventBus().publish(channel, json);
				}
				//Vertx.currentContext().owner().eventBus().publish("address.inbound", json);
				// }
			} else {
				log.error("Cmd with Unauthorised data recieved");
			}
		});
	}

}
