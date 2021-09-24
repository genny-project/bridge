package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
//import life.genny.channel.Consumer;
//import life.genny.channel.Producer;
import life.genny.cluster.CurrentVtxCtx;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.TokenIntrospection;
import life.genny.qwandautils.GennySettings;

@ApplicationScoped
public class BridgeHandler {

	@Inject
	Producer producer;
	private static final String DATA = "data";
	private static final String CODE = "code";
	private static final String TARGET_CODE = "targetCode";
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

	public static Vertx avertx;
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	//protected static Router eventBusHandler(final Vertx vertx) {
	protected Router eventBusHandler(final Vertx vertx) {

    SockJSHandlerOptions sockOptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		final SockJSHandler sockJSHandler = SockJSHandler.create(vertx,sockOptions);

		avertx = vertx;

		//Router bridge = sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), BridgeHandler::bridgeHandler);
		Router bridge = sockJSHandler.bridge(BridgeConfig.setBridgeOptions(), this::bridgeHandler);

		return bridge;
	}

	public static JsonObject msgTmp = null;

	//protected static void bridgeHandler(final BridgeEvent bridgeEvent) {
	protected void bridgeHandler(final BridgeEvent bridgeEvent) {

		if (bridgeEvent.type() == BridgeEventType.PUBLISH || bridgeEvent.type() == BridgeEventType.SEND) {

			JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject(BODY);
			log.info("[DEBUG] Raw Message is:" + rawMessage.toString());

			try {
				rawMessage = rawMessage.getJsonObject(DATA);
			} catch (Exception e) {
				bridgeEvent.complete(true);
				return;
			}
			String token = rawMessage.getString(TOKEN);
			GennyToken userToken = new GennyToken(token);
			if (token !=null ) { // do not allow empty
								
				if (("10.123.123.123".equals(GennySettings.defaultLocalIP))  || (TokenIntrospection.checkAuthForRoles(avertx,roles, token))) {
				// tokens
				if (rawMessage.getString(MSG_TYPE).equals(DATA_MSG)) {

					log.info("WEBSOCKET DATA >> EVENT-BUS DATA:" + userToken.getString("session_state") + " :"
							+ rawMessage.getString(DATA_TYPE) + ":" + StringUtils.abbreviateMiddle(token, "...", 30)
							+ "  [" + userToken.getUserCode() + "]:  ");

					// clean
						String cleanedMessage = rawMessage.toString().replaceAll("[^\\x20-\\x7E]", "");
						producer.getToData().send(cleanedMessage);

				} else if (rawMessage.getString(MSG_TYPE).equals(EVT_MSG)) {
					log.info("WEB EVENT    >> EVENT-BUS EVT  :" + userToken.getString("session_state") + " : "
							+ rawMessage.getString(EVENT_TYPE) + ":"

							+ rawMessage.getJsonObject(DATA).getString(CODE) + " :[" + userToken.getUserCode() + "]"); // +
							rawMessage.put("sourceAddress", GennySettings.defaultLocalIP);
						producer.getToEvents().send(rawMessage.toString());


					// HACK , change incoming button event to data
					if ((rawMessage.getJsonObject(DATA).getString(CODE) != null)
						&& (rawMessage.getJsonObject(DATA).getString(CODE).equals("QUE_SUBMIT"))) {
						Answer dataAnswer = new Answer(userToken.getUserCode(), rawMessage.getJsonObject(DATA).getString(TARGET_CODE), "PRI_SUBMIT",
								"QUE_SUBMIT");
						dataAnswer.setChangeEvent(false);
						QDataAnswerMessage dataMsg = new QDataAnswerMessage(dataAnswer);
						dataMsg.setToken(token);
						rawMessage = new JsonObject(JsonUtils.toJson(dataMsg));
						producer.getToData().send(rawMessage.toString());
					} 
				} 
				} else {
					log.error("checkAuthForRoles returned false");
				}
			} else {
				log.error("EMPTY TOKEN");
			}
		}
		bridgeEvent.complete(true);
	}
}
