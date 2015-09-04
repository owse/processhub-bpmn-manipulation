package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class InsertNode extends BpmntInsertionDependentOperation {
    private String afterOfId;
    private String beforeOfId;
    private FlowNode flowNodeToInsert;

    public InsertNode(String afterOfId, String beforeOfId, FlowNode flowNodeToInsert) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.insert(modelInstance, afterOfId, beforeOfId, flowNodeToInsert);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID, afterOfId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID, beforeOfId);
        BpmnElementHandler.contribute((BpmnModelInstance) subProcess.getModelInstance(), subProcess, flowNodeToInsert);
    }

    public String getAfterOfId() {
        return afterOfId;
    }
    public String getBeforeOfId() {
        return beforeOfId;
    }
    public FlowNode getFlowNodeToInsert() {
        return flowNodeToInsert;
    }
}