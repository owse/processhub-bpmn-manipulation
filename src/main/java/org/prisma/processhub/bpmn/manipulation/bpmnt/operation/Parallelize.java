package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Parallelize extends BpmntOperation {
    private String startingNodeId;
    private String endingNodeId;

    public Parallelize(String startingNodeId, String endingNodeId) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
}

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.parallelize(modelInstance, startingNodeId, endingNodeId);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        ModelElementInstance currentExtension = initExtensionElement(process);

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
