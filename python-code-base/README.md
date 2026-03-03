# Playwright UI Action Adapter (Python)

LLM-powered UI test automation framework with two-agent architecture.

## Features

- **Two-Agent Architecture**: Separate ActionAgent and ValidationAgent for execution and validation
- **LLM-Powered**: Uses Google Gemini or Groq for intelligent test execution
- **Excel Test Cases**: Load test cases from Excel files
- **Multiple Selector Strategies**: CSS, XPath, Text, Role, and more
- **Comprehensive Logging**: Detailed action logs and screenshots
- **Flexible Configuration**: Environment-based configuration

## Installation

### Using Poetry (Recommended)

```bash
# Install dependencies
poetry install

# Activate virtual environment
poetry shell

# Install Playwright browsers
playwright install chromium
```

### Using pip

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Install Playwright browsers
playwright install chromium
```

## Configuration

1. Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

2. Edit `.env` with your configuration:
```bash
# For Google Vertex AI
VERTEX_PROJECT=your-project-id
VERTEX_LOCATION=us-central1
GEMINI_MODEL=gemini-2.5-flash

# For Groq
LLM_PROVIDER=groq
GROQ_API_KEY=your-api-key
```

## Usage

### Run Test Cases

```bash
# Using Poetry
poetry run python scripts/main.py --testcase path/to/testcases.xlsx

# Using Python directly
python scripts/main.py --testcase path/to/testcases.xlsx
```

### Run with Validation

```bash
python scripts/validation_main.py --testcase path/to/testcases.xlsx
```

## Development

### Run Tests

```bash
pytest
```

### Code Quality

```bash
# Format code
black playwright_ui_adapter/

# Lint code
ruff check playwright_ui_adapter/

# Type check
mypy playwright_ui_adapter/
```

## Architecture

- `playwright_ui_adapter/model/` - Data models
- `playwright_ui_adapter/agent/` - LLM agents
- `playwright_ui_adapter/executor/` - Action execution
- `playwright_ui_adapter/driver/` - Browser drivers
- `playwright_ui_adapter/selector/` - Selector resolution
- `playwright_ui_adapter/testcase/` - Test case loading

## License

MIT
