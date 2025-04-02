package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.util.HttpUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Slf4j
public class APILogService {
    private JwtHelper jwtHelper;

    @Autowired
    public APILogService(JwtHelper jwtHelper) {
        this.jwtHelper = jwtHelper;
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public void logApiCall(HttpServletRequest request, Object... params) {
        UserDetails ud = null;
        Logger logger = Logger.getLogger("APILog");
        try { ud = jwtHelper.getUserDetail(HttpUtils.getToken(request)); } catch (Exception e) { logger.log(Level.INFO, e.getMessage(), e); }

        String ip = request.getRemoteAddr();
        for (String header: IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                ip = ipList.split(",")[0];
            }
        }

        boolean f = false;
        for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
            if (f) {
                logger.info("call " + request.getMethod() + " " + el.getClassName() + "." + el.getMethodName() + "(" + StringUtils.join(Arrays.stream(params).map(x -> x == null ? "null" : x.toString()).toArray(), ", ") + ")");
                logger.info("user " + (ud == null ? "null" : (ud.getUid() + " " + ud.getUsername() + " - " + ud.getDisplayName())));
                logger.info("user-agent " + request.getHeader(HttpHeaders.USER_AGENT));
                logger.info("origin " + request.getHeader(HttpHeaders.ORIGIN));
                logger.info("IP " + ip);
                break;
            }
            if (el.getClassName().contains(".APILogService"))
                f = true;
        }
    }
}
