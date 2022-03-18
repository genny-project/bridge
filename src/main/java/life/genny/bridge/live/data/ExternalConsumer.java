package life.genny.bridge.live.data;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import java.util.UUID;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.qwandaq.data.BridgeSwitch;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.security.keycloak.model.KeycloakTokenPayload;
import life.genny.security.keycloak.service.RoleBasedPermission;
import life.genny.serviceq.Service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * ExternalConsumer --- External clients can connect to the endpoint configured in {@link
 * ExternalConsumerConfig} to upgrade websockets and maintain a real time communication. The only
 * knowned external consumer is alyson but it can be adapted to any other client.
 *
 * @author hello@gada.io
 */
@Singleton
public class ExternalConsumer {

	private static final Logger log = Logger.getLogger(ExternalConsumer.class);

	@Inject RoleBasedPermission permissions;
	@Inject BlackListInfo blacklist;
	@Inject Service service;

	@ConfigProperty(name = "bridge.id", defaultValue = "false")
	String bridgeId;

    void onStart(@Observes StartupEvent ev) {

		// log our service config
		service.showConfiguration();

		// init necessary connections
		service.initToken();
		service.initCache();
		service.initKafka();
		log.info("[*] Finished Startup!");
    }

	/**
	 * Extract the token from the headers. The websocket mesage will be a json object and the root key
	 * properties of the object will contain headers, body and types such as PING PUBLISH and others
	 * from {@BridgeEvent}
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @return A bearer token
	 */
	public String extractTokenFromMessageHeaders(BridgeEvent bridgeEvent) {
		JsonObject headers = bridgeEvent.getRawMessage().getJsonObject("headers");
		return HttpUtils.extractTokenFromHeaders(headers.getString("Authorization"));
	}

	/**
	 * Checks if the token has been verified and contains the roles and permission for this request
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @param roles An arrays of string with each string being a role such user, test, admin etc.
	 */
	void handleIfRolesAllowed(final BridgeEvent bridgeEvent, String... roles) {

		KeycloakTokenPayload payload =
			KeycloakTokenPayload.decodeToken(extractTokenFromMessageHeaders(bridgeEvent));

		if (blacklist.getBlackListedUUIDs().contains(UUID.fromString(payload.sub))) {

			bridgeEvent.socket().close(-1, "Here is the confs ::: KLJHsSF#22452345SD09Jjla");
			log.errorv("A blacklisted user {} tried to access the sockets from remote {}",
					payload.sub,
					bridgeEvent.socket().remoteAddress());
			return;
		}

		if (permissions.rolesAllowed(payload, roles)) {
			bridgeHandler(bridgeEvent, payload);
		} else {
			log.error("A message was sent with a bad token or an unauthorized user or a token from "
					+ "a different authority this user has not access to this request " + payload.sub);
			bridgeEvent.complete(false);
		}
	}

	/**
	 * Checks whether the messsage contains within body and data key property. In addition a limit of
	 * 100kb is set so if the message is greater than tha the socket will be closed
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @return - True if contains data key field and json message is less than 100kb - False otherwise
	 */
	Boolean validateMessage(BridgeEvent bridgeEvent) {

		JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");

		if (rawMessage.toBuffer().length() > 100000) {

			log.errorv("message of size {} is larger than 100kb sent from {} coming from the domain {}",
					rawMessage.toBuffer().length(),
					bridgeEvent.socket().remoteAddress(),
					bridgeEvent.socket().uri());

			bridgeEvent.socket().close(-1, "message message is larger than 100kb");
			return false;
		}

		try {

			return rawMessage.containsKey("data");

		} catch (Exception e) {
			log.error("message does not have data field inside body");
			bridgeEvent.complete(true);
			return false;
		}
	}

	/**
	 * Only handle mesages when they are type SEND or PUBLISH.
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 */
	void handleConnectionTypes(final BridgeEvent bridgeEvent) {

		switch (bridgeEvent.type()) {
			case PUBLISH:
			case SEND:
				{
					handleIfRolesAllowed(bridgeEvent, "user");
				}
			case SOCKET_CLOSED:
			case SOCKET_CREATED:
			default:
				{
					bridgeEvent.complete(true);
					return;
				}
		}
	}

	/**
	 * Depending of the message type the corresponding internal producer channel is used to route that
	 * request on the backends such as rules, api, sheelemy notes, messages etc.
	 *
	 * @param body The body extracted from the raw json object sent from BridgeEvent
	 * @param userUUID User UUID
	 */
	void routeDataByMessageType(GennyToken gennyToken, JsonObject body) {

		// forward data to data channel
		if (body.getString("msg_type").equals("DATA_MSG")) {

			log.info("Sending to message from user " + gennyToken.getUniqueId() + " to data " + bridgeId);
			// KafkaUtils.writeMsg("data", body.put(jti, bridgeId).toString());
			KafkaUtils.writeMsg("data", body.toString());

		// forward events to events channel
		} else if (body.getString("msg_type").equals("EVT_MSG")) {

			log.info("Sending to message from user " + gennyToken.getUniqueId() + " " + bridgeId + " to events");
			// KafkaUtils.writeMsg("events", body.put(jti, bridgeId).toString());
			KafkaUtils.writeMsg("events", body.toString());

		} else if ((body.getJsonObject("data").getString("code") != null)
				&& (body.getJsonObject("data").getString("code").equals("QUE_SUBMIT"))) {

			log.error("A deadend message was sent with the code QUE_SUBMIT");
		}
	}

	/**
	 * Handle message after token has been verified
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @param payload KeycloakTokenPayload object return from an authorized access check
	 */
	protected void bridgeHandler(final BridgeEvent bridgeEvent, KeycloakTokenPayload payload) {

		JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");

		if (!validateMessage(bridgeEvent)) {
			log.errorv("An invalid message has been received from this user {} this message will be ingored ", payload.sid);
			return;
		}

		// NOTE: We should probably remove KeycloakTokenPayload in favour of GennyToken
		GennyToken gennyToken = new GennyToken(payload.token);

		// put id into user cached bridge info
		BridgeSwitch.put(gennyToken, bridgeId);

		// add id to active bridges if not there already
		if (!BridgeSwitch.activeBridgeIds.contains(bridgeId)) {
			BridgeSwitch.activeBridgeIds.add(bridgeId);
		}

		routeDataByMessageType(gennyToken, rawMessage.getJsonObject("data"));
		bridgeEvent.complete(true);
	}
}
