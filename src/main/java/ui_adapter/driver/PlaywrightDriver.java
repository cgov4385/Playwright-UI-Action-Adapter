package ui_adapter.driver;

import com.microsoft.playwright.*;

public class PlaywrightDriver implements BrowserDriver {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @Override
    public void start() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
    }

    @Override
    public Page getPage() {
        if (page == null) {
            throw new IllegalStateException("Driver not started. Call start() first.");
        }
        return page;
    }

    @Override
    public java.util.List<Page> getAllPages() {
        if (context == null) {
            throw new IllegalStateException("Driver not started. Call start() first.");
        }
        return context.pages();
    }

    @Override
    public void switchToPage(int index) {
        if (context == null) {
            throw new IllegalStateException("Driver not started. Call start() first.");
        }
        java.util.List<Page> pages = context.pages();
        if (index < 0 || index >= pages.size()) {
            throw new IllegalArgumentException("Invalid page index: " + index + ". Total pages: " + pages.size());
        }
        this.page = pages.get(index);
        this.page.bringToFront();
    }

    @Override
    public byte[] takeScreenshot() {
        if (page == null) {
            return new byte[0];
        }
        return page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
