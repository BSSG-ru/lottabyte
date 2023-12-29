package ru.bssg.lottabyte.core.model;

public enum ErrorCode {
    success,
    info,
    redirect,
    unknown,
    client_error,
    not_found,
    conflict,
    server_error,
    service_unavailable,
    initialization_in_progress,
    external_rabbitmq_unavailable,
    invalid_json_provided,
    uniqueness_constraint_violated,
    insufficient_privileges,
    plan_limit_reached,
    database_deadlock,
    missing_required_property,
    unchangeable_property,
    db2_down,
    db2_out_of_service,
    db2_unknown;

    private ErrorCode() {
    }

    public static ErrorCode fromStatus(HttpStatus status) {
        return fromStatus(status.getCode());
    }

    public static ErrorCode fromStatus(int statusCode) {
        switch(statusCode / 100) {
            case 1:
                return info;
            case 2:
                return success;
            case 3:
                return redirect;
            case 4:
                switch(statusCode) {
                    case 404:
                        return not_found;
                    case 409:
                        return conflict;
                    default:
                        return client_error;
                }
            case 5:
                switch(statusCode) {
                    case 503:
                        return service_unavailable;
                    default:
                        return server_error;
                }
            default:
                return unknown;
        }
    }
}

