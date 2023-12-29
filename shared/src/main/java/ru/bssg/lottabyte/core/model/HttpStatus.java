package ru.bssg.lottabyte.core.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum HttpStatus {
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NO_CONTENT(204),
    BAD_REQUEST(400),
    NOT_AUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    GONE(410),
    TOO_MANY_REQUESTS(429),
    INTERNAL_ERROR(500),
    GATEWAY_ERROR(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    private int code_;
    private static final Logger LOGGER = Logger.getLogger(HttpStatus.class.getName());

    private HttpStatus(int code) {
        this.code_ = code;
    }

    public int getCode() {
        return this.code_;
    }

    public static HttpStatus fromCode(int code) throws IllegalArgumentException {
        Optional<HttpStatus> httpStatus = Arrays.stream(values()).filter((HttpStatus) -> {
            return HttpStatus.code_ == code;
        }).findFirst();
        if (httpStatus.isPresent()) {
            return (HttpStatus) httpStatus.get();
        } else {
            LOGGER.log(Level.INFO, "Unknown HTTP status code :" + code);
            return INTERNAL_ERROR;
        }
    }
}
