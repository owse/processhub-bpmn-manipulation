package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class MoveFragment extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveFragment(String startingNodeId, String endingNodeId,
                        String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.move(modelInstance, startingNodeId, endingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public String getStartingNodeId() { return startingNodeId; }
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
        ModelElementInstance currentExtension = initExtensionElement(process);

        currentExtension.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID, newPositionAfterOfId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID, newPositionBeforeOfId);
    }
}