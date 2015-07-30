package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.util.Collection;

public final class BpmnElementRemover {
    private BpmnElementRemover () {}

    // Returns a BPMN model with the desired sequence flow removed
    public static void removeSequenceFlow(BpmnModelInstance modelInstance, SequenceFlow sequenceFlow) {

        if (modelInstance == null || sequenceFlow == null) {
            return;
        }

        modelInstance.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class).iterator().next().getFlowElements().remove(sequenceFlow);
        return;
    }

    // Returns a BPMN model with the desired list of sequence flows removed
    public static void removeAllSequenceFlows(BpmnModelInstance modelInstance, Collection<SequenceFlow> sequenceFlows) {

        if (modelInstance == null || sequenceFlows == null) {
            return;
        }

        modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements().removeAll(sequenceFlows);
        return;
    }

    // Removes a flow node and all sequence flows connected to it
    public static void removeFlowNode(BpmnModelInstance modelInstance, String flowNodeId) {

        if (modelInstance == null || flowNodeId == null) {
            return;
        }
        FlowNode flowNode = modelInstance.getModelElementById(flowNodeId);

        Collection<SequenceFlow> sequenceFlowsIn = flowNode.getIncoming();
        Collection<SequenceFlow> sequenceFlowsOut = flowNode.getOutgoing();

        removeAllSequenceFlows(modelInstance, sequenceFlowsIn);
        removeAllSequenceFlows(modelInstance, sequenceFlowsOut);
        modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements().remove(flowNode);

        return;
    }

    public static void isolateFlowNode(FlowNode flowNode) {
        Collection<SequenceFlow> incomingFlows = flowNode.getIncoming();
        Collection<SequenceFlow> outgoingFlows = flowNode.getOutgoing();

        incomingFlows.clear();
        outgoingFlows.clear();
    }
}

