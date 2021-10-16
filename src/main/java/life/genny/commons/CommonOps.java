package life.genny.commons;

public class CommonOps {

    public static String extractTokenFromHeaders(String authorization){
        String[] splittedAuthValue = authorization.split(" ");
        if(splittedAuthValue.length < 2){
            if(
                    splittedAuthValue.length != 0 && 
                    !splittedAuthValue[0].toLowerCase().equals("bearer") && 
                    splittedAuthValue[0].length() > 5
                    )
                return splittedAuthValue[0];
        }
        return splittedAuthValue[1];
    }
}
