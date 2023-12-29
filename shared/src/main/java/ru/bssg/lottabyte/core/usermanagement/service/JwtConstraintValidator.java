package ru.bssg.lottabyte.core.usermanagement.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.bssg.lottabyte.core.usermanagement.exception.UnauthorisedException;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JwtConstraintValidator {
    private final @NonNull HttpServletRequest request;
    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private ApiTokensService apiTokensService;

    @Before("within(@ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured *) || @annotation(ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured)")
    public void validateAspect(JoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Secured validateHeaders = method.getAnnotation(Secured.class);

        if(validateHeaders == null) { //If null it was a class level annotation
            Class annotatedClass = joinPoint.getSignature().getDeclaringType();
            validateHeaders = (Secured) annotatedClass.getAnnotation(Secured.class);
        }

        final String authorizationHeaderValue = request.getHeader("Authorization");
        if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer")) {
            String token = authorizationHeaderValue.substring(7, authorizationHeaderValue.length());

            if(!jwtHelper.verifyToken(token)){
                log.error("Токен не прошёл верификацию: " + method);
                throw new UnauthorisedException("Токен не прошёл верификацию: " + method);
            }

            List<String> tokenPermissions = jwtHelper.getPermissions(token);
            boolean hasPermissions = true;

            List<String> requiredRoles = new ArrayList<>(Arrays.asList(validateHeaders.roles()));

            if(jwtHelper.getLongPeriodToken(token)){
                String apiTokenId = jwtHelper.getUid(token);
                if(apiTokensService.longPeriodTokenInBlacklistExist(apiTokenId))
                    throw new UnauthorisedException("This token is on the blacklist: " + apiTokenId);
            }

            switch (validateHeaders.level()) {
                case ANY_ROLE: {
                    for(String requiredRole : requiredRoles){
                        if(tokenPermissions.contains(requiredRole)){
                            return;
                        }
                    }
                    hasPermissions = false;
                    break;
                }
                case ALL_ROLES_STRICT: {
                    for(String requiredRole : requiredRoles){
                        if(!tokenPermissions.contains(requiredRole)){
                            hasPermissions = false;
                            break;
                        }
                    }
                    break;
                }
                default:
                    break;
            }

            if(!hasPermissions){
                log.error("Отсутствуют необходимые разрешения: " + method);
                throw new UnauthorisedException("Отсутствуют необходимые разрешения: " + method);
            }

            return;
        }

        log.error("Токен отсутствует: " + method);
        throw new UnauthorisedException("Токен отсутствует: " + method);
    }
}