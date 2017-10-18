package life.genny.bridgecmd;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channels.EBProducers;

public class BridgeHandler {

  private static final Logger logger = LoggerFactory.getLogger(ServiceVerticle.class);

  protected static SockJSHandler eventBusHandler(final Vertx vertx) {
    final MessageProducer<JsonObject> toAddressOutbound =
        vertx.eventBus().publisher("address.outbound");
    EBProducers.setToClientOutbound(toAddressOutbound);
    final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    return sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);
  }

  protected static void bridgeHandler(final BridgeEvent bridgeEvent) {
    if (bridgeEvent.type() == BridgeEventType.PUBLISH
        || bridgeEvent.type() == BridgeEventType.SEND) {
      JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
      rawMessage = rawMessage.getJsonObject("data");
      System.out.println("INCOMING TOKEN=" + rawMessage.getString("token"));
      if (rawMessage.getString("token") != null) { // do not allow empty tokens
        logger.info("Incoming Frontend Event :" + rawMessage);
        if (rawMessage.getString("msg_type").equals("DATA_MSG")) {
          logger.info("PUBLISH to data...");
          EBProducers.getToData().write(rawMessage);
          logger.info("PUBLISHED to data...");
        }
        else if (rawMessage.getString("msg_type").equals("EVT_MSG")) {
          logger.info("PUBLISH to events...");
          EBProducers.getToEvents().write(rawMessage);
          logger.info("PUBLISHED to events ....");
        }
      } else {
        System.out.println("EMPTY TOKEN");
      }
    }
    bridgeEvent.complete(true);
  }
}
