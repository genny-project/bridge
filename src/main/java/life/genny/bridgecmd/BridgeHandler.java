package life.genny.bridgecmd;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channels.EBProducers;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
public class BridgeHandler {

	private static final Logger logger = LoggerFactory.getLogger(ServiceVerticle.class);

	protected static SockJSHandler eventBusHandler(Vertx vertx) {	
		MessageProducer<JsonObject> toAddressOutbound = vertx.eventBus().publisher("address.outbound");
		EBProducers.setToClientOutbound(toAddressOutbound);
		SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
		return sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);
	}

	protected static void bridgeHandler(BridgeEvent bridgeEvent) {
		if (bridgeEvent.type() == BridgeEventType.PUBLISH || bridgeEvent.type() == BridgeEventType.SEND) {
			JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
			String token = bridgeEvent.getRawMessage().getString("token");
			rawMessage = rawMessage.getJsonObject("data");
			logger.info("Incoming Frontend Event :" + rawMessage);
			logger.info("PUBLISH to events...");
			EBProducers.getToEvents().write(rawMessage);
			logger.info("PUBLISH to events ....");
		}
		bridgeEvent.complete(true);
	}
}
