"""Application configuration management using Pydantic."""

import os
from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables.
    
    Supports loading from .env file using python-dotenv.
    All settings can be overridden via environment variables.
    
    Example:
        >>> from playwright_ui_adapter.config import settings
        >>> print(settings.llm_provider)
        'gemini-vertex'
    """
    
    # LLM Configuration
    llm_provider: str = "gemini-vertex"
    
    # Vertex AI Settings
    vertex_project: Optional[str] = None
    google_cloud_project: Optional[str] = None  # Alias for vertex_project
    vertex_location: str = "us-central1"
    google_cloud_location: Optional[str] = None  # Alias for vertex_location
    gemini_model: str = "gemini-2.5-flash"
    
    # Groq Settings
    groq_api_key: Optional[str] = None
    groq_model: str = "openai/gpt-oss-safeguard-20b"
    
    # Agent Settings
    llm_usage_log_enabled: bool = True
    llm_usage_log_file: str = "llm-usage.txt"
    llm_max_execution_messages_to_send: int = 10
    validation_wire_log_enabled: bool = True
    
    # Application Secrets
    my_sso_password: Optional[str] = None
    my_app_password: Optional[str] = None
    
    # Google Cloud Authentication
    google_application_credentials: Optional[str] = None
    
    # Browser Settings
    playwright_headless: bool = False
    playwright_browser: str = "chromium"
    
    # Screenshot Settings
    screenshot_dir: str = "screenshots"
    screenshot_on_failure: bool = True
    
    # Action Logging
    action_log_enabled: bool = True
    action_log_dir: str = "logs/action-logs"
    
    @property
    def effective_vertex_project(self) -> Optional[str]:
        """Get effective Vertex project (supports multiple env var names)."""
        return self.vertex_project or self.google_cloud_project
    
    @property
    def effective_vertex_location(self) -> str:
        """Get effective Vertex location (supports multiple env var names)."""
        return self.google_cloud_location or self.vertex_location
    
    class Config:
        """Pydantic configuration."""
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "allow"  # Allow extra fields from environment


# Global settings instance
settings = Settings()
