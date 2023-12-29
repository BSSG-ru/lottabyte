package ru.bssg.lottabyte.core.usermanagement.security.annotation;

import ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Secured {
    String[] roles() default {};
    SecurityLevel level() default SecurityLevel.VALID_TOKEN;
}