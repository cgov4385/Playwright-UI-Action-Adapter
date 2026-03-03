"""Selector resolver for converting selectors to Playwright locators."""

from typing import Optional

from playwright.sync_api import Page, Locator

from ..model.selector import Selector, SelectorType


class SelectorResolver:
    """
    Resolves selectors to Playwright locators.
    
    Supports multiple selector strategies including CSS, XPath, text,
    role-based selectors, and more.
    
    Example:
        >>> resolver = SelectorResolver()
        >>> selector = Selector(type=SelectorType.TEXT, value="Submit")
        >>> locator = resolver.resolve(page, selector)
        >>> locator.click()
    """
    
    def resolve(self, page: Page, selector: Selector) -> Locator:
        """
        Resolve a selector to a Playwright locator.
        
        Args:
            page: The Playwright page instance
            selector: The selector to resolve
            
        Returns:
            A Playwright Locator instance
            
        Raises:
            ValueError: If selector type is not supported
        """
        if selector.type == SelectorType.CSS:
            return self._resolve_css(page, selector)
        elif selector.type == SelectorType.XPATH:
            return self._resolve_xpath(page, selector)
        elif selector.type == SelectorType.TEXT:
            return self._resolve_text(page, selector)
        elif selector.type == SelectorType.ROLE:
            return self._resolve_role(page, selector)
        elif selector.type == SelectorType.LABEL:
            return self._resolve_label(page, selector)
        elif selector.type == SelectorType.PLACEHOLDER:
            return self._resolve_placeholder(page, selector)
        elif selector.type == SelectorType.TEST_ID:
            return self._resolve_test_id(page, selector)
        else:
            raise ValueError(f"Unsupported selector type: {selector.type}")
    
    def _resolve_css(self, page: Page, selector: Selector) -> Locator:
        """Resolve CSS selector."""
        locator = page.locator(selector.value)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_xpath(self, page: Page, selector: Selector) -> Locator:
        """Resolve XPath selector."""
        locator = page.locator(f"xpath={selector.value}")
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_text(self, page: Page, selector: Selector) -> Locator:
        """Resolve text-based selector."""
        # Use Playwright's text selector with exact match
        locator = page.get_by_text(selector.value, exact=False)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_role(self, page: Page, selector: Selector) -> Locator:
        """Resolve ARIA role selector."""
        if selector.role:
            # If role is explicitly provided, use it
            locator = page.get_by_role(selector.role, name=selector.value)
        else:
            # Otherwise, value should be the role and name combined
            locator = page.get_by_role(selector.value)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_label(self, page: Page, selector: Selector) -> Locator:
        """Resolve label-based selector."""
        locator = page.get_by_label(selector.value)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_placeholder(self, page: Page, selector: Selector) -> Locator:
        """Resolve placeholder-based selector."""
        locator = page.get_by_placeholder(selector.value)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _resolve_test_id(self, page: Page, selector: Selector) -> Locator:
        """Resolve test ID selector."""
        locator = page.get_by_test_id(selector.value)
        return self._apply_near_filter(locator, selector.near_text)
    
    def _apply_near_filter(self, locator: Locator, near_text: Optional[str]) -> Locator:
        """
        Apply near filter to disambiguate elements.
        
        Args:
            locator: The base locator
            near_text: Optional nearby text for disambiguation
            
        Returns:
            Filtered locator
        """
        if near_text:
            # Find the element near the specified text
            near_locator = locator.page.get_by_text(near_text)
            return locator.near(near_locator)
        return locator
