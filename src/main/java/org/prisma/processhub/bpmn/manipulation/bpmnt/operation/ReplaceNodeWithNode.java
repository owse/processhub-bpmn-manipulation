package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ReplaceNodeWithNode extends BpmntInsertionDependentOperation {
    private String replacedNodeId;
    private FlowNode replacingNode;

    public ReplaceNodeWithNode(String replacedNodeId, FlowNode replacingNode) {
        this.replacedNodeId = replacedNodeId;
        this.replacingNode = replacingNode;
        this.name = "ReplaceNodeWithNode";
    }

    public String getReplacedNodeId() {
        return replacedNodeId;
    }

    public FlowNode getReplacingNode() {
        return replacingNode;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID, replacedNodeId);
        BpmnElementCreator.add(subProcess, replacingNode);
    }
}