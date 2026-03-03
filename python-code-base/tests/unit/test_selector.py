"""Unit tests for Selector model."""

import pytest

from playwright_ui_adapter.model import Selector, SelectorType


def test_selector_creation():
    """Test basic selector creation."""
    selector = Selector(
        type=SelectorType.CSS,
        value="#username"
    )
    
    assert selector.type == SelectorType.CSS
    assert selector.value == "#username"
    assert selector.role is None
    assert selector.near_text is None
    assert selector.description is None


def test_selector_with_all_fields():
    """Test selector with all optional fields."""
    selector = Selector(
        type=SelectorType.TEXT,
        value="Submit",
        role="button",
        near_text="Login Form",
        description="Submit button in login form"
    )
    
    assert selector.type == SelectorType.TEXT
    assert selector.value == "Submit"
    assert selector.role == "button"
    assert selector.near_text == "Login Form"
    assert selector.description == "Submit button in login form"


def test_selector_string_representation():
    """Test selector string representation."""
    selector = Selector(
        type=SelectorType.CSS,
        value="#btn-submit",
        near_text="Footer"
    )
    
    str_repr = str(selector)
    assert "CSS=#btn-submit" in str_repr
    assert "near=Footer" in str_repr


def test_selector_types():
    """Test all selector types."""
    types = [
        SelectorType.CSS,
        SelectorType.XPATH,
        SelectorType.TEXT,
        SelectorType.ROLE,
        SelectorType.LABEL,
        SelectorType.PLACEHOLDER,
        SelectorType.TEST_ID,
    ]
    
    for selector_type in types:
        selector = Selector(type=selector_type, value="test")
        assert selector.type == selector_type
        assert str(selector.type) == selector_type.value
