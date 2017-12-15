package life.genny.channels;

import org.json.JSONObject;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.qwandautils.KeycloakUtils;

public class EBCHandlers {

	private static final Logger logger = LoggerFactory.getLogger(EBCHandlers.class);

	public static void registerHandlers() {

		EBConsumers.getFromCmds().subscribe(arg -> {
			String incomingCmd = arg.body().toString();
			logger.info(incomingCmd);
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
				logger.error("Cmd with Unauthorised cmd recieved");
			}
		});

		EBConsumers.getFromData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			logger.info(incomingData);
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
					Vertx.currentContext().owner().eventBus().publish(channel, json);
				}
				// }
			} else {
				logger.error("Cmd with Unauthorised data recieved");
			}
		});
	}

}
