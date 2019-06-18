package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.channel.Consumer;
import life.genny.channel.Producer;
import life.genny.cluster.CurrentVtxCtx;
import life.genny.security.TokenIntrospection;

public class BridgeHandler {

  private static final String DATA = "data";
  private static final String CODE = "code";
  private static final String BODY = "body";
  private static final String EVENTS = "events";
  private static final String TOKEN = "token";
  private static final String MSG_TYPE = "msg_type";
  private static final String EVENT_TYPE = "event_type";
  private static final String DATA_TYPE = "data_type";
  private static final String DATA_MSG = "DATA_MSG";
  private static final String EVT_MSG = "EVT_MSG";
  private static final List<String> roles;


  static {

    roles = TokenIntrospection.setRoles("user");
  }

  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());

  protected static SockJSHandler eventBusHandler(final Vertx vertx) {

    final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

    SockJSHandler bridge =
        sockJSHandler.bridge(BridgeConfig.setBridgeOptions(),
            BridgeHandler::bridgeHandler);

    return bridge;
  }

  public static JsonObject msgTmp = null;

  protected static void bridgeHandler(final BridgeEvent bridgeEvent) {

    if (bridgeEvent.type() == BridgeEventType.PUBLISH
        || bridgeEvent.type() == BridgeEventType.SEND) {

      JsonObject rawMessage =
          bridgeEvent.getRawMessage().getJsonObject(BODY);
      rawMessage = rawMessage.getJsonObject(DATA);
      String token = rawMessage.getString(TOKEN);
      if ( token != null && TokenIntrospection.checkAuthForRoles(roles, token)) { // do not allow empty tokens

          log.info("Roles from this token are allow and authenticated " +TokenIntrospection.checkAuthForRoles(roles, token) );

          rawMessage.put("sourceAddress", Consumer.directIP);  // set the source (return) address for any command
          
        if (rawMessage.getString(MSG_TYPE).equals(DATA_MSG)) {

          log.info("WEBSOCKET DATA >> EVENT-BUS DATA:"
              + rawMessage.getString(DATA_TYPE) + ":"
              + StringUtils.abbreviateMiddle(
                  rawMessage.getString(TOKEN), "...", 40));

          if (Producer.getToData().writeQueueFull()) {

            log.error(
                "WEBSOCKET EVT >> producer data is full hence message cannot be sent");

            Producer.setToData(CurrentVtxCtx.getCurrentCtx()
                .getClusterVtx().eventBus().publisher(DATA));

            Producer.getToData().send(rawMessage).end();

          } else {
            Producer.getToData().send(rawMessage).end();
          }
        } else if (rawMessage.getString(MSG_TYPE).equals(EVT_MSG)) {

          log.info("WEBSOCKET EVT >> EVENT-BUS EVT:"
              + rawMessage.getString(EVENT_TYPE) + ":"
              + rawMessage.getJsonObject(DATA).getString(CODE) + ":"
              + StringUtils.abbreviateMiddle(
                  rawMessage.getString(TOKEN), "...", 40));

          if (Producer.getToEvents().writeQueueFull()) {

            log.error(
                "WEBSOCKET EVT >> producer events is full hence message cannot be sent");
            Producer.setToEvents(CurrentVtxCtx.getCurrentCtx()
                .getClusterVtx().eventBus().publisher(EVENTS));
            Producer.getToEvents().send(rawMessage).end();

          } else {

            Producer.getToEvents().send(rawMessage).end();

          }

        }
      } else {
        log.error("EMPTY TOKEN");
      }
    }
    bridgeEvent.complete(true);
  }
}

