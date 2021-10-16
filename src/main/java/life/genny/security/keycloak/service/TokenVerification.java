package life.genny.security.keycloak.service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import life.genny.security.keycloak.client.KeycloakHttpClient;
import life.genny.security.keycloak.exception.GennyKeycloakException;
import life.genny.security.keycloak.model.CertsResponse;
import life.genny.security.keycloak.model.KeycloakRealmKey;
import life.genny.security.keycloak.model.KeycloakTokenPayload;

@ApplicationScoped
public class TokenVerification {

    @Inject JWTParser parser;
    @Inject @RestClient KeycloakHttpClient client;

    Map<String, CertsResponse> certStore = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(TokenVerification.class);

    public CertsResponse getCert(String realm){
        if(certStore.containsKey(realm)){
            return certStore.get(realm);
        }
        LOG.info("The public cert for realm " + realm + " has been retrieved");
        certStore.put(realm, client.fetchRealmCerts(realm));
        return certStore.get(realm);
    }

    public KeycloakTokenPayload verify(String realm, String token) throws GennyKeycloakException{

        CertsResponse certs = getCert(realm);
        Optional<KeycloakRealmKey> keyMaybe = certs.keys.stream().findFirst();

        if(keyMaybe.isEmpty())
            throw new GennyKeycloakException(
                    "Code is : I dont'have code please define me",
                    "Not Certificate public key found for realm " + realm, 
                    new NullPointerException());

        KeycloakRealmKey key = keyMaybe.get();
        PublicKey publicKey = null;
        JsonWebToken result = null;

        try {
            publicKey = KeyFactory.getInstance(key.kty)
                .generatePublic(new RSAPublicKeySpec(key.modulus, key.exponent));
            result = parser.verify(token, publicKey);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException exception) {
            throw new GennyKeycloakException(
                    "Code is : I dont'have code please define me", 
                    "Something wrong with the Certificte key fields"+
                    " maybe the wrong algorithm is used or wrong jwt specifications",
                    exception);
        } catch (ParseException exception) {
            throw new GennyKeycloakException(
                    "Code is : I dont'have code please define me",
                    "An error occurred when parsing the token",
                    exception);
        }
        KeycloakTokenPayload payload = new KeycloakTokenPayload(result);
        return payload;
    }
}
