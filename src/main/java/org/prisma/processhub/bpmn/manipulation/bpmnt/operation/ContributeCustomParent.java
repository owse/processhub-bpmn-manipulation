package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ContributeCustomParent extends BpmntInsertionDependentOperation {
    private String parentElementId;
    private FlowElement newElement;

    public ContributeCustomParent(String parentElementId, FlowElement newElement) {
        this.newElement = newElement;
        this.parentElementId = parentElementId;
        this.name = "ContributeCustomParent";
    }

    public String getParentElementId() {
        return parentElementId;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        BpmnElementCreator.add(subProcess, newElement);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.PARENT_ELEMENT_ID, parentElementId);

    }
}
