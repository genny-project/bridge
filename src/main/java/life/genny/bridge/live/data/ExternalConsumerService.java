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



@GrpcService
public class ExternalConsumerService implements Stream {

    @Inject ExternalConsumer handler;

    private Duration timeout = Duration.ofSeconds(15);

    private static Map<String, BroadcastProcessor<Item>> processors = new HashMap<>();



    private static void onSubscription() {
        //Do something?
    }

    private static void onFail(String token) {
        System.out.println(token + " timed out!");
        processors.remove(token);
    }

    /**
     * Is called by the Frontend to create a new connection.
     * Should create an {@link BroadcastProcessor} and {@link Multi} for the connection
     */
    @Override
    public Multi<Item> connect(Item request) {

        if (processors.containsKey(request.getToken())) {
            //Throw an exception to indicate that they're already connected?
            //Not sure how to do this
        }

        BroadcastProcessor<Item> processor = BroadcastProcessor.create();
        Multi<Item> multi = processor
            .onSubscription().invoke(ExternalConsumerService::onSubscription)
            
            //.onItem().invoke() // - Called when an item is being sent
            .ifNoItem().after(timeout).failWith(new TimeoutException()).invoke(() -> {onFail(request.getToken());});

        processors.put(request.getToken(), processor);
            

        return multi;
    }

    /**
     * Is called by the Frontend when they want to send data
     */
    @Override
    public Uni<Empty> sink(Multi<Item> request) {

        //Proccess the data here somehow

        return Uni.createFrom().nothing();
    }

    
    public void send(String token, Item data) {

        if (processors.containsKey(token)) {
            processors.get(token).onNext(data);
        }

        //Throw an exception or something?        

    }

    


}