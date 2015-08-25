package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class DeleteNode extends BpmnOperation {
    private String nodeId;

    public DeleteNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.delete(modelInstance, nodeId);
    }

}