package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

public class Contribute extends BpmnOperation {
    private FlowElement newElement;

    public FlowElement getNewElement() {
        return newElement;
    }

    public Contribute(FlowElement newElement) {
        this.newElement = newElement;
    }

    public void execute(BpmnModelInstance modelInstance) {
        Process process = BpmnElementSearcher.findFirstProcess(modelInstance);
        BpmnElementHandler.contribute(modelInstance, process, newElement);
    }

}
