package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class Split extends BpmntOperation {
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
}