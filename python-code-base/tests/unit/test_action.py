"""Unit tests for Action model."""

import pytest

from playwright_ui_adapter.model import Action, ActionType, Selector, SelectorType


def test_action_navigate():
    """Test NAVIGATE action."""
    action = Action(
        type=ActionType.NAVIGATE,
        value="https://example.com"
    )
    
    assert action.type == ActionType.NAVIGATE
    assert action.value == "https://example.com"
    assert action.selector is None


def test_action_click():
    """Test CLICK action."""
    selector = Selector(type=SelectorType.TEXT, value="Submit")
    action = Action(
        type=ActionType.CLICK,
        selector=selector
    )
    
    assert action.type == ActionType.CLICK
    assert action.selector == selector
    assert action.value is None


def test_action_type():
    """Test TYPE action."""
    selector = Selector(type=SelectorType.CSS, value="#username")
    action = Action(
        type=ActionType.TYPE,
        selector=selector,
        value="admin"
    )
    
    assert action.type == ActionType.TYPE
    assert action.selector == selector
    assert action.value == "admin"


def test_action_string_masking():
    """Test that password values are masked in string representation."""
    selector = Selector(type=SelectorType.CSS, value="#password")
    action = Action(
        type=ActionType.TYPE,
        selector=selector,
        value="secret123"
    )
    
    str_repr = str(action)
    assert "***" in str_repr
    assert "secret123" not in str_repr


def test_all_action_types():
    """Test all action types can be created."""
    types = [
        ActionType.NAVIGATE,
        ActionType.CLICK,
        ActionType.TYPE,
        ActionType.WAIT,
        ActionType.WAIT_FOR_VISIBLE,
        ActionType.ASSERT_VISIBLE,
        ActionType.ASSERT_TEXT,
        ActionType.SCREENSHOT,
        ActionType.SCROLL,
        ActionType.SWITCH_TAB,
        ActionType.MAXIMIZE_WINDOW,
        ActionType.CLICK_CHECKBOX,
        ActionType.CLOSE_TAB,
        ActionType.COMPLETE,
    ]
    
    for action_type in types:
        action = Action(type=action_type)
        assert action.type == action_type
