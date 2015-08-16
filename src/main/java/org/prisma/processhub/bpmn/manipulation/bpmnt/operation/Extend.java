package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Extend extends BpmntOperation {
    private String baseProcessId;
    private String newProcessId;

    public Extend (String baseProcessId) {
        name = "extend";
        executionOrder = 1;
        this.baseProcessId = baseProcessId;
        newProcessId = "BPMNt_" + baseProcessId;
    }

    public String getNewProcessId() {
        return newProcessId;
    }
}
