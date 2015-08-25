package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class MoveNode extends BpmnOperation {
    private String nodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveNode(String nodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.nodeId = nodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.move(modelInstance, nodeId, newPositionAfterOfId, newPositionBeforeOfId);
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