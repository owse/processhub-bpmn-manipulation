package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class MoveNode extends BpmntOperation {
    private String nodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveNode(String nodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.nodeId = nodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.move(modelInstance, nodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        ModelElementInstance currentExtension = initExtensionElement(process);

        currentExtension.setAttributeValue(BpmntExtensionAttributes.NODE_ID, nodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID, newPositionAfterOfId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID, newPositionBeforeOfId);
    }

    public String getNodeId() {
        return nodeId;
    }
    public String getNewPositionAfterOfId() {
        return newPositionAfterOfId;
    }
    public String getNewPositionBeforeOfId() {
        return newPositionBeforeOfId;
    }
}