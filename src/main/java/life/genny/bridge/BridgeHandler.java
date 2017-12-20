package life.genny.bridge;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channels.EBProducers;

public class BridgeHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	protected static SockJSHandler eventBusHandler(final Vertx vertx) {
		// final MessageProducer<JsonObject> toAddressOutbound =
		// vertx.eventBus().publisher("address.outbound");
		// EBProducers.setToClientOutbound(toAddressOutbound);
		final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
		return sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);

	}

	protected static void bridgeHandler(final BridgeEvent bridgeEvent) {
		if (bridgeEvent.type() == BridgeEventType.PUBLISH || bridgeEvent.type() == BridgeEventType.SEND) {
			JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
			rawMessage = rawMessage.getJsonObject("data");
			if (rawMessage.getString("token") != null) { // do not allow empty tokens
				if (rawMessage.getString("msg_type").equals("DATA_MSG")) {
					log.info("WEBSOCKET DATA >> EVENT-BUS DATA:" + rawMessage.getString("data_type") + ":"
							+ StringUtils.abbreviateMiddle(rawMessage.getString("token"), "...", 40));
					EBProducers.getToData().write(rawMessage);
				} else if (rawMessage.getString("msg_type").equals("EVT_MSG")) {
					log.info("WEBSOCKET EVNT >> EVENT-BUS EVNT:" + rawMessage.getString("event_type") + ":"
							+ rawMessage.getJsonObject("data").getString("code") + ":"
							+ StringUtils.abbreviateMiddle(rawMessage.getString("token"), "...", 40));
					EBProducers.getToEvents().write(rawMessage);
				}
			} else {
				System.out.println("EMPTY TOKEN");
			}
		}
		bridgeEvent.complete(true);
	}
}
