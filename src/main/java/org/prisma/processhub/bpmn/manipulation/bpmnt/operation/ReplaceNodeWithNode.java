package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceNodeWithNode extends BpmntInsertionDependentOperation {
    private String replacedNodeId;
    private FlowNode replacingNode;

    public ReplaceNodeWithNode(String replacedNodeId, FlowNode replacingNode) {
        this.replacedNodeId = replacedNodeId;
        this.replacingNode = replacingNode;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, replacedNodeId, replacingNode);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID, replacedNodeId);
        BpmnElementHandler.contribute((BpmnModelInstance) subProcess.getModelInstance(), subProcess, replacingNode);
    }

    public String getReplacedNodeId() { return replacedNodeId; }
    public FlowNode getReplacingNode() {
        return replacingNode;
    }
}