package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class Rename extends BpmntOperation {
    private String elementId;
    private String newName;

    public Rename(int executionOrder, String elementId, String newName) {
        this.executionOrder = executionOrder;
        this.elementId = elementId;
        this.newName = newName;
        this.name = "Rename";
    }

    public String getElementId() {
        return elementId;
    }

    public String getNewName() {
        return newName;
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                                                        .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.ELEMENT_ID, elementId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_NAME, newName);
    }
}
