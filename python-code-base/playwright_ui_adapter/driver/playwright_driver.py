"""Playwright implementation of BrowserDriver."""

from typing import List, Optional

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    sync_playwright,
)

from .browser_driver import BrowserDriver


class PlaywrightDriver(BrowserDriver):
    """
    Playwright implementation of BrowserDriver.
    
    Manages Chromium browser lifecycle using Playwright.
    Supports headless/headed modes and multiple tabs.
    
    Example:
        >>> driver = PlaywrightDriver(headless=False)
        >>> driver.start()
        >>> try:
        ...     page = driver.get_page()
        ...     page.goto("https://example.com")
        ... finally:
        ...     driver.close()
    """
    
    def __init__(self, headless: bool = False):
        """
        Initialize PlaywrightDriver.
        
        Args:
            headless: Whether to run browser in headless mode
        """
        self._headless = headless
        self._playwright: Optional[Playwright] = None
        self._browser: Optional[Browser] = None
        self._context: Optional[BrowserContext] = None
        self._page: Optional[Page] = None
    
    def start(self) -> None:
        """Start Playwright and launch browser."""
        self._playwright = sync_playwright().start()
        self._browser = self._playwright.chromium.launch(headless=self._headless)
        self._context = self._browser.new_context()
        self._page = self._context.new_page()
    
    def get_page(self) -> Page:
        """
        Get the current active page.
        
        Returns:
            The currently active Playwright Page instance
            
        Raises:
            RuntimeError: If browser hasn't been started
        """
        if self._page is None:
            raise RuntimeError("Browser not started. Call start() first.")
        return self._page
    
    def get_all_pages(self) -> List[Page]:
        """
        Get all open pages/tabs.
        
        Returns:
            List of all open Playwright Page instances
            
        Raises:
            RuntimeError: If browser hasn't been started
        """
        if self._context is None:
            raise RuntimeError("Browser not started. Call start() first.")
        return self._context.pages
    
    def switch_to_page(self, index: int) -> None:
        """
        Switch to a specific page by index.
        
        Args:
            index: Zero-based index of the page to switch to
            
        Raises:
            IndexError: If index is out of range
            RuntimeError: If browser hasn't been started
        """
        pages = self.get_all_pages()
        if index < 0 or index >= len(pages):
            raise IndexError(f"Page index {index} out of range. Available pages: {len(pages)}")
        
        self._page = pages[index]
        self._page.bring_to_front()
    
    def take_screenshot(self) -> bytes:
        """
        Take a screenshot of the current page.
        
        Returns:
            Screenshot bytes
        """
        if self._page is None:
            raise RuntimeError("Browser not started. Call start() first.")
        return self._page.screenshot(full_page=True)
    
    def close(self) -> None:
        """Close browser and cleanup resources."""
        if self._context:
            self._context.close()
        if self._browser:
            self._browser.close()
        if self._playwright:
            self._playwright.stop()
    
    def __enter__(self) -> "PlaywrightDriver":
        """Context manager entry."""
        self.start()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """Context manager exit."""
        self.close()
