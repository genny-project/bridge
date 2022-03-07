package life.genny.bridge.live.data;


import javax.inject.Inject;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import life.genny.bridge.model.grpc.Empty;
import life.genny.bridge.model.grpc.Item;
import life.genny.bridge.model.grpc.Stream;



@GrpcService
public class ExternalConsumerService implements Stream {
    @Inject ExternalConsumer handler;


    private static void onSubscription() {
        //Do something?
    }

    /**
     * Is called by the Frontend to create a new connection.
     * Should create an {@link BroadcastProcessor} and {@link Multi} for the connection
     */
    @Override
    public Multi<Item> connect(Empty request) {

        BroadcastProcessor<Item> processor = BroadcastProcessor.create();
        Multi<Item> multi = processor
            .onSubscription().invoke(ExternalConsumerService::onSubscription);
            //.onItem().invoke()

        return multi;
    }

    @Override
    public Uni<Empty> sink(Multi<Item> request) {
        return null;
    }
    


}