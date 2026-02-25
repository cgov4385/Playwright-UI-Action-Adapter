@echo off
REM Proteuse - Playwright UI Test Execution
REM Usage: proteuse -testc <path-to-test-cases.xlsx>

REM Find Maven wrapper or use system mvn
if exist "mvnw.cmd" (
    set MVN_CMD=mvnw.cmd
) else (
    set MVN_CMD=mvn
)

REM Execute with all arguments passed through
%MVN_CMD% compile exec:java -Dexec.mainClass="ui_adapter.Main" -Dexec.args="%*" -q
