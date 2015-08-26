package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

public class Contribute extends BpmntInsertionDependentOperation {
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

    @Override
    public void generateExtensionElement(Process process) {
        // Create subprocess container and add element to it
        SubProcess subProcess = generateSubProcessContainer(process);
        BpmnElementHandler.contribute((BpmnModelInstance) subProcess.getModelInstance(), subProcess, newElement);
    }

}
