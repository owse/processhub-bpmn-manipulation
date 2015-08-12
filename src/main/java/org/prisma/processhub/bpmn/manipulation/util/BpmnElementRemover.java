package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.Collection;

import static org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher.findFirstProcess;

public final class BpmnElementRemover {
    private BpmnElementRemover () {}

    // Returns a BPMN model with the desired sequence flow removed
    public static void removeSequenceFlow(BpmnModelInstance modelInstance, SequenceFlow sequenceFlow) {
        // Check for null arguments
        BpmnHelper.checkNotNull(modelInstance, "Argument modelInstance must not be null");
        BpmnHelper.checkNotNull(sequenceFlow, "Argument sequenceFlow must not be null");

        // Check if sequence flow is contained in model instance
        BpmnHelper.checkElementPresent(modelInstance.getModelElementById(sequenceFlow.getId()) != null,
                                      "SequenceFlow with id \'" + sequenceFlow.getId() + "\' not part of the given BpmnModelInstance");
        sequenceFlow.getParentElement().removeChildElement(sequenceFlow);
    }

    // Returns a BPMN model with the desired list of sequence flows removed
    public static void removeAllSequenceFlows(BpmnModelInstance modelInstance, Collection<SequenceFlow> sequenceFlows) {

        BpmnHelper.checkNotNull(modelInstance, "Argument modelInstance must not be null");
        BpmnHelper.checkNotNull(sequenceFlows, "Argument sequenceFlows must not be null");

        // Get first process in model instance
        Process process = findFirstProcess(modelInstance);

        // Get all flow elements in first process
        Collection<FlowElement> flowElements = process.getFlowElements();
        if (flowElements.isEmpty()) {
            throw new ElementNotFoundException("No FlowElements found in Process");
        }

        flowElements.removeAll(sequenceFlows);
        return;
    }

    // Removes a flow node and all sequence flows connected to it
    public static void removeFlowNode(BpmnModelInstance modelInstance, String flowNodeId) {

        // Check for null arguments
        BpmnHelper.checkNotNull(modelInstance, "Argument modelInstance must not be null");
        BpmnHelper.checkNotNull(flowNodeId, "Argument flowNodeId must not be null");

        // Get first process in model instance
        Process process = findFirstProcess(modelInstance);

        // Get flow node and sequence flows associated with it
        FlowNode flowNode = modelInstance.getModelElementById(flowNodeId);
        if (flowNode == null) {
            throw new ElementNotFoundException("No FlowNode with given id found in BpmnModelInstance");
        }
        Collection<SequenceFlow> sequenceFlowsIn = flowNode.getIncoming();
        Collection<SequenceFlow> sequenceFlowsOut = flowNode.getOutgoing();

        // Check if there are any flow elements in the process
        Collection<FlowElement> flowElements = process.getFlowElements();
        if (flowElements.isEmpty()) {
            throw new ElementNotFoundException("No FlowElement found in Process");
        }

        // Remove FlowNode and SequenceFlows
        removeAllSequenceFlows(modelInstance, sequenceFlowsIn);
        removeAllSequenceFlows(modelInstance, sequenceFlowsOut);
        flowElements.remove(flowNode);

        return;
    }

    public static void isolateFlowNode(FlowNode flowNode) {
        Collection<SequenceFlow> incomingFlows = flowNode.getIncoming();
        Collection<SequenceFlow> outgoingFlows = flowNode.getOutgoing();

        BpmnModelInstance modelInstance = (BpmnModelInstance) flowNode.getModelInstance();

        removeAllSequenceFlows(modelInstance, incomingFlows);
        removeAllSequenceFlows(modelInstance, outgoingFlows);
    }
}