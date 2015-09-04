package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Modify extends BpmntOperation {
    String modifiedElementId;
    String property;
    String value;

    public String getModifiedElementId() {
        return modifiedElementId;
    }
    public String getProperty() {
        return property;
    }
    public String getValue() {
        return value;
    }

    public Modify(String modifiedElementId, String property, String value) {
        this.modifiedElementId = modifiedElementId;
        this.property = property;
        this.value = value;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.modify(modelInstance, modifiedElementId, property, value);
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = initExtensionElement(process);

        currentExtension.setAttributeValue(BpmntExtensionAttributes.MODIFIED_ELEMENT_ID, modifiedElementId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.PROPERTY, property);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.VALUE, value);
    }
}