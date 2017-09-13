package life.genny.channels;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EBCHandlers {
	
	private static final Logger logger = LoggerFactory.getLogger(EBCHandlers.class);

	public static void registerHandlers(){
		
		EBConsumers.getFromCmds().subscribe(arg -> {
			String incomingCmd = arg.body().toString();
			logger.info(incomingCmd);
			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				// ugly, but remove the outer array
				if (incomingCmd.startsWith("[")) {
					incomingCmd = incomingCmd.replaceFirst("\\[", "");
					incomingCmd = incomingCmd.substring(0, incomingCmd.length()-1);
				}
				final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
				final DeliveryOptions options = new DeliveryOptions();
				EBProducers.getToClientOutbound().deliveryOptions(options);
				EBProducers.getToClientOutbound().write(json);
				
			} else {
				logger.error("Cmd with Unauthorised data recieved");
			}
		});
	}
	
}
