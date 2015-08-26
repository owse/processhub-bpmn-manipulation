package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Split extends BpmntInsertionDependentOperation {
    private String taskId;
    private BpmnModelInstance newSubProcessModel;

    public Split(String taskId, BpmnModelInstance newSubProcessModel) {
        this.taskId = taskId;
        this.newSubProcessModel = newSubProcessModel;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.split(modelInstance, taskId, newSubProcessModel);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.TASK_ID, taskId);
        BpmnElementHandler.convertModelToSubprocess(subProcess, newSubProcessModel);
    }

    public String getTaskId() {
        return taskId;
    }
    public BpmnModelInstance getNewSubProcessModel() {
        return newSubProcessModel;
    }
}