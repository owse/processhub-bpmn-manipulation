package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class MoveFragment extends BpmnOperation {
    private String startingNodeId;
    private String endingNodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveFragment(String startingNodeId, String endingNodeId,
                            String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.move(modelInstance, startingNodeId, endingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public String getStartingNodeId() { return startingNodeId; }
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