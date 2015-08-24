package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ReplaceNodeWithFragment extends BpmntInsertionDependentOperation {
    private String replacedNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceNodeWithFragment(String replacedNodeId, BpmnModelInstance replacingFragment) {
        this.replacedNodeId = replacedNodeId;
        this.replacingFragment = replacingFragment;
        this.name = "ReplaceNodeWithFragment";
    }

    public String getReplacedNodeId() {
        return replacedNodeId;
    }

    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID, replacedNodeId);
        BpmnElementCreator.convertModelToSubprocess(subProcess, replacingFragment);
    }
}