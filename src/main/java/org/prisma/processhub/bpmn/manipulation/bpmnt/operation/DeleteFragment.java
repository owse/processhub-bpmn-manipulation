package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class DeleteFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;

    public DeleteFragment(String startingNodeId, String endingNodeId) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.delete(modelInstance, startingNodeId, endingNodeId);
    }

    @Override
    public void generateExtensionElement(Process process) {
        // Set name and execution order of the operation
        ModelElementInstance currentExtension = initExtensionElement(process);
        // Set other attributes of the operation
        currentExtension.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }
    public String getEndingNodeId() {
        return endingNodeId;
    }
}