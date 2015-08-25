package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public abstract class BpmnOperation {
    protected String name;

    public String getName() { return this.getClass().getSimpleName(); }

    public abstract void execute(BpmnModelInstance modelInstance);
}
