package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ConditionalInsertFragment extends BpmntInsertionDependentOperation {
    private String afterOfId;
    private String beforeOfId;
    private BpmnModelInstance fragmentToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertFragment(String afterOfId, String beforeOfId,
                                     BpmnModelInstance fragmentToInsert, String condition, boolean inLoop) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.fragmentToInsert = fragmentToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.conditionalInsert(modelInstance, afterOfId, beforeOfId, fragmentToInsert, condition, inLoop);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        // Create subprocess container
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        // Add attributes to the operation
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID, afterOfId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID, beforeOfId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.CONDITION, condition);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.IN_LOOP, Boolean.toString(inLoop));
        BpmnElementHandler.convertModelToSubprocess(subProcess, fragmentToInsert);
    }

    public String getAfterOfId() { return afterOfId; }
    public String getBeforeOfId() { return beforeOfId; }
    public BpmnModelInstance getFragmentToInsert() {
        return fragmentToInsert;
    }
    public String getCondition() {
        return condition;
    }
    public boolean isInLoop() {
        return inLoop;
    }
}