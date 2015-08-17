package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Split extends BpmntOperation {
    private String taskId;

    public Split(int executionOrder, String taskId) {
        this.executionOrder = executionOrder;
        this.taskId = taskId;
        this.name = "Split";
    }

    public String getTaskId() {
        return taskId;
    }

}