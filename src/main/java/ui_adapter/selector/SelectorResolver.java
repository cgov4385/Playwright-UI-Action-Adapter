package ui_adapter.selector;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import ui_adapter.model.Selector;

public class SelectorResolver {

    /** Simple + reversible switch: pick first match on ambiguity to avoid strict-mode violations. */
    private static final boolean PICK_FIRST_ON_MULTIPLE = true;

    public Locator resolve(Page page, Selector selector) {
        if (selector == null) {
            return null;
        }

        Locator loc = resolveBaseStrategy(page, selector);

        // Optional refinement
        if (loc != null && selector.getNearText() != null && !selector.getNearText().isEmpty()) {
            loc = loc.filter(new Locator.FilterOptions().setHasText(selector.getNearText()));
        }

        if (PICK_FIRST_ON_MULTIPLE && loc != null) {
            loc = loc.first();
        }

        return loc;
    }

    private Locator resolveBaseStrategy(Page page, Selector selector) {
        String value = selector.getValue();
        if (value == null) value = "";

        switch (selector.getType()) {
            case TEST_ID:
                return page.getByTestId(value);

            case LABEL:
                return page.getByLabel(value);

            case PLACEHOLDER:
                return page.getByPlaceholder(value);

            case ROLE:
                // ROLE uses meta.role as AriaRole and value as accessible name
                String roleStr = selector.getRole();
                if (roleStr != null && !roleStr.isEmpty()) {
                    try {
                        AriaRole roleEnum = AriaRole.valueOf(roleStr.toUpperCase().replace(" ", "_"));
                        return page.getByRole(roleEnum, new Page.GetByRoleOptions().setName(value));
                    } catch (IllegalArgumentException ignore) {
                        // fall through to text
                    }
                }
                return page.getByText(value);

            case TEXT:
                // Prefer clickable elements when selecting by text.
                // Try: button with text, then link with text, then any element by text.
                Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(value));
                if (safeHasAny(button)) {
                    return button;
                }
                Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(value));
                if (safeHasAny(link)) {
                    return link;
                }
                return page.getByText(value);

            case CSS:
                return page.locator("css=" + value);

            case XPATH:
                return page.locator("xpath=" + value);

            default:
                return page.locator(value);
        }
    }

    private static boolean safeHasAny(Locator locator) {
        try {
            return locator != null && locator.count() > 0;
        } catch (Exception ignore) {
            return false;
        }
    }
}
