package life.genny.bridge.live.data;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.security.keycloak.exception.GennyKeycloakException;
import life.genny.security.keycloak.model.KeycloakTokenPayload;
import life.genny.security.keycloak.service.TokenVerification;

/**
 * InternalConsumer --- The class where all messages from the backends such as lauchy, 
 * wildfly-rulesservice and other internal applications are received to dispatch to the 
 * requested client by reading the token and routing by sesssion id.
 *
 * @author    hello@gada.io
 *
 */
public class InternalConsumer {

	private static final Logger LOG = Logger.getLogger(InternalConsumer.class);

	@Inject TokenVerification verification;
	@Inject EventBus bus;
	@Inject BlackListInfo blackList;
	
	
	@ConfigProperty(name = "genny.log.show.outgoing.json", defaultValue = "false")
	Boolean showOutgoingJson;


	/**
	 * A request with a protocol which will add, delete all or delete just a record depending on 
	 * the protocol specified in the in the message. The protocol consist of the following:
	 *      - Just a dash/minus (-)
	 *      - A dash/minus appended with a {@link UUID} (-UUID.toString())
	 *      - A {@link UUID} (UUID.toString())
	 *
	 * @param protocol A string with the rules already mentioned
	 */
	@Incoming("blacklists")
	public void getBlackLists(String protocol){
		LOG.warn("New recorded info associated to invalid data this protocol {"+protocol+"} "
				+"will be handled in the blacklisted class" );
		blackList.onReceived(protocol);
	}

	@Incoming("webcmds")
	public void getFromWebCmds(String arg){
		LOG.info("Message received in webcmd "+(showOutgoingJson?arg:""));
		handleIncomingMessage(arg);
	}

	@Incoming("webdata")
	public void getFromWebData(String arg)  {
		LOG.info("Message received in webdata");
		handleIncomingMessage(arg);
	}

	/**
	 * It checks that no confidential information has been leaked. It will delete the key properties
	 * if it finds any
	 *
	 * @param json A JsonObject 
	 *
	 * @return A JsonObject without the confidential key properties 
	 */
	public static JsonObject removeKeys(final JsonObject json) {
		if(json.containsKey("token"))
			json.remove("token"); // do not show the token
		if(json.containsKey("recipientCodeArray"))
			json.remove("recipientCodeArray"); // do not show the token
		return json;
	}

	/**
	 * Handle the message and route by session id which is extracted from the token 
	 *
	 * @param arg A Json string which is parsed inside the body of the method
	 */
	public void handleIncomingMessage(String arg){
		String incoming = arg;
		if ("{}".equals(incoming)) {
			LOG.warn("The payload sent from the webcmd producer is empty");
			return;
		}
                try {
                        final JsonObject json = new JsonObject(incoming); 
                        KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(json.getString("token"));
                        try {
                                verification.verify(payload.realm, payload.token);
                                if (!incoming.contains("<body>Unauthorized</body>")) {
                                        LOG.info("Publishing message to session " + payload.sessionState);
                                        bus.publish(payload.sessionState, removeKeys(json));
                                }else{
                                        LOG.error("The host service of channel producer tried to accessed an endpoint and got"+
                                                "an unauthorised message potentially from api and the producer hosted in rulesservice");
                                }
                        } catch (GennyKeycloakException e) {
                                LOG.error("The token verification has failed somehow this token was able to penatrate other "+
                                        "security barriers please check this exception in more depth");
                                e.printStackTrace();
                                return ;
                        }

                }catch(io.vertx.core.json.DecodeException e){
                        LOG.error("Failed to parse this message {"+arg+"} to a json object ");
                }
	}
}
