package ru.bssg.lottabyte.core.util;

import org.springframework.http.HttpHeaders;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;

public class HttpUtils {
    public static String getToken(HttpHeaders headers) throws LottabyteException {
        String h = (headers == null ? null : headers.getFirst(HttpHeaders.AUTHORIZATION));
        if (h == null || h.isEmpty())
            throw new LottabyteException(Message.LBE00031, Language.ru);

        return h.replace("Bearer ","");
    }

    public static UserDetails getUserDetails(JwtHelper jwtHelper, String token) throws LottabyteException {
        return jwtHelper.getUserDetail(token);
        /*UserDetails userDetails = new UserDetails();
        userDetails.setTenant("999");
        userDetails.setUid("1000330998");
        return userDetails;*/
    }

}
