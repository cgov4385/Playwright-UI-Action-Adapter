package ui_adapter.model;

public class Action {
    public enum Type {
        NAVIGATE,
        CLICK,
        TYPE,
        WAIT_FOR_VISIBLE,
        ASSERT_VISIBLE,
        ASSERT_TEXT,
        SCREENSHOT,
        SCROLL,
        SWITCH_TAB
    }

    private final Type type;
    private final Selector selector;
    private final String value; // For TYPE, NAVIGATE (url), ASSERT_TEXT

    public Action(Type type, Selector selector, String value) {
        this.type = type;
        this.selector = selector;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Selector getSelector() {
        return selector;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Action{" +
                "type=" + type +
                ", selector=" + selector +
                ", value='" + value + '\'' +
                '}';
    }
}
