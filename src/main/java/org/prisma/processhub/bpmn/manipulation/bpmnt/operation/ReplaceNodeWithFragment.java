package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ReplaceNodeWithFragment extends BpmntInsertionDependentOperation {
    private String replacedNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceNodeWithFragment(String replacedNodeId, BpmnModelInstance replacingFragment) {
        this.replacedNodeId = replacedNodeId;
        this.replacingFragment = replacingFragment;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.replace(modelInstance, replacedNodeId, replacingFragment);
    }

    @Override
    public void generateExtensionElement(org.camunda.bpm.model.bpmn.instance.Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID, replacedNodeId);
        BpmnElementHandler.convertModelToSubprocess(subProcess, replacingFragment);
    }

    public String getReplacedNodeId() { return replacedNodeId; }
    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }
}