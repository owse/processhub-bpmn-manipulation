package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class DeleteNode extends BpmntOperation {
    private String nodeId;

    public DeleteNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.delete(modelInstance, nodeId);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        // Add operation extensions with its attributes
        ModelElementInstance currentExtension = initExtensionElement(process);
        currentExtension.setAttributeValue(BpmntExtensionAttributes.NODE_ID, nodeId);
    }
}