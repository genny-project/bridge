package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channel.Producer;

public class BridgeHandler {

  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  protected static SockJSHandler eventBusHandler(final Vertx vertx) {
    final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    return sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);

  }

  protected static void bridgeHandler(final BridgeEvent bridgeEvent) {
    if (bridgeEvent.type() == BridgeEventType.PUBLISH
        || bridgeEvent.type() == BridgeEventType.SEND) {
      JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
      rawMessage = rawMessage.getJsonObject("data");
      if (rawMessage.getString("token") != null) { // do not allow empty tokens
        if (rawMessage.getString("msg_type").equals("DATA_MSG")) {
          log.info("WEBSOCKET DATA >> EVENT-BUS DATA:" + rawMessage.getString("data_type") + ":"
              + StringUtils.abbreviateMiddle(rawMessage.getString("token"), "...", 40));
          if (Producer.getToData().writeQueueFull()) {
            log.error("WEBSOCKET EVNT >> PRODUCER GETTODATA IS FULL: #####################################");
          } else {
            log.info("WEBSOCKET EVNT >> PRODUCER GETTODATA NOT FULL:");
            Producer.getToData().send(rawMessage);
          }
          
        } else if (rawMessage.getString("msg_type").equals("EVT_MSG")) {

          log.info("WEBSOCKET EVNT >> EVENT-BUS EVNT:" + rawMessage.getString("event_type") + ":"
              + rawMessage.getJsonObject("data").getString("code") + ":"
              + StringUtils.abbreviateMiddle(rawMessage.getString("token"), "...", 40));
          if (Producer.getToEvents().writeQueueFull()) {
            log.error("WEBSOCKET EVNT >> PRODUCER GETTOEVENTS IS FULL: ###################################");
          } else {
            log.info("WEBSOCKET EVNT >> PRODUCER GETTOEVENTS NOT FULL:");
            Producer.getToEvents().send(rawMessage);
          }
          
        }
      } else {
        System.out.println("EMPTY TOKEN");
      }
    }
    bridgeEvent.complete(true);
  }
}
