package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public abstract class BpmntInsertionDependentOperation extends BpmntOperation {
    protected SubProcess generateSubProcessContainer(Process process) {
        ModelInstance modelInstance = process.getModelInstance();

        SubProcess subProcess = modelInstance.newInstance(SubProcess.class);
        subProcess.setId(name + "_" + executionOrder);
        subProcess.setName(name + " " + executionOrder);
        process.addChildElement(subProcess);

        subProcess.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));

        ModelElementInstance subProcessExt = subProcess
                                                .getExtensionElements()
                                                .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        return subProcess;
    }
}
