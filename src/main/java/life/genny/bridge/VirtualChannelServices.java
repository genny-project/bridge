package life.genny.bridge;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.vertx.core.json.JsonObject;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/eventbus")
@RegisterRestClient
@RegisterProvider(value = MyMapper.class,
                  priority = 50)
public interface VirtualChannelServices {

    @POST
    @Path("/datawithreply")
    JsonObject sendPayload(Object name);
}
