package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Parallelize extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;

    public Parallelize(int executionOrder, String startingNodeId, String endingNodeId) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = startingNodeId;
        this.name = "Parallelize";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }
}