package life.genny.security.keycloak.service;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import life.genny.security.keycloak.exception.GennyKeycloakException;
import life.genny.security.keycloak.model.KeycloakTokenPayload;

@Singleton
public class RoleBasedPermission{

    static final Logger LOG = Logger.getLogger(RoleBasedPermission.class);

    @Inject TokenVerification verification;

    public Boolean rolesAllowed(KeycloakTokenPayload payload,String...roles){
        try {
            payload = verification.verify(payload.realm, payload.token);
        } catch (GennyKeycloakException e) {
            LOG.error("An error occurred when verifying the token",e);
        }
        return Arrays.asList(roles).stream()
            .filter(payload.roles::contains)
            .map(d -> true)
            .findAny()
            .orElse(false);
    }

}
