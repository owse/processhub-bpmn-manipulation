package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ConditionalInsertNode extends BpmntInsertionDependentOperation {
    private String afterOfId;
    private String beforeOfId;
    private FlowNode flowNodeToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertNode(int executionOrder, String afterOfId, String beforeOfId,
                                 FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        this.executionOrder = executionOrder;
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
        this.name = "ConditionalInsertNode";
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

    public String getCondition() {
        return condition;
    }

    public boolean isInLoop() {
        return inLoop;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID, afterOfId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID, beforeOfId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.CONDITION, condition);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.IN_LOOP, Boolean.toString(inLoop));
        BpmnElementCreator.add(subProcess, flowNodeToInsert);
    }
}