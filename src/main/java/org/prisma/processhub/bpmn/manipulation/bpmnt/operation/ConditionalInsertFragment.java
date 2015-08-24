package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ConditionalInsertFragment extends BpmntInsertionDependentOperation {
    private String afterOfId;
    private String beforeOfId;
    private BpmnModelInstance fragmentToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertFragment(String afterOfId, String BeforeOfId,
                                     BpmnModelInstance fragmentToInsert, String condition, boolean inLoop) {
        this.afterOfId = afterOfId;
        this.beforeOfId = BeforeOfId;
        this.fragmentToInsert = fragmentToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
        this.name = "ConditionalInsertFragment";
    }

    public String getAfterOfId() {
        return afterOfId;
    }

    public String getBeforeOfId() {
        return beforeOfId;
    }

    public BpmnModelInstance getFragmentToInsert() {
        return fragmentToInsert;
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
        BpmnElementCreator.convertModelToSubprocess(subProcess, fragmentToInsert);
    }
}