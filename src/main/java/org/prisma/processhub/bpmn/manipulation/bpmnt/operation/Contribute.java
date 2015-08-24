package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class Contribute extends BpmntInsertionDependentOperation {
    private FlowElement newElement;

    public Contribute(FlowElement newElement) {
        this.newElement = newElement;
        name = "Contribute";
    }

    public FlowElement getNewElement() {
        return newElement;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);

        BpmnElementCreator.add(subProcess, newElement);
    }

}
