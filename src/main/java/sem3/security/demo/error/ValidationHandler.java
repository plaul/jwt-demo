package sem3.security.demo.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ValidationHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status, WebRequest request) {

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("path",((ServletWebRequest)request).getRequest().getRequestURI().toString());
        errorBody.put("status","400");
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err-> err.getField()+": "+err.getDefaultMessage())
                .collect(Collectors.toList());

        errorBody.put("error", String.join(",",errors));
        return new ResponseEntity<Object>(errorBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Client4xxException.class)
    public ResponseEntity<Map<String, String>> handleException(HttpServletRequest request, Client4xxException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("path",request.getRequestURI().toString());
        errorResponse.put("error", e.getLocalizedMessage());
        errorResponse.put("status", ""+HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }




}
