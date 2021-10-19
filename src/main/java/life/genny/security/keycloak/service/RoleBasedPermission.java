package life.genny.security.keycloak.service;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import life.genny.security.keycloak.exception.GennyKeycloakException;
import life.genny.security.keycloak.model.KeycloakTokenPayload;

/**
 * RoleBasedPermission --- This class is composed with Authentication 
 * The methods below are for checking if the user has the right roles 
 * and permissions within the token payload.
 * @author    hello@gada.io
 *
 */
@Singleton
public class RoleBasedPermission{

    static final Logger LOG = Logger.getLogger(RoleBasedPermission.class);

    @Inject TokenVerification verification;

    /**
     * Similar to RolesAllowed annotation used in restful methods. It will 
     * check if the array of roles passed in the second and subsequent 
     * parameters are contained within the payload token. It will suffice 
     * to have only one role matched. 
     * 
     *
     * @param payload {@link KeycloakTokenPayload}
     * @param roles String array of roles 
     *
     * @return True if a role is matched within the roles of the payload
     *         False otherwise
     */
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
