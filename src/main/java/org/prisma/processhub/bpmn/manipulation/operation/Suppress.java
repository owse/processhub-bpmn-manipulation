package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Suppress extends BpmnOperation {
    private String suppressedElementId;

    public String getSuppressedElementId() {
        return suppressedElementId;
    }

    public Suppress(String suppressedElementId) {
        this.suppressedElementId = suppressedElementId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.suppress(modelInstance, suppressedElementId);
    }
}
