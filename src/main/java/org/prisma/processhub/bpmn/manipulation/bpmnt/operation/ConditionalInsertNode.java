package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class ConditionalInsertNode extends BpmntOperation {
    private String afterOfId;
    private String BeforeOfId;
    private FlowNode flowNodeToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertNode(int executionOrder, String afterOfId, String BeforeOfId,
                                 FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        this.executionOrder = executionOrder;
        this.afterOfId = afterOfId;
        this.BeforeOfId = BeforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
        this.name = "ConditionalInsertNode";
    }

    public String getAfterOfId() {
        return afterOfId;
    }

    public String getBeforeOfId() {
        return BeforeOfId;
    }

    public FlowNode getFlowNodeToInsert() {
        return flowNodeToInsert;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isInLoop() {
        return inLoop;
    }
}