package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceFragmentWithFragment extends BpmnOperation {
    private String startingNodeId;
    private String endingNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceFragmentWithFragment(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingFragment = replacingFragment;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, startingNodeId, endingNodeId, replacingFragment);
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