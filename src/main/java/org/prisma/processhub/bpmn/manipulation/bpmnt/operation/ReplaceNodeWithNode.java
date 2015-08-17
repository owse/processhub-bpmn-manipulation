package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class ReplaceNodeWithNode extends BpmntOperation {
    private String replacedNodeId;
    private FlowNode replacingNode;

    public ReplaceNodeWithNode(int executionOrder, String replacedNodeId, FlowNode replacingNode) {
        this.executionOrder = executionOrder;
        this.replacedNodeId = replacedNodeId;
        this.replacingNode = replacingNode;
        this.name = "ReplaceNodeWithNode";
    }

    public String getReplacedNodeId() {
        return replacedNodeId;
    }

    public FlowNode getReplacingNode() {
        return replacingNode;
    }
}