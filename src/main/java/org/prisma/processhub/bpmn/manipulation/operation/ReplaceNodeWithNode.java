package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceNodeWithNode extends BpmnOperation {
    private String replacedNodeId;
    private FlowNode replacingNode;

    public ReplaceNodeWithNode(String replacedNodeId, FlowNode replacingNode) {
        this.replacedNodeId = replacedNodeId;
        this.replacingNode = replacingNode;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, replacedNodeId, replacingNode);
    }

    public String getReplacedNodeId() { return replacedNodeId; }
    public FlowNode getReplacingNode() {
        return replacingNode;
    }
}