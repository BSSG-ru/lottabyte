package ru.bssg.lottabyte.coreapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ErrorCode;
import ru.bssg.lottabyte.core.model.ErrorContainer;
import ru.bssg.lottabyte.core.model.ErrorModel;
import ru.bssg.lottabyte.core.usermanagement.exception.UnauthorisedException;

import java.util.Collections;
import java.util.UUID;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(LottabyteException.class)
    public ResponseEntity<?> handleDiffControllerExceptions(LottabyteException lottabyteException) {
        ErrorModel errorModel = new ErrorModel();
        errorModel.setCode(ErrorCode.fromStatus(lottabyteException.getHttpStatusCode()));
        errorModel.setMessage(lottabyteException.getMessage());
        if (lottabyteException.getMsgId() != null)
            errorModel.setId(lottabyteException.getMsgId().toString());
        String traceId = UUID.randomUUID().toString();
        ErrorContainer errorContainer = new ErrorContainer(
                traceId, Collections.singletonList(errorModel));
        log.error("Error trace ID: " + traceId);
        log.error(lottabyteException.getMessage());
        log.error(ExceptionUtils.getStackTrace(lottabyteException));
        return new ResponseEntity<>(errorContainer.toString(), HttpStatus.resolve(lottabyteException.getHttpStatusCode().getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleBaseControllerExceptions(Exception exception) {
        ErrorModel errorModel = new ErrorModel();
        errorModel.setCode(ErrorCode.fromStatus(ru.bssg.lottabyte.core.model.HttpStatus.INTERNAL_ERROR));
        errorModel.setMessage(exception.getMessage() == null || exception.getMessage().isEmpty() ?
                exception.toString() : exception.getMessage());
        String traceId = UUID.randomUUID().toString();
        ErrorContainer errorContainer = new ErrorContainer(
                traceId, Collections.singletonList(errorModel));
        log.error("Error trace ID: " + traceId);
        log.error(exception.getMessage());
        log.error(ExceptionUtils.getStackTrace(exception));
        return new ResponseEntity<>(errorContainer.toString(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnauthorisedException.class)
    public ResponseEntity<?> handleBaseControllerExceptions(UnauthorisedException exception) {
        ErrorModel errorModel = new ErrorModel();
        errorModel.setCode(ErrorCode.fromStatus(ru.bssg.lottabyte.core.model.HttpStatus.NOT_AUTHORIZED));
        errorModel.setMessage(exception.getMessage());
        String traceId = UUID.randomUUID().toString();
        ErrorContainer errorContainer = new ErrorContainer(
                traceId, Collections.singletonList(errorModel));
        log.error("Error trace ID: " + traceId);
        log.error(exception.getMessage());
        log.error(ExceptionUtils.getStackTrace(exception));
        return new ResponseEntity<>(errorContainer.toString(),
                HttpStatus.UNAUTHORIZED);
    }
}