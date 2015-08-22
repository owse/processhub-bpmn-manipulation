package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class Split extends BpmntInsertionDependentOperation {
    private String taskId;
    private BpmnModelInstance newSubProcessModel;

    public Split(int executionOrder, String taskId, BpmnModelInstance newSubProcessModel) {
        this.executionOrder = executionOrder;
        this.taskId = taskId;
        this.newSubProcessModel = newSubProcessModel;
        this.name = "Split";
    }

    public String getTaskId() {
        return taskId;
    }

    public BpmnModelInstance getNewSubProcessModel() {
        return newSubProcessModel;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.TASK_ID, taskId);
        BpmnElementCreator.convertModelToSubprocess(subProcess, newSubProcessModel);
    }
}