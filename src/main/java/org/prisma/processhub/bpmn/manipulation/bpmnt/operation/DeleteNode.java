package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class DeleteNode extends BpmntOperation {
    private String nodeId;

    public DeleteNode(int executionOrder, String nodeId) {
        this.executionOrder = executionOrder;
        this.nodeId = nodeId;
        this.name = "DeleteNode";
    }

    public String getNodeId() {
        return nodeId;
    }
}