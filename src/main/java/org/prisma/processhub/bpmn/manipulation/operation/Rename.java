package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Rename extends BpmnOperation {
    private String elementId;
    private String newName;

    public Rename(String elementId, String newName) {
        this.elementId = elementId;
        this.newName = newName;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.rename(modelInstance, elementId, newName);
    }

    public String getElementId() { return elementId; }
    public String getNewName() {
        return newName;
    }
}
