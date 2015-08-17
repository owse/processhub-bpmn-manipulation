package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowElement;

public class Contribute extends BpmntOperation {
    private FlowElement newElement;

    Contribute(int executionOrder, FlowElement newElement) {
        this.executionOrder = executionOrder;
        this.newElement = newElement;
        name = "Contribute";
    }

    public FlowElement getNewElement() {
        return newElement;
    }

}
