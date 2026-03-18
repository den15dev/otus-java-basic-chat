package ru.otus.server;

public enum Role {
    USER("user"),
    ADMIN("admin"),
    MODERATOR("moderator");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Role fromString(String input) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(input)) {
                return role;
            }
        }
        return null;
    }
}
