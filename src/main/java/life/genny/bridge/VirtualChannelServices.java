package life.genny.bridge;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.vertx.core.json.JsonObject;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/eventbus")
@RegisterRestClient
public interface VirtualChannelServices {

    @POST
    JsonObject sendPayload(Object name);
}
