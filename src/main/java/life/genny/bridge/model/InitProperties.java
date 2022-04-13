package life.genny.bridge.model;

import java.util.Arrays;
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
    @JsonProperty(value="ENV_KEYCLOAK_REDIRECTURI")
    String keycloakRedirectUri;
    @JsonProperty(value="ENV_MEDIA_PROXY_URL")
    String mediaProxyUrl;
    @JsonProperty(value="api_url")
    String apiUrl;
    @JsonProperty
    String clientId;

    public InitProperties(String url) throws BridgeException{
        this();
        // url = Optional.ofNullable(System.getenv("SERVER_URL")).orElse(url);
        setMediaProxyUrl(url);
        setApiUrl(url);

		if (url.contains("internmatch") || url.contains("alyson")) {
			setClientId("alyson");
		} else if (url.contains("mentormatch") || url.contains("mentor-match")) {
			setClientId("mentormatch");
		} else if (url.contains("lojing")) {
			setClientId("lojing");
		} else if (url.contains("credmatch") || url.contains("cred-match")) {
			setClientId("credmatch");
		}
    }

    public InitProperties() throws BridgeException{
    	// TODO: fetch these values from Kafka dependent upon the project url
        setRealm("internmatch");
        setKeycloakRedirectUri(System.getenv("ENV_KEYCLOAK_REDIRECTURI"));
    }

    public void setRealm(String realm) throws BridgeException {
        this.realm = throwIfNull(realm,"realm");
    }

    public void setMediaProxyUrl(String url) {
        this.mediaProxyUrl = url + "/web/public";
    }

    public void setApiUrl(String url) {
        this.apiUrl = url;
    }

	public void setClientId(String clientId) {
		this.clientId = clientId;
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
