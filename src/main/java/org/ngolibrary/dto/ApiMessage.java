package org.ngolibrary.dto;

public class ApiMessage {
    private String message;

    public ApiMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
