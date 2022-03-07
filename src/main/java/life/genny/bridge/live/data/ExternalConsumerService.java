package life.genny.bridge.live.data;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import life.genny.bridge.model.grpc.Empty;
import life.genny.bridge.model.grpc.Item;
import life.genny.bridge.model.grpc.Stream;
import org.jboss.logging.Logger;


/**
 * Implementation of {@link Stream} that handles GRPC communication
 * between bridge and the frontend. Needs refining to use infispan cache rather
 * than a local hashmap
 * 
 * @author Dan
 */
@GrpcService
public class ExternalConsumerService implements Stream {

    private static final Logger LOG = Logger.getLogger(ExternalConsumerService.class);

    @Inject ExternalConsumer handler;

    /**
     * Duration to wait before a timeout is fired
     */
    private Duration timeout = Duration.ofSeconds(15);

    /**
     * Stores a map between the token of a session (Please fix this to use JTI or something!) and
     * a broadcast processor
     */
    private static Map<String, BroadcastProcessor<Item>> processors = new HashMap<>();

    /**
     * Called when a connection times out. Get this to work with JTI or something
     * @param token
     */
    private static void onFail(String token) {
        LOG.warn("Session with token " + token + " timed out!");
        processors.remove(token);
    }

    /**
     * Is called by the Frontend to create a new connection.
     * Should create an {@link BroadcastProcessor} and {@link Multi} for the connection
     */
    @Override
    public Multi<Item> connect(Item request) {

        if (processors.containsKey(request.getToken())) {
            LOG.error("2 sessions with the same token tried to connect!");
            //Throw an exception to indicate that they're already connected?
            //Not sure how to do this
            return null;
        }

        BroadcastProcessor<Item> processor = BroadcastProcessor.create();
        Multi<Item> multi = processor
            //.onItem().invoke() // - Called when an item is being sent
            .ifNoItem().after(timeout).failWith(new TimeoutException()).invoke(() -> {onFail(request.getToken());});

        processors.put(request.getToken(), processor);

        LOG.info("New session with token " + request.getToken() + " just connected!");
            

        return multi;
    }

    /**
     * Is called by the Frontend when they want to send data
     * @param request - A multi of Items containing the data
     */
    @Override
    public Uni<Empty> sink(Multi<Item> request) {

        LOG.info("Got data!");

        //Proccess the data here somehow

        //Return an Empty Uni
        return Uni.createFrom().nothing();
    }

    /**
     * Call this to send data to the frontend based on a token
     * (Convert to JTI or something please!)
     * @param token - User to send to
     * @param data - Data to send. Can possibly be a Multi if we want to send a few things
     */
    public void send(String token, Item data) {

        if (processors.containsKey(token)) {
            processors.get(token).onNext(data);
        }

        //Throw an exception or something?        

    }

    


}