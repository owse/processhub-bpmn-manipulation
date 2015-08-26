package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
import org.prisma.processhub.bpmn.manipulation.util.BpmnHelper;

import java.util.Date;

public class Extend extends BpmntOperation {
    private String baseProcessId;
    private String newProcessId;
    public static final String ID_PREFIX = "BPMNt_";

    public Extend (String baseProcessId) {
        this.baseProcessId = baseProcessId;
        newProcessId = generateNewProcessId(baseProcessId);
    }

    // Generates a new unique id for the extended process
    private String generateNewProcessId(String baseProcessId) {
        String newId;
        if (baseProcessId.startsWith(ID_PREFIX)) {
            newId = baseProcessId + "-" + (new Date()).getTime();
        } else {
            newId = ID_PREFIX + baseProcessId;
        }
        return newId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        // Check if model instance to be extended is the same the operation was intended to
        Process process = BpmnElementSearcher.findFirstProcess(modelInstance);
        BpmnHelper.checkInvalidArgument(baseProcessId.equals(process.getId()),
                                        "Can't extend process in modelInstance. Process id doesn't match baseProcessId");
        // Set the new id for the extended process
        process.setId(newProcessId);
    }

    @Override
    public void generateExtensionElement(Process process) {
        if (process.getExtensionElements() == null) {
            ModelInstance modelInstance = process.getModelInstance();
            process.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));

            ModelElementInstance processExtension = initExtensionElement(process);
            processExtension.setAttributeValue(BpmntExtensionAttributes.BASE_PROCESS_ID, baseProcessId);
            processExtension.setAttributeValue(BpmntExtensionAttributes.NEW_PROCESS_ID, newProcessId);
        }
    }

    public String getBaseProcessId() {
        return baseProcessId;
    }
    public String getNewProcessId() {
        return newProcessId;
    }
}
