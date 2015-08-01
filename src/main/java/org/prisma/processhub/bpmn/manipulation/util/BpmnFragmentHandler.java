package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.instance.*;

import java.util.ArrayList;
import java.util.Collection;

public final class BpmnFragmentHandler {
    private BpmnFragmentHandler() {}

    // Returns a list with all flow nodes between startingNode and endingNode (both inclusive)
    public static Collection<FlowNode> mapProcessFragment(FlowNode startingNode, FlowNode endingNode) throws Exception {
        if (startingNode instanceof StartEvent || endingNode instanceof EndEvent) {
            return null;
        }

        Collection<FlowNode> flowNodes = new ArrayList<FlowNode>();
        flowNodes.add(startingNode);

        if (startingNode.getId().equals(endingNode.getId())) {
            return flowNodes;
        }

        Collection<SequenceFlow> sequenceFlows = startingNode.getOutgoing();

        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }

        return flowNodes;
    }

    // TODO: fix mapping for incomplete process fragments
    // Recursive iteration from mapProcessFragment
    public static void mapProcessFragment(Collection<FlowNode> flowNodes, FlowNode currentNode, FlowNode endingNode) throws Exception {

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
            throw new Exception("Invalid fragment detected");
        }

        flowNodes.add(currentNode);

        Collection<SequenceFlow> sequenceFlows = currentNode.getOutgoing();

        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }

    }

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
