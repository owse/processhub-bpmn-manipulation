package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class Extend extends BpmntOperation {
    private String baseProcessId;
    private String newProcessId;

    public Extend (String baseProcessId) {
        this.name = "Extend";
        this.executionOrder = 1;
        this.baseProcessId = baseProcessId;
        this.newProcessId = BpmntExtensionAttributes.MODIFIED_PROCESS_ID_PREFIX + baseProcessId;
    }

    public String getBaseProcessId() {
        return baseProcessId;
    }

    public String getNewProcessId() {
        return newProcessId;
    }

    @Override
    public void generateExtensionElement(Process process) {
        if (process.getExtensionElements() == null) {
            ModelInstance modelInstance = process.getModelInstance();
            process.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));

            ModelElementInstance processExtension =
                    process
                        .getExtensionElements()
                        .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);

            processExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));
            processExtension.setAttributeValue(BpmntExtensionAttributes.BASE_PROCESS_ID, baseProcessId);
            processExtension.setAttributeValue(BpmntExtensionAttributes.NEW_PROCESS_ID, newProcessId);
        }
    }
}
