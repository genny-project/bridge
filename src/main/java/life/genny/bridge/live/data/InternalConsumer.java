package life.genny.bridge.live.data;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.security.keycloak.exception.GennyKeycloakException;
import life.genny.security.keycloak.model.KeycloakTokenPayload;
import life.genny.security.keycloak.service.TokenVerification;

/**
 * InternalConsumer --- 
 *
 * @author    hello@gada.io
 *
 */
public class InternalConsumer {

	private static final Logger LOG = Logger.getLogger(InternalConsumer.class);

	@Inject TokenVerification verification;
	@Inject EventBus bus;
	@Inject BlackListInfo blackList;

	@Incoming("blacklists")
	public void getBlackLists(String uuid){
		LOG.warn("New recorded info associated to invalid data this uuid {"+uuid+"} will be blacklisted" );
		blackList.onReceived(uuid);
	}

	@Incoming("webcmds")
	public void getFromWebCmds(String arg){
		LOG.info("Message received in webcmd");
		handleIncomingMessage(arg);
	}

	@Incoming("webdata")
	public void getFromWebData(String arg)  {
		LOG.info("Message received in webdata");
		handleIncomingMessage(arg);
	}

	public static JsonObject removeKeys(final JsonObject json) {
		if(json.containsKey("token"))
			json.remove("token"); // do not show the token
		if(json.containsKey("recipientCodeArray"))
			json.remove("recipientCodeArray"); // do not show the token
		return json;
	}

	public void handleIncomingMessage(String arg){
		String incoming = arg;
		if ("{}".equals(incoming)) {
			LOG.warn("The payload sent from the webcmd producer is empty");
			return;
		}
		final JsonObject json = new JsonObject(incoming); 
		KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(json.getString("token"));
		try {
			verification.verify(payload.realm, payload.token);
			if (!incoming.contains("<body>Unauthorized</body>")) {
				LOG.info("Publishing message to session " + payload.sessionState);
				bus.publish(payload.sessionState, removeKeys(json));
			}else{
				LOG.error("The host service of channel producer tried to accessed an endpoint and got"+
						"an unathorized message potentially from api and the producer hosted in rulesservice");
			}
		} catch (GennyKeycloakException e) {
			LOG.error("The token verification has failed somehow this token was able to penatrate other "+
					"security barriers please check this exception in more depth");
			e.printStackTrace();
			return ;
		}
	}
}
