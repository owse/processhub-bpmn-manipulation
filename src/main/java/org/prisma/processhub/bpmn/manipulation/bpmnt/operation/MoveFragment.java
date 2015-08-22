package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class MoveFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveFragment(int executionOrder, String startingNodeId, String endingNodeId,
                            String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.executionOrder = executionOrder;
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
        this.name = "MoveFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public String getNewPositionAfterOfId() {
        return newPositionAfterOfId;
    }

    public String getNewPositionBeforeOfId() {
        return newPositionBeforeOfId;
    }

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID, newPositionAfterOfId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID, newPositionBeforeOfId);
    }
}