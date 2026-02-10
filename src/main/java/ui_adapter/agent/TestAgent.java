package ui_adapter.agent;

import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;

import java.util.List;

public interface TestAgent {
    Action nextAction(ActionResult previousResult);

    boolean isTestComplete();

    void onTestEnd(List<ActionResult> results);
}

