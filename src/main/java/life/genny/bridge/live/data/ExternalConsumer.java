package life.genny.bridge.live.data;

import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.bridge.blacklisting.BlackListedMessages;
import life.genny.commons.CommonOps;
import life.genny.security.keycloak.model.KeycloakTokenPayload;
import life.genny.security.keycloak.service.RoleBasedPermission;

/**
 * ExternalConsumer --- External clients can connect to the endpoint configured in 
 * {@link ExternalConsumerConfig} to upgrade websockets and maintain a real time 
 * communication. The only knowned external consumer is alyson but it can be adapted
 * to any other client. 
 *
 * @author    hello@gada.io
 *
 */
@Singleton
public class ExternalConsumer {

    private static final Logger LOG = Logger.getLogger(ExternalConsumer.class);

    @Inject RoleBasedPermission permissions;
    @Inject BlackListInfo blacklist;

    @Inject
    InternalProducer producer;

    /**
     * Extract the token from the headers. The websocket mesage will be a 
     * json object and the root key properties of the object will contain 
     * headers, body and types such as PING PUBLISH and others
     * from {@BridgeEvent}
     *
     * @param bridgeEvent BridgeEvent object pass as an argument. 
     * See more {@link ExternalConsumerConfig} method - init(@Observes Router router)
     *
     * @return A bearer token 
     */
    public String extractTokenFromMessageHeaders(BridgeEvent bridgeEvent){
        JsonObject headers = bridgeEvent.getRawMessage().getJsonObject("headers");
        return CommonOps.extractTokenFromHeaders(headers.getString("Authorization"));
    }

    /**
     * Checks if the token has been verified and contains the roles and permission
     * for this request
     *
     * @param bridgeEvent BridgeEvent object pass as an argument. 
     * See more {@link ExternalConsumerConfig} method - init(@Observes Router router)
     * @param roles An arrays of string with each string being a role such user, test, 
     * admin etc.
     */
    void handleIfRolesAllowed(final BridgeEvent bridgeEvent,String...roles) {
        KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(
                extractTokenFromMessageHeaders(bridgeEvent));
        if(blacklist.getBlackListedUUIDs().contains(UUID.fromString(payload.sid))){
            bridgeEvent.socket().close(-1,BlackListedMessages.BLACKLISTED_MSG);
            LOG.error("A blacklisted user "
                    + payload.sid+" tried to access the sockets from remote " 
                    + bridgeEvent.socket().remoteAddress() );
            return;
        }

        if(permissions.rolesAllowed(payload, roles))
        {
            bridgeHandler(bridgeEvent,payload);
        }
        else{
            LOG.error("A message was sent with a bad token or an unauthorized user or a token from "+
                    "a different authority this user has not access to this request " + payload.sid );
            bridgeEvent.complete(false);
        }
    }

    /**
     * Checks whether the messsage contains within body and data key property. In addition 
     * a limit of 100kb is set so if the message is greater than tha the socket will be closed
     *
     * @param bridgeEvent BridgeEvent object pass as an argument. 
     * See more {@link ExternalConsumerConfig} method - init(@Observes Router router)
     *
     * @return - True if contains data key field and json message is less than 100kb
     *         - False otherwise
     */
    Boolean validateMessage(BridgeEvent bridgeEvent){
        JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
        if(rawMessage.toBuffer().length() > 100000){
            LOG.error("message of size " +
                    rawMessage.toBuffer().length() +
                    " is larger than 100kb sent from " 
                    + bridgeEvent.socket().remoteAddress() 
                    + " coming from the domain " 
                    + bridgeEvent.socket().uri());
            bridgeEvent.socket().close(-1, "message message is larger than 100kb");
            return false;
        }
        try {
            return rawMessage.containsKey("data");
        } catch (Exception e) {
            LOG.error("message does not have data field inside body");
            bridgeEvent.complete(true);
            return false;
        }
    }

    /**
     * Only handle mesages when they are type SEND or PUBLISH.
     *
     * @param bridgeEvent BridgeEvent object pass as an argument. 
     * See more {@link ExternalConsumerConfig} method - init(@Observes Router router)
     */
    void handleConnectionTypes(final BridgeEvent bridgeEvent){
        switch(bridgeEvent.type()){
            case PUBLISH:
            case SEND: {
                         handleIfRolesAllowed(bridgeEvent,"user");
            }
            case SOCKET_CLOSED:
            case SOCKET_CREATED:
            default: {
                         bridgeEvent.complete(true);
                         return;
            }

        }
    }

    /**
     * Depending of the message type the corresponding internal producer channel 
     * is used to route that request on the backends such as rules, api, sheelemy
     * notes, messages etc.
     *
     * @param body The body extracted from the raw json object sent from BridgeEvent
     * @param userUUID User UUID
     */
    void routeDataByMessageType(JsonObject body, String userUUID){
        if (body.getString("msg_type").equals("DATA_MSG")) {
            LOG.info("Sending to message from user " + userUUID + " to data");
            producer.getToData().send(body.toString());
        } else if (body.getString("msg_type").equals("EVT_MSG")){
            LOG.info("Sending to message from user " + userUUID + " to event");
            producer.getToEvents().send(body.toString());
        } else if (
                (body.getJsonObject("data").getString("code") != null) 
                && 
                (body.getJsonObject("data").getString("code").equals("QUE_SUBMIT")))
        {
            LOG.error("A deadend message was sent with the code QUE_SUBMIT");

        }

    }

    /**
     * Handle message after token has been verified 
     *
     * @param bridgeEvent BridgeEvent object pass as an argument. 
     * See more {@link ExternalConsumerConfig} method - init(@Observes Router router)
     * @param payload KeycloakTokenPayload object return from an authorized access check
     */
    protected void bridgeHandler(final BridgeEvent bridgeEvent,KeycloakTokenPayload payload) {
        JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
        if(!validateMessage(bridgeEvent)){
            LOG.error("An invalid message has been received from this user " + 
                    payload.sid +
                    " this message will be ingored ");
            return;
        }
        routeDataByMessageType(rawMessage.getJsonObject("data"),payload.sid);
        bridgeEvent.complete(true);
    }

}
