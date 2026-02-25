package ui_adapter.driver;

import com.microsoft.playwright.Page;
import java.util.List;

public interface BrowserDriver extends AutoCloseable {
    void start();
    Page getPage();

    // Add capability to handle multiple pages
    List<Page> getAllPages();
    void switchToPage(int index);

    void close();
    byte[] takeScreenshot();
}
