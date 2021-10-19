package life.genny.security.keycloak.model;

import java.math.BigInteger;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jose4j.base64url.Base64Url;

/**
 * KeycloakRealmKey --- An object of this type will have the necessary 
 * information to construct the public key and verify a token signature 
 * to confirm if it was signed by the right Authorization server.
 *
 * @author    hello@gada.io
 *
 */
public class KeycloakRealmKey {


    /* Key id */
    public String kid; 
    /* Algorithm such as SH256 */
    public String kty;
    /* Algorithm type such as HS256 */
    public String alg;
    public String sig;
    public BigInteger modulus;
    public BigInteger exponent;

    @JsonCreator
    public KeycloakRealmKey(
            String kid,
            String kty,
            String alg,
            String sig,
            @JsonProperty(value="n")
            String modulus,
            @JsonProperty(value="e")
            String exponent
            ){
        this.kid = kid;
        this.kty = kty;
        this.alg = alg;
        this.sig = sig;
        this.modulus = new BigInteger(1, Base64Url.decode(modulus));
        this.exponent = new BigInteger(1, Base64Url.decode(exponent));
    }

    @Override
    public String toString() {
        return "KeycloakRealmKey [alg=" + alg + ", exponent=" + exponent + ", kid=" + kid + ", kty=" + kty
                + ", modulus=" + modulus + ", sig=" + sig + "]";
    }
}

