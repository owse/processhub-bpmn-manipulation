package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.instance.*;
import org.prisma.processhub.bpmn.manipulation.exception.IllegalFragmentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BpmnFragmentHandler {
    private BpmnFragmentHandler() {}

    // Returns a list with all flow nodes between startingNode and endingNode (both inclusive)
    public static Collection<FlowNode> mapProcessFragment(FlowNode startingNode, FlowNode endingNode) {

        BpmnHelper.checkInvalidArgument(startingNode instanceof StartEvent || endingNode instanceof EndEvent,
                "Nodes passed as arguments must not be of type StartEvent or EndEvent");

        // Add starting node to collection
        Collection<FlowNode> flowNodes = new ArrayList<FlowNode>();
        flowNodes.add(startingNode);

        // Return just one node if start equals end of fragment
        if (startingNode.getId().equals(endingNode.getId())) {
            return flowNodes;
        }

        // Call a recursive algorithm in the nodes following the first to cover all paths
        Collection<SequenceFlow> sequenceFlows = startingNode.getOutgoing();
        if (sequenceFlows.isEmpty()) {
            throw new IllegalFragmentException("Start and end nodes are not connected");
        }
        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }

        if (!validateProcessFragment(flowNodes)) {
            throw new IllegalFragmentException("Invalid BPMN fragment");
        }

        return flowNodes;
    }

    // Recursive iteration from mapProcessFragment
    private static void mapProcessFragment(Collection<FlowNode> flowNodes, FlowNode currentNode, FlowNode endingNode) {

        // If node already created, return
        for (FlowNode fn: flowNodes) {
            if (fn.getId().equals(currentNode.getId())) {
                return;
            }
        }

        // End reached
        if (currentNode.getId().equals(endingNode.getId())) {
            flowNodes.add(endingNode);
            return;
        }

        if (currentNode instanceof EndEvent) {
            throw new IllegalFragmentException("The fragment contains an EndEvent");
        }

        flowNodes.add(currentNode);

        // Call the recursion in the nodes following the current one
        Collection<SequenceFlow> sequenceFlows = currentNode.getOutgoing();
        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }
    }

    // Verifies if a process fragment is valid
    // Assumes that there are no mixed (convergent and divergent) gateways
    public static boolean validateProcessFragment(Collection<FlowNode> flowNodes) {

        List<Gateway> gateways = new ArrayList<Gateway>();

        for (FlowNode fn: flowNodes) {
            if (fn instanceof Gateway) {
                if (BpmnHelper.isGatewayDivergent((Gateway) fn) || BpmnHelper.isGatewayConvergent((Gateway) fn)) {
                    gateways.add((Gateway) fn);
                }
            }
        }

        if (gateways.size() == 0) {
            return true;
        }

        if (BpmnHelper.isGatewayConvergent(gateways.get(0))) {
            return false;
        }

        if (BpmnHelper.isGatewayDivergent(gateways.get(gateways.size() - 1))) {
            return false;
        }

        Collection<FlowNode> succedingNodes = gateways.get(0).getSucceedingNodes().list();
        Collection<FlowNode> previousNodes = gateways.get(gateways.size() - 1).getPreviousNodes().list();

        // All nodes immediately after the first divergent gateway and immediately before the last convergent gateway
        // must be part of the fragment
        return flowNodes.containsAll(succedingNodes) && flowNodes.containsAll(previousNodes);

    }

}
