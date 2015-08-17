package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class MoveFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveFragment(int executionOrder, String startingNodeId, String endingNodeId,
                            String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
        this.name = "MoveFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public String getNewPositionAfterOfId() {
        return newPositionAfterOfId;
    }

    public String getNewPositionBeforeOfId() {
        return newPositionBeforeOfId;
    }
}