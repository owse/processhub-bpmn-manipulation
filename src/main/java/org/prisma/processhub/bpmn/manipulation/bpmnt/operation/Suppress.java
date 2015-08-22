package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class Suppress extends BpmntOperation {
    private String suppressedElementId;

    public Suppress(int executionOrder, String suppressedElementId) {
        this.executionOrder = executionOrder;
        this.suppressedElementId = suppressedElementId;
        name = "Suppress";
    }

    public String getSuppressedElementId() {
        return suppressedElementId;
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                                                        .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.SUPPRESSED_ELEMENT_ID, suppressedElementId);
    }

}
