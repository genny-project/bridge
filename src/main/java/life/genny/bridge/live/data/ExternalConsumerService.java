package life.genny.bridge.live.data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.reactive.messaging.providers.MediatorConfigurationSupport.GenericTypeAssignable;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.model.grpc.Empty;
import life.genny.bridge.model.grpc.Item;
import life.genny.bridge.model.grpc.Stream;
import life.genny.security.keycloak.model.KeycloakTokenPayload;

/**
 * Implementation of {@link Stream} that handles GRPC communication
 * between bridge and the frontend. Needs refining to use infispan cache rather
 * than a local hashmap
 * 
 * @author Dan
 */
@GrpcService
@Default
@Singleton
public class ExternalConsumerService implements Stream {

    private static final Logger LOG = Logger.getLogger(ExternalConsumerService.class);

    @Inject
    ExternalConsumer handler;

    /**
     * Duration to wait before a timeout is fired
     */
    private Duration timeout = Duration.ofSeconds(15);

    /**
     * Stores a map between the token of a session (Please fix this to use JTI or
     * something!) and
     * a broadcast processor
     */
    private static Map<String, BroadcastProcessor<Item>> processors = new HashMap<>();

    /**
     * Called when a connection times out. Get this to work with JTI or something
     * 
     * @param jti
     */
    private static void onFail(String jti) {
        LOG.warn("Session with jti " + jti + " timed out!");
        processors.remove(jti);
    }

    /**
     * Is called by the Frontend to create a new connection.
     * Should create an {@link BroadcastProcessor} and {@link Multi} for the
     * connection
     */
    @Override
    public Multi<Item> connect(Item request) {

        if (processors.containsKey(getPayload(request).jti)) {
            LOG.error("2 sessions with the same token tried to connect!");
            // Throw an exception to indicate that they're already connected?
            // Not sure how to do this
            return null;
        }

        BroadcastProcessor<Item> processor = BroadcastProcessor.create();
        Multi<Item> multi = processor
                // .onItem().invoke() // - Called when an item is being sent
                .ifNoItem().after(timeout).failWith(new TimeoutException()).invoke(() -> {
                    onFail(getPayload(request).jti);
                });

        processors.put(getPayload(request), processor);

        LOG.info("New session with jti " + getPayload(request).jti + " just connected!");

        return multi;
    }

    /**
     * Is called by the Frontend when they want to send data
     * 
     * @param request - A multi of Items containing the data
     */
    @Override
    public Uni<Empty> sink(Item request) {
        LOG.info("Got data from " + getPayload(request).jti + " \n " + request.getBody());

        routeMessage(request);

        // event.setRawMessage(object);

        // Return an Empty Uni
        return Uni.createFrom().nothing();
    }

    /**
     * Call this to send data to the frontend based on a token
     * (Convert to JTI or something please!)
     * 
     * @param token - User to send to
     * @param data  - Data to send. Can possibly be a Multi if we want to send a few
     *              things
     */
    public void send(String jti, Item data) {

        if (processors.containsKey(jti)) {
            processors.get(jti).onNext(data);
        }

        // Throw an exception or something?

    }

    /**
     * Broadcast data to all connected clients
     * 
     * @param data
     */
    public void broadcast(Item data) {

        for (Entry<String, BroadcastProcessor<Item>> entry : processors.entrySet()) {
            send(entry.getKey(), data);
        }

    }

    /**
     * Heartbeat to keep connections alive. Should be called by the frontend on a
     * timer
     */
    @Override
    public Uni<Empty> heartbeat(Empty request) {
        return Uni.createFrom().nothing();
    }

    public void routeMessage(Item request) {
        LOG.info("Item body " + request.getBody() + "Token " + request.getToken());
        KeycloakTokenPayload payload = getPayload(request);
        System.out.println("JTI " + payload.jti + " " + payload.sid);
        JsonObject object = new JsonObject(request.getBody());
        handler.routeDataByMessageType(object, payload.sid, payload.jti);
    }

    private KeycloakTokenPayload getPayload(Item request) {
        KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(request.getToken());
        return payload;
    }

}