package life.genny.bridge.live.data;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.inject.Inject;

import io.grpc.stub.StreamObserver;
import life.genny.bridge.model.grpc.Stream;
import life.genny.security.keycloak.model.KeycloakTokenPayload;
import life.genny.bridge.endpoints.Bridge;
import life.genny.bridge.model.grpc.Empty;
import life.genny.bridge.model.grpc.Item;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.Json;
import io.vertx.ext.bridge.BaseBridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;



@GrpcService
public class ExternalConsumerService implements Stream {
    @Inject ExternalConsumer handler;
    @Override
    public Multi<Item> source(Empty request) {
        // Just returns a stream emitting an item every 2ms and stopping after 10 items.
        return Multi.createFrom().ticks().every(Duration.ofMillis(20))
                .select().first(10)
                .map(l -> Item.newBuilder().setBody(Long.toString(l)).build());
    }

    @Override
    public Uni<Empty> sink(Multi<Item> request) {
        // Reads the incoming streams, consume all the items.
        // request.subscribe().with(item -> {
        //     System.out.println("Item body " + item.getBody() + "Token " + item.getToken());
        //     KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(item.getToken());
        //     System.out.println("JTI " + payload.jti + " " + payload.sid);
        //     // JsonObject object = (JsonObject) Json.decodeValue(item.getBody(), JsonObject.class);
        //     JsonObject object = new JsonObject(item.getBody());
        //     BridgeEvent event;
        //     source(Empty.getDefaultInstance());
        //     handler.routeDataByMessageType(object, payload.sid, payload.jti);

        //     // event.setRawMessage(object);
        // });
        
        return request
                .map(Item::getBody)
                .map(Long::parseLong)
                .collect().last()
                .map(l -> Empty.newBuilder().build());
    }

    @Override
    public Multi<Item> pipe(Multi<Item> request) {
    

        // Reads the incoming stream, compute a sum and return the cumulative results
        // in the outbound stream.
        request.subscribe().with(item -> {
            System.out.println("Item body " + item.getBody() + "Token " + item.getToken());
            KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(item.getToken());
            System.out.println("JTI " + payload.jti + " " + payload.sid);
            // JsonObject object = (JsonObject) Json.decodeValue(item.getBody(), JsonObject.class);
            JsonObject object = new JsonObject(item.getBody());
            handler.routeDataByMessageType(object, payload.sid, payload.jti);
            
            // event.setRawMessage(object);
        },
        failure -> System.out.println("FAILED SUBSCRIPTION - " + failure), () -> System.out.println("Completed subscription")
        );
        // handler.handleConnectionTypes(event);

        return request
                .map(Item::getBody)
                .map(Long::parseLong)
                .onItem().scan(() -> 0L, Long::sum)
                .onItem().transform(l -> Item.newBuilder().setBody(Long.toString(l)).build());
    }
    @Override
    public Uni<Item> poll(Empty empty){
        return Uni.createFrom().item(Item.newBuilder().setBody("New value").build());
    }
    
    public StreamObserver<Item> pipe(final StreamObserver<Item> observer) {
        return new StreamObserver<Item>() {
            @Override
            public void onNext(Item item) {
                List<Item> items = new ArrayList<>();
                for(Item prevItem : items.toArray(new Item[0])) {
                    observer.onNext(prevItem);
                }
                items.add(item);
            }
            @Override
            public void onError(Throwable t) {
                System.out.println("Error " + t);
            }
            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        };
    }


}