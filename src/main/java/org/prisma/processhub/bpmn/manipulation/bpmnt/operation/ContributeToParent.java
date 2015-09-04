package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ContributeToParent extends BpmntInsertionDependentOperation {
    private String parentElementId;
    private FlowElement newElement;

    public FlowElement getNewElement() {
        return newElement;
    }
    public String getParentElementId() {
        return parentElementId;
    }

    public ContributeToParent(String parentElementId, FlowElement newElement) {
        this.newElement = newElement;
        this.parentElementId = parentElementId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.contribute(modelInstance, parentElementId, newElement);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        BpmnElementHandler.contribute((BpmnModelInstance) subProcess.getModelInstance(), subProcess, newElement);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.PARENT_ELEMENT_ID, parentElementId);
    }
}
