package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class DeleteFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;

    public DeleteFragment(int executionOrder, String startingNodeId, String endingNodeId) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = startingNodeId;
        this.name = "DeleteFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
    }
}