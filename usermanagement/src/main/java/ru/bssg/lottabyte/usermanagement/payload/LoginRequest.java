package ru.bssg.lottabyte.usermanagement.payload;

import ru.bssg.lottabyte.core.usermanagement.model.Language;

public class LoginRequest {
    private String username;

    private String password;
    private Language language;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LoginRequest(String username, String password, Language language) {
        this.username = username;
        this.password = password;
        this.language = language;
    }

    public LoginRequest() {
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}