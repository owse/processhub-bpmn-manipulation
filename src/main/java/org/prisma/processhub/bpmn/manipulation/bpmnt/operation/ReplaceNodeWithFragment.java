package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class ReplaceNodeWithFragment extends BpmntOperation {
    private String replacedNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceNodeWithFragment(int executionOrder, String replacedNodeId, BpmnModelInstance replacingFragment) {
        this.executionOrder = executionOrder;
        this.replacedNodeId = replacedNodeId;
        this.replacingFragment = replacingFragment;
        this.name = "ReplaceNodeWithFragment";
    }

    public String getReplacedNodeId() {
        return replacedNodeId;
    }

    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }
}