package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

import java.util.concurrent.locks.Condition;

public class ConditionalInsertFragment extends BpmnOperation {
    private String afterOfId;
    private String beforeOfId;
    private BpmnModelInstance fragmentToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertFragment(String afterOfId, String beforeOfId,
                                     BpmnModelInstance fragmentToInsert, String condition, boolean inLoop) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.fragmentToInsert = fragmentToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.conditionalInsert(modelInstance, afterOfId, beforeOfId, fragmentToInsert, condition, inLoop);
    }

    public String getAfterOfId() { return afterOfId; }
    public String getBeforeOfId() { return beforeOfId; }
    public BpmnModelInstance getFragmentToInsert() {
        return fragmentToInsert;
    }
    public String getCondition() {
        return condition;
    }
    public boolean isInLoop() {
        return inLoop;
    }
}