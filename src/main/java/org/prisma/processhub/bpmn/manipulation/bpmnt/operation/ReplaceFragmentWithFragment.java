package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class ReplaceFragmentWithFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceFragmentWithFragment(int executionOrder, String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingFragment = replacingFragment;
        this.name = "ReplaceFragmentWithFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }
}