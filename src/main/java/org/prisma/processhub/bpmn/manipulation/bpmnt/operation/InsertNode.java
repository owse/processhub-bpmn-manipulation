package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class InsertNode extends BpmntOperation {
    private String afterOfId;
    private String BeforeOfId;
    private FlowNode flowNodeToInsert;

    public InsertNode(int executionOrder, String afterOfId, String BeforeOfId, FlowNode flowNodeToInsert) {
        this.executionOrder = executionOrder;
        this.afterOfId = afterOfId;
        this.BeforeOfId = BeforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
        this.name = "InsertNode";
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
}