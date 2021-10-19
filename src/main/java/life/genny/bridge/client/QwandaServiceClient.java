
package life.genny.bridge.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.client.exception.ResponseException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * QwandaServiceClient --- Make call to wildfly-qwanda-service endpoints
 *
 * @author    hello@gada.io
 */
@Path("/qwanda")
@RegisterRestClient
@RegisterProvider(value = ResponseException.class,
                  priority = 50)
public interface QwandaServiceClient{

    @POST
    @Path("baseentitys")
    JsonObject sendPayload(Object name);
}
