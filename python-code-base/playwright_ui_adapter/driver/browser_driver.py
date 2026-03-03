"""Abstract base class for browser drivers."""

from abc import ABC, abstractmethod
from typing import List

from playwright.sync_api import Page


class BrowserDriver(ABC):
    """
    Abstract base class for browser drivers.
    
    Defines the contract for browser automation drivers.
    Implementations should handle browser lifecycle, page management,
    and basic browser operations.
    """
    
    @abstractmethod
    def start(self) -> None:
        """
        Start the browser and initialize resources.
        
        Should launch the browser, create context, and prepare for automation.
        """
        pass
    
    @abstractmethod
    def get_page(self) -> Page:
        """
        Get the current active page.
        
        Returns:
            The currently active Playwright Page instance
            
        Raises:
            RuntimeError: If browser hasn't been started
        """
        pass
    
    @abstractmethod
    def get_all_pages(self) -> List[Page]:
        """
        Get all open pages/tabs.
        
        Returns:
            List of all open Playwright Page instances
            
        Raises:
            RuntimeError: If browser hasn't been started
        """
        pass
    
    @abstractmethod
    def switch_to_page(self, index: int) -> None:
        """
        Switch to a specific page by index.
        
        Args:
            index: Zero-based index of the page to switch to
            
        Raises:
            IndexError: If index is out of range
            RuntimeError: If browser hasn't been started
        """
        pass
    
    @abstractmethod
    def take_screenshot(self) -> bytes:
        """
        Take a screenshot of the current page.
        
        Returns:
            Screenshot bytes
        """
        pass
    
    @abstractmethod
    def close(self) -> None:
        """
        Close the browser and cleanup resources.
        
        Should close all pages, browser, and Playwright instance.
        """
        pass
