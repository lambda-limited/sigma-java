
package lambda.sigma;

/**
 *
 * @author James Thorpe
 */
public class SigmaException extends RuntimeException {

    public SigmaException() {
    }

    public SigmaException(String message) {
        super(message);
    }

    public SigmaException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigmaException(Throwable cause) {
        super(cause);
    }

    public SigmaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
