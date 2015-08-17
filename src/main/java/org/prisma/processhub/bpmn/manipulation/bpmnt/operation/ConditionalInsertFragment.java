package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class ConditionalInsertFragment extends BpmntOperation {
    private String afterOfId;
    private String BeforeOfId;
    private BpmnModelInstance fragmentToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertFragment(int executionOrder, String afterOfId, String BeforeOfId,
                                     BpmnModelInstance fragmentToInsert, String condition, boolean inLoop) {
        this.executionOrder = executionOrder;
        this.afterOfId = afterOfId;
        this.BeforeOfId = BeforeOfId;
        this.fragmentToInsert = fragmentToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
        this.name = "ConditionalInsertFragment";
    }

    public String getAfterOfId() {
        return afterOfId;
    }

    public String getBeforeOfId() {
        return BeforeOfId;
    }

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