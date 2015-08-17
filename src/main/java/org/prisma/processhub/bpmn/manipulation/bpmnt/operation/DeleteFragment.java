package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class DeleteFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;

    public DeleteFragment(int executionOrder, String startingNodeId, String endingNodeId) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = startingNodeId;
        this.name = "DeleteFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }
}