package ru.bssg.lottabyte.core.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ErrorCode;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.Language;

@Data
@EqualsAndHashCode(callSuper=false)
public class LottabyteException extends Exception {

    private HttpStatus httpStatusCode;
    private ErrorCode errorCode;
    private Message msgId;
    private Object[] msgArgs;

    public LottabyteException(Message msgId, Throwable cause, Object... msgArgs) {
        this((ErrorCode)null, msgId, cause, msgArgs);
    }

    public LottabyteException(ErrorCode code, Message msgId, Throwable cause, Object... msgArgs) {
        super((String)null, cause);
        this.httpStatusCode = msgId == null ? HttpStatus.BAD_REQUEST : msgId.getHttpStatus();
        this.errorCode = code;
        this.msgId = msgId;
        this.msgArgs = msgArgs;
    }

    public LottabyteException() {
        this((HttpStatus)HttpStatus.INTERNAL_ERROR, (String)null);
    }

    public LottabyteException(String message) {
        this((String)message, (Throwable)null);
    }

    public LottabyteException(String message, Throwable cause) {
        this(HttpStatus.INTERNAL_ERROR, message, cause);
    }

    public LottabyteException(Message msgId, Language language) {
        this(msgId == null ? HttpStatus.BAD_REQUEST : msgId.getHttpStatus(), Message.format(msgId, (language == null ? Language.en : language).name(), new Object[]{}));
        this.msgId = msgId;
    }

    public LottabyteException(Message msgId, Language language, Object... msgArgs) {
        this(msgId == null ? HttpStatus.BAD_REQUEST : msgId.getHttpStatus(), Message.format(msgId, (language == null ? Language.en : language).name(), msgArgs));
        this.msgId = msgId;
    }

    public LottabyteException(HttpStatus status, String message) {
        this((HttpStatus)status, (String)message, (Throwable)null);
    }

    public LottabyteException(HttpStatus status, String message, Throwable cause) {
        this(status, ErrorCode.fromStatus(status), message, cause);
    }

    public LottabyteException(HttpStatus status, ErrorCode code, String message) {
        this(status, (ErrorCode)code, (String)message, (Throwable)null);
    }

    public LottabyteException(HttpStatus status, ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = code;
        this.httpStatusCode = status;
    }

    public String getMessage() {
        String msg = super.getMessage();
        if (msg == null && this.msgId != null) {
            //msg = Message.getString(this.msgId, this.msgArgs);
        } else if (msg == null && super.getCause() != null) {
            msg = super.getCause().getMessage();
        }
        return msg;
    }

    public String getLocalizedMessage() {
        String msg = super.getLocalizedMessage();
        if (msg == null && this.msgId != null) {
            /*Locale locale = Locale.ENGLISH;
            ISessionInfo sessionInfo = SessionManager.getCurrentSession();
            if (sessionInfo != null) {
                locale = sessionInfo.getLocale();
            }

            msg = Message.format(this.msgId, locale, this.msgArgs);*/
        } else if (msg == null && super.getCause() != null) {
            msg = super.getCause().getLocalizedMessage();
        }

        return msg;
    }




}
