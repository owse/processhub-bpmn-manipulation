package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

// Operations that needs to store one or more elements when generating extensions
public abstract class BpmntInsertionDependentOperation extends BpmntOperation {

    // Create subprocess to act as a container storing elements involved in the operation
    // and create an extension to store info regarding the operation
    protected SubProcess generateSubProcessContainer(Process process) {
        // Create new subprocess in given process
        ModelInstance modelInstance = process.getModelInstance();
        SubProcess subProcess = modelInstance.newInstance(SubProcess.class);
        subProcess.setId(getName() + "_" + executionOrder);
        subProcess.setName(getName() + " " + executionOrder);
        process.addChildElement(subProcess);

        // Create extension
        subProcess.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));
        // Add operation name and execution order to extension
        initExtensionElement(subProcess);

        return subProcess;
    }
}
