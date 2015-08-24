package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;

public abstract class BpmntOperation {
    protected int executionOrder;
    protected String name;

    public int getExecutionOrder() {
        return executionOrder;
    }

    public String getName() {
        return name;
    }

    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }

    public abstract void generateExtensionElement(Process process);
}
