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
                "FlowNodes passed as arguments must not be of type StartEvent or EndEvent");

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
            throw new IllegalFragmentException("First FlowNode in the fragment has no outgoing SequenceFlows");
        }
        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }

        return flowNodes;
    }

    // Recursive iteration from mapProcessFragment
    private static void mapProcessFragment(Collection<FlowNode> flowNodes, FlowNode currentNode, FlowNode endingNode) {

        // If node already created, return
        for (FlowNode node: flowNodes) {
            if (node.getId().equals(currentNode.getId())) {
                return;
            }
        }

        // End reached
        if (currentNode.getId().equals(endingNode.getId())) {
            flowNodes.add(endingNode);
            return;
        }

        // Check if node is start or end event
        if (currentNode instanceof EndEvent) {
            throw new IllegalFragmentException("The fragment contains an EndEvent");
        } else if (currentNode instanceof StartEvent) {
            throw new IllegalFragmentException("The fragment contains a StartEvent");
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
    public static void validateDeleteProcessFragment(Collection<FlowNode> fragment) {

        // Identify all gateways
        List<Gateway> gateways = new ArrayList<Gateway>();
        for (FlowNode node: fragment) {
            if (node instanceof Gateway) {
                Gateway gateway = (Gateway) node;
                // Diregard many to many gateways as possible cause of invalidation
                if (BpmnHelper.isGatewayDivergent(gateway) || BpmnHelper.isGatewayConvergent(gateway)) {
                    gateways.add(gateway);
                }
            }
        }
        // Fragment is valid if there are no gateways
        if (gateways.size() == 0) {
            return;
        }

        // The first gateway can't be convergent (means there' a divergent gateway before the fragment)
        if (BpmnHelper.isGatewayConvergent(gateways.get(0))) {
            throw new IllegalFragmentException("First gateway in fragment is convergent. The previous corresponding " +
                                               "divergent gateway should also be in the fragment to be deleted");
        }

        // The last gateway can't be divergent (means there' a convergent gateway after the fragment)
        if (BpmnHelper.isGatewayDivergent(gateways.get(gateways.size() - 1))) {
            throw new IllegalFragmentException("Last gateway in fragment is divergent. The succeeding corresponding " +
                    "convergent gateway should also be in the fragment to be deleted");
        }

        // All nodes immediately after the first divergent gateway and immediately before the last convergent gateway
        // must be part of the fragment
        Collection<FlowNode> succeedingNodes = gateways.get(0).getSucceedingNodes().list();
        Collection<FlowNode> previousNodes = gateways.get(gateways.size() - 1).getPreviousNodes().list();

        if (!fragment.containsAll(succeedingNodes)) {
            throw new IllegalFragmentException("All nodes immediately after the first divergent gateway " +
                    "should be part of the fragment to be deleted");
        }

        if (!fragment.containsAll(previousNodes)) {
            throw new IllegalFragmentException("All nodes immediately before the last convergent gateway " +
                    "should be part of the fragment to be deleted");
        }

    }

}
