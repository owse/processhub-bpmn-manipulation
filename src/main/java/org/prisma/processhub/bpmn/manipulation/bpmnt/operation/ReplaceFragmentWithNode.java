package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class ReplaceFragmentWithNode extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;
    private FlowNode replacingNode;

    public ReplaceFragmentWithNode(int executionOrder, String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingNode = replacingNode;
        this.name = "ReplaceFragmentWithNode";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public FlowNode getReplacingNode() {
        return replacingNode;
    }
}
