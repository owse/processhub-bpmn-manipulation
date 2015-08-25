package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceNodeWithFragment extends BpmnOperation {
    private String replacedNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceNodeWithFragment(String replacedNodeId, BpmnModelInstance replacingFragment) {
        this.replacedNodeId = replacedNodeId;
        this.replacingFragment = replacingFragment;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, replacedNodeId, replacingFragment);
    }

    public String getReplacedNodeId() {
        return replacedNodeId;
    }
    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }
}