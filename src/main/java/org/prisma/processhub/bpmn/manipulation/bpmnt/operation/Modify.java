package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class Modify extends BpmntOperation {
    private String modifiedElementId;
    private String property;
    private String value;

    public Modify(String modifiedElementId, String property, String value) {
        this.modifiedElementId = modifiedElementId;
        this.property = property;
        this.value = value;
        name = "Modify";
    }

    public String getModifiedElementId() {
        return modifiedElementId;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                                                        .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.MODIFIED_ELEMENT_ID, modifiedElementId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.PROPERTY, property);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.VALUE, value);
    }


}