package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public class MoveNode extends BpmntOperation {
    private String nodeId;
    private String newPositionAfterOfId;
    private String newPositionBeforeOfId;

    public MoveNode(int executionOrder, String nodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        this.executionOrder = executionOrder;
        this.nodeId = nodeId;
        this.newPositionAfterOfId = newPositionAfterOfId;
        this.newPositionBeforeOfId = newPositionBeforeOfId;
        this.name = "MoveNode";
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

    @Override
    public void generateExtensionElement(Process process) {
        ModelElementInstance currentExtension = process.getExtensionElements()
                .addExtensionElement(BpmntExtensionAttributes.DOMAIN, name);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));

        currentExtension.setAttributeValue(BpmntExtensionAttributes.NODE_ID, nodeId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID, newPositionAfterOfId);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID, newPositionBeforeOfId);
    }
}