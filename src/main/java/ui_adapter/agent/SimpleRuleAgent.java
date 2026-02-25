package ui_adapter.agent;

import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.Selector;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference deterministic agent implementation (no AI).
 *
 * Produces actions sequentially for a fixed scenario and may stop early on failure.
 */
public class SimpleRuleAgent implements TestAgent {

    private final String scenarioText;
    private final List<Action> plan;
    private int index = 0;
    private boolean complete = false;

    public SimpleRuleAgent(String scenarioText) {
        this.scenarioText = scenarioText;
        this.plan = buildPlan();
    }

    @Override
    public Action nextAction(ActionResult previousResult) {
        if (complete) {
            return null;
        }

        // Agent reacts to failure by stopping (no retries / no healing).
        if (previousResult != null && previousResult.getStatus() == ActionResult.Status.FAIL) {
            complete = true;
            return null;
        }

        if (index >= plan.size()) {
            complete = true;
            return null;
        }

        return plan.get(index++);
    }

    @Override
    public boolean isTestComplete() {
        return complete;
    }

    @Override
    public void onTestEnd(List<ActionResult> results) {
        // No external calls; deterministic summary hook.
        int pass = 0;
        int fail = 0;
        for (ActionResult r : results) {
            if (r.getStatus() == ActionResult.Status.PASS) {
                pass++;
            } else {
                fail++;
            }
        }
        System.out.println("Scenario: " + scenarioText);
        System.out.println("Results: PASS=" + pass + ", FAIL=" + fail);
    }

    private List<Action> buildPlan() {
        // Deterministic plan. Selectors/URLs are explicitly provided (no inference).
        List<Action> actions = new ArrayList<>();

        actions.add(new Action(Action.Type.NAVIGATE, null, "https://www.cse.lk/"));

        // Example: just take a screenshot after navigation.
        // Replace with your real selectors/actions for the target site.
        actions.add(new Action(Action.Type.SCREENSHOT, null, null));

        return actions;
    }
}

