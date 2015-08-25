package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceFragmentWithNode extends BpmnOperation {
    private String startingNodeId;
    private String endingNodeId;
    private FlowNode replacingNode;

    public ReplaceFragmentWithNode(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingNode = replacingNode;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, startingNodeId, endingNodeId, replacingNode);
    }

    public String getStartingNodeId() { return startingNodeId; }
    public String getEndingNodeId() {
        return endingNodeId;
    }
    public FlowNode getReplacingNode() {
        return replacingNode;
    }
}
