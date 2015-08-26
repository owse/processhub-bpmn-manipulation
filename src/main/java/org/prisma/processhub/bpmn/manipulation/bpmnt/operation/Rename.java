package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Rename extends BpmntOperation {
    private String elementId;
    private String newName;

    public Rename(String elementId, String newName) {
        this.elementId = elementId;
        this.newName = newName;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.rename(modelInstance, elementId, newName);
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = initExtensionElement(process);

        currentExtension.setAttributeValue(BpmntExtensionAttributes.ELEMENT_ID, elementId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_NAME, newName);
    }

    public String getElementId() { return elementId; }
    public String getNewName() {
        return newName;
    }
}
