package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Suppress extends BpmntOperation {
    private String suppressedElementId;

    Suppress(int executionOrder, String suppressedElementId) {
        this.executionOrder = executionOrder;
        this.suppressedElementId = suppressedElementId;
        name = "Suppress";
    }

    public String getSuppressedElementId() {
        return suppressedElementId;
    }
}
