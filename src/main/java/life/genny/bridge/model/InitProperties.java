package life.genny.bridge.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.bridge.exception.BridgeException;

/**
 * InitProperties --- 
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

    public InitProperties() throws BridgeException{
        setRealm(System.getenv("realm"));
        setGoogleMapsApikey(System.getenv("ENV_GOOGLE_MAPS_APIKEY"));
        setKeycloakRedirectUri(System.getenv("ENV_KEYCLOAK_REDIRECTURI"));
    }

    public void setRealm(String realm) throws BridgeException {
        this.realm = throwIfNull(realm,"realm");
    }


    public void setGoogleMapsApikey(String googleMapsApikey) throws BridgeException {
        this.googleMapsApikey = throwIfNull(googleMapsApikey,"ENV_GOOGLE_MAPS_APIKEY");
    }


    public void setKeycloakRedirectUri(String keycloakRedirectUri) throws BridgeException {
        this.keycloakRedirectUri = throwIfNull(keycloakRedirectUri,"ENV_KEYCLOAK_REDIRECTURI");
    }

    public String throwIfNull(String val,String field) throws BridgeException{

        return Optional.ofNullable(val).orElseThrow(
                () -> new BridgeException("GEN_000", "The value {"+field+"} is compulsary "
                                          + " for the InitProperties class in order to provide the necessary information"
                                          + " to the requested client. This happens when a call is "
                                          + "made to the /api/events/init but the initProperties "
                                          + "do not contain the value",new NullPointerException()));
    }
}
