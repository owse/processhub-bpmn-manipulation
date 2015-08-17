package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class MoveNode extends BpmntOperation {
    private String nodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveNode(int executionOrder, String nodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.executionOrder = executionOrder;
        this.nodeId = nodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
        this.name = "MoveNode";
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNewPositionAfterOfId() {
        return newPositionAfterOfId;
    }

    public String getNewPositionBeforeOfId() {
        return newPositionBeforeOfId;
    }
}