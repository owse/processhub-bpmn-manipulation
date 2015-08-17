package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Rename extends BpmntOperation {
    private String elementId;
    private String newName;

    public Rename(int executionOrder, String elementId, String newName) {
        this.executionOrder = executionOrder;
        this.elementId = elementId;
        this.newName = newName;
        this.name = "Rename";
    }

    public String getElementId() {
        return elementId;
    }

    public String getNewName() {
        return newName;
    }
}
