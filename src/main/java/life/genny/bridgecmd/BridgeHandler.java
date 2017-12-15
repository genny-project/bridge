package life.genny.bridgecmd;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channels.EBProducers;
import life.genny.qwandautils.KeycloakUtils;

public class BridgeHandler {

  private static final Logger logger = LoggerFactory.getLogger(ServiceVerticle.class);

  
  protected static SockJSHandler eventBusHandler(final Vertx vertx) {
//    final MessageProducer<JsonObject> toAddressOutbound =
//        vertx.eventBus().publisher("address.outbound");
//    EBProducers.setToClientOutbound(toAddressOutbound);
    final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    return sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);
    
  }

  protected static void bridgeHandler(final BridgeEvent bridgeEvent) {
    if (bridgeEvent.type() == BridgeEventType.PUBLISH
        || bridgeEvent.type() == BridgeEventType.SEND) {
      JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
      rawMessage = rawMessage.getJsonObject("data");
      if (rawMessage.getString("token") != null) { // do not allow empty tokens
        String token = rawMessage.getString("token");
 //       System.out.println("INCOMING TOKEN FROM WEB BROWSER VERTX =" + StringUtils.abbreviateMiddle(rawMessage.getString("token"),"...",40));
  //      JSONObject tokenJSON = KeycloakUtils.getDecodedToken(token);
//        System.out.println(tokenJSON.str);
        if (rawMessage.getString("msg_type").equals("DATA_MSG")) {
          EBProducers.getToData().write(rawMessage);
          logger.info("PUBLISHED to data..."+rawMessage.getString("data_type")+":"+StringUtils.abbreviateMiddle(rawMessage.getString("token"),"...",40));
        }
        else if (rawMessage.getString("msg_type").equals("EVT_MSG")) {
          EBProducers.getToEvents().write(rawMessage);
          logger.info("PUBLISHED to events "+rawMessage.getString("event_type")+":"+rawMessage.getString("code")+":"+StringUtils.abbreviateMiddle(rawMessage.getString("token"),"...",40));
        }
      } else {
        System.out.println("EMPTY TOKEN");
      }
    }
    bridgeEvent.complete(true);
  }
}
