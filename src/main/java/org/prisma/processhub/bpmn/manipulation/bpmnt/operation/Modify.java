package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

public class Modify extends BpmntOperation {
    String modifiedElementId;
    String property;
    String value;

    Modify(int executionOrder, String modifiedElementId, String property, String value) {
        this.executionOrder = executionOrder;
        this.modifiedElementId = modifiedElementId;
        this.property = property;
        this.value = value;
        name = "modify";
    }

    public String getModifiedElementId() {
        return modifiedElementId;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
}