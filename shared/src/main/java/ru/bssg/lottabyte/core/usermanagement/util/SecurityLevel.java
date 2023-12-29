package ru.bssg.lottabyte.core.usermanagement.util;

public enum SecurityLevel {
    /**
     * Требуются все перечисленные роли и валидный токен. Если список пустой, то доступ разрешён для всех.
     */
    ALL_ROLES_STRICT,

    /**
     * Требуется любая из перечисленных ролей и валидный токен.
     */
    ANY_ROLE,

    /**
     * Требуется только валидный токен.
     */
    VALID_TOKEN
}
