package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Suppress extends BpmntOperation {
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

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        ModelElementInstance currentExtension = initExtensionElement(process);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.SUPPRESSED_ELEMENT_ID, suppressedElementId);
    }
}
