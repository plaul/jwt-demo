package sem3.security.demo.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class Client4xxException extends RuntimeException{
    public Client4xxException(String message) {
        super(message);
    }
}
