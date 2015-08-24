package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ReplaceFragmentWithNode extends BpmntInsertionDependentOperation {
    private String startingNodeId;
    private String endingNodeId;
    private FlowNode replacingNode;

    public ReplaceFragmentWithNode(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingNode = replacingNode;
        this.name = "ReplaceFragmentWithNode";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public FlowNode getReplacingNode() {
        return replacingNode;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
        BpmnElementCreator.add(subProcess, replacingNode);
    }
}
