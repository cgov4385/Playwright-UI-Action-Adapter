package ui_adapter.model;

public class Selector {
    public enum Type {
        CSS,
        XPATH,
        TEXT,
        ROLE,
        LABEL,
        PLACEHOLDER,
        TEST_ID
    }

    private final Type type;
    private final String value;

    // Optional semantic metadata to help the executor build robust locators
    private final String role;        // e.g. button, link, textbox, checkbox
    private final String nearText;    // nearby text to disambiguate
    private final String description; // free-form description from the LLM

    public Selector(Type type, String value) {
        this(type, value, null, null, null);
    }

    public Selector(Type type, String value, String role, String nearText, String description) {
        this.type = type;
        this.value = value;
        this.role = role;
        this.nearText = nearText;
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getRole() {
        return role;
    }

    public String getNearText() {
        return nearText;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s=%s (role=%s, nearText=%s, description=%s)",
                type, value, role, nearText, description);
    }
}
