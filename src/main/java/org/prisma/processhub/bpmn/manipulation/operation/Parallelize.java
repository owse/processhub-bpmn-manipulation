package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Parallelize extends BpmnOperation {
    private String startingNodeId;
    private String endingNodeId;

    public Parallelize(String startingNodeId, String endingNodeId) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.parallelize(modelInstance, startingNodeId, endingNodeId);
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }
    public String getEndingNodeId() {
        return endingNodeId;
    }
}