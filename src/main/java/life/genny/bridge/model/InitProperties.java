package life.genny.bridge.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.bridge.exception.BridgeException;

/**
 * InitProperties --- The class contains all the fields neccessary to contruct the protocol external clients
 * will use. The information pass to external client will tell paths for saving media files, google map keys,
 * the keycloak server it needs to use and which is trusted in the backends etcs.
 *
 * @author    hello@gada.io
 *
 */
@RegisterForReflection
public class InitProperties {

    @JsonProperty
    String realm;
    @JsonProperty(value="ENV_GOOGLE_MAPS_APIKEY")
    String googleMapsApikey;
    @JsonProperty(value="ENV_KEYCLOAK_REDIRECTURI")
    String keycloakRedirectUri;
    @JsonProperty(value="ENV_MEDIA_PROXY_URL")
    String mediaProxyUrl;

    public InitProperties(String url) throws BridgeException{
        this();
        setMediaProxyUrl(url);

    }

    public InitProperties() throws BridgeException{
        setRealm(System.getenv("realm"));
        setGoogleMapsApikey(System.getenv("ENV_GOOGLE_MAPS_APIKEY"));
        setKeycloakRedirectUri(System.getenv("ENV_KEYCLOAK_REDIRECTURI"));
    }

    public void setRealm(String realm) throws BridgeException {
        this.realm = throwIfNull(realm,"realm");
    }

    public void setMediaProxyUrl(String url) {
        this.mediaProxyUrl = url + "/web/pubilc";
    }

    public void setGoogleMapsApikey(String googleMapsApikey) throws BridgeException {
        this.googleMapsApikey = throwIfNull(googleMapsApikey,"ENV_GOOGLE_MAPS_APIKEY");
    }


    public void setKeycloakRedirectUri(String keycloakRedirectUri) throws BridgeException {
        this.keycloakRedirectUri = throwIfNull(keycloakRedirectUri,"ENV_KEYCLOAK_REDIRECTURI");
    }

    /**
     * It will throw an BridgeException error it the required field is null or empty
     *
     * @param val A value of the global field 
     * @param fieldName Name the global field 
     *
     * @return A non empty or null value
     *
     * @throws BridgeException A error if the field is null or empty along with a NullPointerException
     */
    public String throwIfNull(String val,String fieldName) throws BridgeException{

        return Optional.ofNullable(val).orElseThrow(
                () -> new BridgeException("GEN_000", "The value {"+fieldName+"} is compulsary "
                                          + " for the InitProperties class in order to provide the necessary information"
                                          + " to the requested client. This happens when a call is "
                                          + "made to the /api/events/init but the initProperties "
                                          + "do not contain the value",new NullPointerException()));
    }
}
