package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

public class ReplaceFragmentWithFragment extends BpmntInsertionDependentOperation {
    private String startingNodeId;
    private String endingNodeId;
    private BpmnModelInstance replacingFragment;

    public ReplaceFragmentWithFragment(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        this.startingNodeId = startingNodeId;
        this.endingNodeId = endingNodeId;
        this.replacingFragment = replacingFragment;
        this.name = "ReplaceFragmentWithFragment";
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public String getEndingNodeId() {
        return endingNodeId;
    }

    public BpmnModelInstance getReplacingFragment() {
        return replacingFragment;
    }

    @Override
    public void generateExtensionElement(Process process) {
        SubProcess subProcess = generateSubProcessContainer(process);
        ModelElementInstance subProcessExt = subProcess.getExtensionElements().getElementsQuery().singleResult();

        subProcessExt.setAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID, startingNodeId);
        subProcessExt.setAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID, endingNodeId);
        BpmnElementCreator.convertModelToSubprocess(subProcess, replacingFragment);
    }
}