package ru.bssg.lottabyte.core.usermanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorisedException extends RuntimeException {

    public UnauthorisedException(String message) {
        super(message);
    }

    public UnauthorisedException(String message, Throwable cause) {
        super(message, cause);
    }
}