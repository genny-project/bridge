package life.genny.commons;

/**
 * CommonOps --- common operations function used within this project
 *
 * @author    hello@gada.io
 *
 */
public class CommonOps {

    /**
     * Extract the token from a header and handle different scenarios such as:
     *      - The authorization parameter does not have spaces
     *      - The authorization parameter does not start with bearer
     *      - The authorization parameter starts with bearer
     *      - The authorization parameter only has bearer
     *      - The authorization parameter only has token
     *      - The authorization parameter starts with bearer and join by space with a token
     *
     *
     * @param authorization Value of the authorization header normally with this 
     * format: Bearer eydsMSklo30...
     *
     * @return token Token extracted or the same token if nothing found to extract
     */
    public static String extractTokenFromHeaders(String authorization){
        String[] splittedAuthValue = authorization.split(" ");
        if(splittedAuthValue.length < 2){
            if(
                    splittedAuthValue.length != 0 && 
                    !splittedAuthValue[0].equalsIgnoreCase("bearer") && 
                    splittedAuthValue[0].length() > 5
                    )
                return splittedAuthValue[0];
        }
        return splittedAuthValue[1];
    }
}
