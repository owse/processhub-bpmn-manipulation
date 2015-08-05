package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.instance.*;
import org.prisma.processhub.bpmn.manipulation.exception.IllegalFragmentException;

import java.util.ArrayList;
import java.util.Collection;

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

    // TODO: CHANGE LOGIC
    // First gateway found that is not mixed must be divergent
    // Last gateway found that is not mixed must be convergent, if a divergent gateway exists
    // All nodes succeeding the first gateway must be part of the input flowNodes


    // Verifies if a process fragment is valid
    public static boolean validateProcessFragment(Collection<FlowNode> flowNodes) {
        Collection<Gateway> gateways = new ArrayList<Gateway>();

        // +1 if gateway divergent
        // -1 if gateway convergent
        //  0 if gateway mixed
        int gatewaySymmetry = 0;

        for (FlowNode fn: flowNodes) {
            if (fn instanceof Gateway) {
                gateways.add((Gateway) fn);
            }
        }

        for (Gateway g: gateways) {
            int numberIncoming = g.getIncoming().size();
            int numberOutgoing = g.getOutgoing().size();

            if (numberOutgoing > numberIncoming) {
                gatewaySymmetry++;
            }
            else if (numberOutgoing < numberIncoming) {
                gatewaySymmetry--;
            }
        }

        if (gatewaySymmetry == 0) {
            return true;
        }

        return false;
    }

}
