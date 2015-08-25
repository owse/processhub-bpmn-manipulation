package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Split extends BpmnOperation {
    private String taskId;
    private BpmnModelInstance newSubProcessModel;

    public Split(String taskId, BpmnModelInstance newSubProcessModel) {
        this.taskId = taskId;
        this.newSubProcessModel = newSubProcessModel;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.split(modelInstance, taskId, newSubProcessModel);
    }

    public String getTaskId() {
        return taskId;
    }
    public BpmnModelInstance getNewSubProcessModel() {
        return newSubProcessModel;
    }
}