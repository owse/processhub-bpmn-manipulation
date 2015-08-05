package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.*;

import java.util.ArrayList;
import java.util.Collection;

public class BpmntModelInstanceImpl extends BpmnModelInstanceImpl implements BpmntModelInstance {

    // Constructor
    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }


    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Remove flow element leaving the rest of the model untouched
    public void suppress (FlowElement targetElement) {

        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) targetElement.getParentElement();
        parentElement.removeChildElement(targetElement);
        return;
    }

    public void suppress (String targetElementId) {
        FlowElement targetElement = getModelElementById(targetElementId);
        if (targetElement == null) {
            throw new ElementNotFoundException("Flow Element with id \'" + targetElementId +  "\' not found");
        }
        suppress(targetElement);
        return;
    }

    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename element
    public void rename(String targetElementId, String newName) {
        FlowElement flowElementToRename = getModelElementById(targetElementId);
        if (flowElementToRename == null) {
            throw new ElementNotFoundException("Flow Element with id \'" + targetElementId + "\' not found");
        }
        flowElementToRename.setName(newName);
        return;
    }

    // Delete a node
    public void delete(FlowNode targetNode){

        // Gateways, start and end events are not allowed to be deleted
        BpmnHelper.checkInvalidArgument(targetNode instanceof Gateway || targetNode instanceof StartEvent || targetNode instanceof EndEvent,
                "Argument FlowNode must not be a Gateway, StartEvent or EndEvent");

        // Get all incoming and outgoing sequence flows from the node
        Collection<SequenceFlow> incomingSequenceFlows = targetNode.getIncoming();
        Collection<SequenceFlow> outgoingSequenceFlows = targetNode.getOutgoing();

        Collection<FlowNode> beforeNodes = new ArrayList<FlowNode>();
        Collection<FlowNode> afterNodes = new ArrayList<FlowNode>();
        Collection<Gateway> gateways = new ArrayList<Gateway>();

        // Get all nodes and gateways before the node
        for (SequenceFlow sf: incomingSequenceFlows) {
            FlowNode source = sf.getSource();
            if (source instanceof Gateway) {
                gateways.add((Gateway) source);
            }
            else {
                beforeNodes.add(source);
            }
        }

        // Get all nodes and gateways after the node
        for (SequenceFlow sf: outgoingSequenceFlows) {
            FlowNode destination = sf.getTarget();
            if (destination instanceof Gateway) {
                gateways.add((Gateway) destination);
            }
            else {
                afterNodes.add(destination);
            }
        }

        // Gateways should have at least 3 incoming or outgoing sequence flows
        // Delete gateway if it has less than that discounting the sequence flow that
        // will be removed when the node is deleted

        // Should also check if there is at least one sequence flow after node deletion
        Collection<Gateway> gatewaysToDelete = new ArrayList<Gateway>();
        for (Gateway g: gateways) {
            if (g.getIncoming().size() + g.getOutgoing().size() - 1 < 3) {
                gatewaysToDelete.add(g);
            }
        }

        // Remove flow node and all sequence flows connected to it
        BpmnElementRemover.removeFlowNode(this, targetNode.getId());

        for (Gateway g: gatewaysToDelete) {
            FlowNode incomingNode = g.getIncoming().iterator().next().getSource();
            FlowNode outgoingNode = g.getOutgoing().iterator().next().getTarget();
            incomingNode.builder().connectTo(outgoingNode.getId());
            BpmnElementRemover.removeFlowNode(this, g.getId());
        }

        // Connect nodes before the removed one to nodes after it
        for (FlowNode beforeNode: beforeNodes) {
            for (FlowNode afterNode: afterNodes) {
                beforeNode.builder().connectTo(afterNode.getId());
            }
        }
        return;
    }

    public void delete(String targetNodeId) {
        FlowNode targetNode = getModelElementById(targetNodeId);
        if (targetNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + targetNodeId +  "\' not found");
        }
        delete(targetNode);
        return;
    }

    public void delete(FlowNode startingNode, FlowNode endingNode) {
        Collection<FlowNode> flowNodesToDelete = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);
        if (startingNode instanceof StartEvent || endingNode instanceof EndEvent) {
            return;
        }

        if (BpmnFragmentHandler.validateProcessFragment(flowNodesToDelete)) {
            Collection<SequenceFlow> sequenceFlowsIncoming = startingNode.getIncoming();
            Collection<SequenceFlow> sequenceFlowsOutgoing = endingNode.getOutgoing();

            for (SequenceFlow sfi: sequenceFlowsIncoming) {
                for (SequenceFlow sfo: sequenceFlowsOutgoing) {
                    sfi.getSource().builder().connectTo(sfo.getTarget().getId());
                }
            }

            for (FlowNode fn: flowNodesToDelete) {
                BpmnElementRemover.removeFlowNode(this, fn.getId());
            }
        }

        return;

    }

    public void delete(String startingNodeId, String endingNodeId) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        delete(startingNode , endingNode);
        return;
    }

    public void replace(FlowNode existingNode, FlowNode replacingNode) {
        // Check null arguments
        BpmnHelper.checkNotNull(existingNode, "Argument existingNode must not be null");
        BpmnHelper.checkNotNull(replacingNode, "Argument replacingNode must not be null");

        // Gateways cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway, "Argument existingNode cannot be a gateway");

        // A start event can only be replaced by another start event and vice-versa
        BpmnHelper.checkInvalidArgument(!(existingNode instanceof StartEvent ^ replacingNode instanceof StartEvent),
                "One of the nodes is a StartEvent and the other is not");

        // An end event can only be replaced by another end event and vice-versa
        BpmnHelper.checkInvalidArgument(!(existingNode instanceof StartEvent ^ replacingNode instanceof StartEvent),
                "One of the nodes is a StartEvent and the other is not");

        // Make sure that the replacing node has no other nodes connected to it
        BpmnElementRemover.isolateFlowNode(replacingNode);

        int numberPreviousNodes = existingNode.getPreviousNodes().count();
        int numberSuccedingNodes = existingNode.getSucceedingNodes().count();
        FlowNode previousNode = null;
        FlowNode succedingNode = null;

        if (numberPreviousNodes > 0) {
            previousNode = existingNode.getPreviousNodes().singleResult();
        }

        if (numberSuccedingNodes > 0) {
            succedingNode = existingNode.getSucceedingNodes().singleResult();
        }

        // Replacing a starting node
        if (numberPreviousNodes == 0) {
            BpmnElementCreator.appendTo(existingNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(createdReplacingNode, succedingNode);
        }
        // Replacing an ending node
        else if (numberSuccedingNodes == 0) {
            BpmnElementCreator.appendTo(previousNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(previousNode, createdReplacingNode);
        }

        else {
            BpmnElementCreator.appendTo(previousNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(createdReplacingNode, succedingNode);
        }
        BpmnElementRemover.removeFlowNode(this, existingNode.getId());

        return;
    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        replace(existingNode, replacingNode);
        return;
    }

    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(existingNode, "Argument existingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        // Gateways cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway, "Argument existingNode cannot be a gateway");

        // Gateways, start events or end events cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway || existingNode instanceof StartEvent || existingNode instanceof EndEvent,
                "Gateways, StartEvent or EndEvent existingNode cannot be replaced");

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = existingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = existingNode.getSucceedingNodes().singleResult();

        BpmnElementRemover.removeFlowNode(replacingFragment, startEvent.getId());
        BpmnElementRemover.removeFlowNode(replacingFragment, endEvent.getId());

        BpmnElementCreator.appendTo(previousNode, firstNode);
        FlowNode createdLastNode = getModelElementById(lastNode.getId());
        BpmnElementCreator.appendTo(createdLastNode, succeedingNode);

        BpmnElementRemover.removeFlowNode(this, existingNode.getId());

        return;
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        if (existingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + existingNodeId +  "\' not found");
        }
        replace(existingNode, replacingFragment);
        return;
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        // Check null arguments
        BpmnHelper.checkNotNull(startingNode, "Argument startingNode must not be null");
        BpmnHelper.checkNotNull(endingNode, "Argument endingNode must not be null");
        BpmnHelper.checkNotNull(replacingNode, "Argument replacingNode must not be null");

        // Can't replace fragment with a start or end event
        if (replacingNode instanceof StartEvent || replacingNode instanceof EndEvent) {
            throw new IllegalArgumentException("Argument replacingNode must not be a StartEvent or EndEvent");
        }

        BpmnElementRemover.isolateFlowNode(replacingNode);

        Collection<FlowNode> replacedNodes = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        BpmnElementCreator.appendTo(previousNode, replacingNode);
        FlowNode createdNode = getModelElementById(replacingNode.getId());
        BpmnElementCreator.appendTo(createdNode, succeedingNode);

        for (FlowNode fn: replacedNodes) {
            BpmnElementRemover.removeFlowNode(this, fn.getId());
        }

        return;
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        replace(startingNode, endingNode, replacingNode);
        return;
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(startingNode, "Argument startingNode must not be null");
        BpmnHelper.checkNotNull(endingNode, "Argument endingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        Collection<FlowNode> replacedNodes = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        BpmnElementRemover.removeFlowNode(replacingFragment, startEvent.getId());
        BpmnElementRemover.removeFlowNode(replacingFragment, endEvent.getId());

        BpmnElementCreator.appendTo(previousNode, firstNode);
        FlowNode createdLastNode = getModelElementById(lastNode.getId());
        BpmnElementCreator.appendTo(createdLastNode, succeedingNode);

        for (FlowNode fn: replacedNodes) {
            BpmnElementRemover.removeFlowNode(this, fn.getId());
        }

        return;

    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new ElementNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        replace(startingNode, endingNode, replacingFragment);
        return;
    }

    public void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {

        // Check null arguments
        BpmnHelper.checkNotNull(targetNode, "Argument targetNode must not be null");

        // New position cannot be defined if both position arguments are the same
        BpmnHelper.checkInvalidArgument(newPositionAfterOf == newPositionBeforeOf,
                                        "Arguments newPositionAfterOf and newPositionBeforeOf cannot be the same node");

        // Target node cannot be start or end events, or gateways
        BpmnHelper.checkInvalidArgument(targetNode instanceof StartEvent, "Argument targetNode must not be a start event");
        BpmnHelper.checkInvalidArgument(targetNode instanceof EndEvent, "Argument targetNode must not be an end event");
        BpmnHelper.checkInvalidArgument(targetNode instanceof Gateway, "Argument targetNode must not be a gateway");

        FlowNode previousNode = targetNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetNode.getSucceedingNodes().singleResult();

        // Only newPositionAfterOf set
        if (newPositionBeforeOf == null) {
            BpmnHelper.checkInvalidArgument(newPositionAfterOf instanceof EndEvent,
                    "Argument newPositionAfterOf must not be an end event if newPositionBeforeOf not set");

            if (newPositionAfterOf instanceof Gateway) {
                BpmnHelper.checkInvalidArgument(
                        BpmnHelper.isGatewayDivergent((Gateway) newPositionAfterOf),
                        "Argument newPositionAfterOf cannot be a divergent gateway if newPositionBeforeOf not set"
                );
            }

            newPositionBeforeOf = newPositionAfterOf.getSucceedingNodes().singleResult();
        }

        else if (newPositionAfterOf == null) {

            BpmnHelper.checkInvalidArgument(newPositionBeforeOf instanceof StartEvent,
                    "Argument newPositionBeforeOf must not be a start event if newPositionAfterOf not set");

            if (newPositionBeforeOf instanceof Gateway) {
                BpmnHelper.checkInvalidArgument(
                        BpmnHelper.isGatewayConvergent((Gateway) newPositionBeforeOf),
                        "Argument newPositionBeforeOf cannot be a convergent gateway if newPositionBeforeOf not set"
                );
            }
            newPositionAfterOf = newPositionBeforeOf.getPreviousNodes().singleResult();
        }

        boolean nodesInSuccession = false;

        for (SequenceFlow sf: newPositionAfterOf.getOutgoing()) {
            if (sf.getTarget().equals(newPositionBeforeOf)) {
                BpmnElementRemover.removeSequenceFlow(this, sf);
                nodesInSuccession = true;
                break;
            }
        }

        if (!nodesInSuccession) {
            BpmnHelper.checkInvalidArgument(true, "newPositionAfterOf must be directly connected to newPositionBeforeOf");
        }

        // Disconnect the target node
        BpmnElementRemover.isolateFlowNode(targetNode);

        // Connect the previous node to the succeeding node at the old position
        previousNode.builder().connectTo(succeedingNode.getId());

        // Place the target node in the new position
        newPositionAfterOf.builder().connectTo(targetNode.getId()).connectTo(newPositionBeforeOf.getId());
    }

    public void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {

        FlowNode targetNode = getModelElementById(targetNodeId);

        BpmnHelper.checkNotNull(targetNode, "targetNode not found");

        FlowNode newPositionAfterOf = getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = getModelElementById(newPositionBeforeOfId);

        BpmnHelper.checkInvalidArgument(newPositionAfterOf == newPositionBeforeOf, "New position not set");

        move(targetNode, newPositionAfterOf, newPositionBeforeOf);
    }

    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode,
                     FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        if (targetStartingNode.equals(targetEndingNode)) {
            move(targetStartingNode, newPositionAfterOf, newPositionBeforeOf);
            return;
        }

        // New position cannot be defined if both position arguments are the same
        BpmnHelper.checkInvalidArgument(newPositionAfterOf == newPositionBeforeOf,
                "Arguments newPositionAfterOf and newPositionBeforeOf cannot be the same node");

        // Target nodes cannot be start or end events
        BpmnHelper.checkInvalidArgument(targetStartingNode instanceof StartEvent,
                                        "Argument targetStartingNode must not be a start event");
        BpmnHelper.checkInvalidArgument(targetStartingNode instanceof EndEvent,
                                        "Argument targetStartingNode must not be an end event");
        BpmnHelper.checkInvalidArgument(targetEndingNode instanceof StartEvent,
                                        "Argument targetEndingNode must not be a start event");
        BpmnHelper.checkInvalidArgument(targetEndingNode instanceof EndEvent,
                                        "Argument targetEndingNode must not be an end event");

        // TODO: FINISH (targetStartingNode can't be converging gateway & targetEndingNode can't be diverging gateway)

//        FlowNode previousNode = targetStartingNode.getPreviousNodes().singleResult();
//        FlowNode succeedingNode = targetEndingNode.getSucceedingNodes().singleResult();
//
//        // Only newPositionAfterOf set
//        if (newPositionBeforeOf == null) {
//            BpmnHelper.checkInvalidArgument(newPositionAfterOf instanceof EndEvent,
//                    "Argument newPositionAfterOf must not be an end event if newPositionBeforeOf not set");
//
//            if (newPositionAfterOf instanceof Gateway) {
//                BpmnHelper.checkInvalidArgument(
//                        BpmnHelper.isGatewayDivergent((Gateway) newPositionAfterOf),
//                        "Argument newPositionAfterOf cannot be a divergent gateway if newPositionBeforeOf not set"
//                );
//            }
//
//            newPositionBeforeOf = newPositionAfterOf.getSucceedingNodes().singleResult();
//        }
//
//        else if (newPositionAfterOf == null) {
//
//            BpmnHelper.checkInvalidArgument(newPositionBeforeOf instanceof StartEvent,
//                    "Argument newPositionBeforeOf must not be a start event if newPositionAfterOf not set");
//
//            if (newPositionBeforeOf instanceof Gateway) {
//                BpmnHelper.checkInvalidArgument(
//                        BpmnHelper.isGatewayConvergent((Gateway) newPositionBeforeOf),
//                        "Argument newPositionBeforeOf cannot be a convergent gateway if newPositionBeforeOf not set"
//                );
//            }
//            newPositionAfterOf = newPositionBeforeOf.getPreviousNodes().singleResult();
//        }
//
//        boolean nodesInSuccession = false;
//
//        for (SequenceFlow sf: newPositionAfterOf.getOutgoing()) {
//            if (sf.getTarget().equals(newPositionBeforeOf)) {
//                BpmnElementRemover.removeSequenceFlow(this, sf);
//                nodesInSuccession = true;
//                break;
//            }
//        }
//
//        if (!nodesInSuccession) {
//            BpmnHelper.checkInvalidArgument(true, "newPositionAfterOf must be directly connected to newPositionBeforeOf");
//        }
//
//        // Disconnect the target node
//        BpmnElementRemover.isolateFlowNode(targetNode);
//
//        // Connect the previous node to the succeeding node at the old position
//        previousNode.builder().connectTo(succeedingNode.getId());
//
//        // Place the target node in the new position
//        newPositionAfterOf.builder().connectTo(targetNode.getId()).connectTo(newPositionBeforeOf.getId());
    }


    public void move(String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {}

    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) throws Exception {
        if (targetStartingNode == targetEndingNode) {
            throw new Exception("Unable to parallelize a single node");
        }

        Collection<FlowNode> fragment = BpmnFragmentHandler.mapProcessFragment(targetStartingNode, targetEndingNode);

        for (FlowNode fn: fragment) {
            if (fn instanceof StartEvent) {
                throw new Exception("Fragment to parallelize cannot contain start events");
            }
            if (fn instanceof EndEvent) {
                throw new Exception("Fragment to parallelize cannot contain end events");
            }
            if (fn instanceof Gateway) {
                throw new Exception("Fragment to parallelize cannot contain gateways");
            }
        }

        FlowNode firstNode = targetStartingNode.getPreviousNodes().singleResult();
        FlowNode lastNode = targetEndingNode.getSucceedingNodes().singleResult();

        BpmnElementRemover.removeAllSequenceFlows(this, targetStartingNode.getIncoming());

        for (FlowNode fn: fragment) {
            BpmnElementRemover.removeAllSequenceFlows(this, fn.getOutgoing());
        }

        firstNode.builder()
                .parallelGateway()
                    .connectTo(targetStartingNode.getId())
                .parallelGateway()
                    .connectTo(lastNode.getId());

        FlowNode divergentGateway = targetStartingNode.getPreviousNodes().singleResult();
        FlowNode convergentGateway = targetStartingNode.getSucceedingNodes().singleResult();

        for (FlowNode fn: fragment) {
            if (fn != targetStartingNode) {
                divergentGateway.builder().connectTo(fn.getId()).connectTo(convergentGateway.getId());
            }
        }




    }

    public void parallelize(String targetStartingNodeId, String targetEndingNodeId) throws Exception {
        FlowNode targetStartingNode = getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = getModelElementById(targetEndingNodeId);

        if (targetStartingNode == null || targetEndingNode == null) {
            throw new Exception("Flow node not found");
        }

        parallelize(targetStartingNode, targetEndingNode);
    }

    public void split(Task targetTask, BpmnModelInstance newSubProcessModel){
        BpmnHelper.checkNotNull(targetTask, "Argument targetTask must not be null");
        BpmnHelper.checkNotNull(newSubProcessModel, "Argument newSubProcessModel must not be null");

        StartEvent sourceStartEvent = BpmnElementSearcher.findStartEvent(newSubProcessModel);
        EndEvent sourceEndEvent = BpmnElementSearcher.findEndEvent(newSubProcessModel);

        FlowNode previousNode = targetTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetTask.getSucceedingNodes().singleResult();

        String targetTaskId = targetTask.getId();
        String targetTaskName = targetTask.getName();

        Process newSubProcess = BpmnElementSearcher.findFirstProcess(this);
        delete(targetTask);
        previousNode.builder().subProcess(targetTaskId).name(targetTaskName);
        SubProcess createdSubProcess = getModelElementById(targetTaskId);
        createdSubProcess.builder().connectTo(succeedingNode.getId());

        BpmnElementCreator.populateSubProcess(createdSubProcess, sourceStartEvent);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        // Check null arguments
        BpmnHelper.checkNotNull(flowNodeToInsert, "Argument flowNodeToInsert must not be null");
        if (afterOf == null && beforeOf == null) {
            throw new IllegalArgumentException("Arguments afterOf and beforeOf must not both be null");
        }

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf)) {
            throw new IllegalArgumentException("Argument afterOf must not be a diverging gateway");
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf)) {
            throw new IllegalArgumentException("Argument beforeOf must not be a converging gateway");
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            throw new IllegalArgumentException("Argument afterOf must not be the same as beforeOf");
        }

        BpmnElementRemover.isolateFlowNode(flowNodeToInsert);

        // Insert node in series before "beforeOf" node
        if (afterOf == null) {
            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

            BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, previousNode.getId(), beforeOf.getId());
            return;
        }

        // Insert node in series after "afterOf" node
        else if (beforeOf == null) {
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
            BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, afterOf.getId(), succeedingNode.getId());
            return;
        }

        else {
            // Insert in series
            if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {
                BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, afterOf.getId(), beforeOf.getId());
                return;
            }

            // Insert in parallel

            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
            SequenceFlow succeedingFlow = afterOf.getOutgoing().iterator().next();

            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
            SequenceFlow previousFlow = beforeOf.getIncoming().iterator().next();

            suppress(succeedingFlow);
            suppress(previousFlow);

            afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
            previousNode.builder().parallelGateway().connectTo(beforeOf.getId());

            BpmnElementCreator.appendTo(afterOf.getSucceedingNodes().singleResult(), flowNodeToInsert);
            FlowNode createdFlowNode = getModelElementById(flowNodeToInsert.getId());
            BpmnElementCreator.appendTo(createdFlowNode, beforeOf.getPreviousNodes().singleResult());
        }

        return;
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        // Check null arguments
        BpmnHelper.checkNotNull(fragmentToInsert, "Argument fragmentToInsert must not be null");
        if (afterOf == null && beforeOf == null) {
            throw new IllegalArgumentException("Arguments afterOf and beforeOf must not both be null");
        }

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf)) {
            throw new IllegalArgumentException("Argument afterOf must not be a diverging gateway");
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf)) {
            throw new IllegalArgumentException("Argument beforeOf must not be a converging gateway");
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            throw new IllegalArgumentException("Argument afterOf must not be the same as beforeOf");
        }

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        BpmnElementRemover.removeFlowNode((BpmntModelInstance) fragmentToInsert.getModelInstance(), startEvent.getId());
        BpmnElementRemover.removeFlowNode((BpmntModelInstance) fragmentToInsert.getModelInstance(), endEvent.getId());

        // Insert node in series before "beforeOf" node
        if (afterOf == null) {
            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

            suppress(previousNode.getOutgoing().iterator().next());

            BpmnElementCreator.appendTo(previousNode, firstNodeToInsert);
            FlowNode lastInsertedNode = getModelElementById(lastNodeToInsertId);
            BpmnElementCreator.appendTo(lastInsertedNode, beforeOf);

            return;
        }

        // Insert node in series after "afterOf" node
        else if (beforeOf == null) {
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();

            suppress(afterOf.getOutgoing().iterator().next());

            BpmnElementCreator.appendTo(afterOf, firstNodeToInsert);
            FlowNode lastInsertedNode = getModelElementById(lastNodeToInsertId);
            BpmnElementCreator.appendTo(lastInsertedNode, succeedingNode);

            return;
        }

        else {
            // Insert in series
            if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {
                suppress(afterOf.getOutgoing().iterator().next());

                BpmnElementCreator.appendTo(afterOf, firstNodeToInsert);
                FlowNode lastInsertedNode = getModelElementById(lastNodeToInsertId);
                BpmnElementCreator.appendTo(lastInsertedNode, beforeOf);

                return;
            }

            // Insert in parallel

            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
            SequenceFlow succeedingFlow = afterOf.getOutgoing().iterator().next();

            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
            SequenceFlow previousFlow = beforeOf.getIncoming().iterator().next();

            suppress(succeedingFlow);
            suppress(previousFlow);

            afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
            previousNode.builder().parallelGateway().connectTo(beforeOf.getId());

            BpmnElementCreator.appendTo(afterOf.getSucceedingNodes().singleResult(), firstNodeToInsert);
            FlowNode lastInsertedFlowNode = getModelElementById(lastNodeToInsertId);
            BpmnElementCreator.appendTo(lastInsertedFlowNode, beforeOf.getPreviousNodes().singleResult());
        }

        return;

    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
        return;
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        // Check null arguments
        BpmnHelper.checkNotNull(flowNodeToInsert, "Argument flowNodeToInsert must not be null");
        BpmnHelper.checkNotNull(afterOf, "Arguments afterOf must not be null");
        BpmnHelper.checkNotNull(beforeOf, "Arguments beforeOf must not be null");

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf)) {
            throw new IllegalArgumentException("Argument afterOf must not be a diverging gateway");
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf)) {
            throw new IllegalArgumentException("Argument beforeOf must not be a converging gateway");
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            throw new IllegalArgumentException("Argument afterOf must not be the same as beforeOf");
        }

        BpmnElementRemover.isolateFlowNode(flowNodeToInsert);


        // Insert in series (optional node)
        if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {

            BpmnElementRemover.removeAllSequenceFlows(this, afterOf.getOutgoing());

            afterOf.builder().parallelGateway();
            ParallelGateway conditionalGateway = (ParallelGateway) afterOf.getSucceedingNodes().singleResult();

            BpmnElementCreator.conditionalAppendTo(conditionalGateway, flowNodeToInsert, null, condition);
            FlowNode createdFlowNode = getModelElementById(flowNodeToInsert.getId());
            createdFlowNode.builder().parallelGateway().connectTo(beforeOf.getId());

            FlowNode convergentGateway = createdFlowNode.getSucceedingNodes().singleResult();
            conditionalGateway.builder().connectTo(convergentGateway.getId());

            return;
        }

        // Insert in parallel

        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();

        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

        BpmnElementRemover.removeAllSequenceFlows(this, afterOf.getOutgoing());
        BpmnElementRemover.removeAllSequenceFlows(this, beforeOf.getIncoming());

        afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
        previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
        FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();

        BpmnElementCreator.conditionalAppendTo(conditionalGateway, flowNodeToInsert, null, condition);
        FlowNode createdFlowNode = getModelElementById(flowNodeToInsert.getId());
        BpmnElementCreator.appendTo(createdFlowNode, beforeOf.getPreviousNodes().singleResult());

    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
        // Check null arguments
        BpmnHelper.checkNotNull(fragmentToInsert, "Argument fragmentToInsert must not be null");
        BpmnHelper.checkNotNull(afterOf, "Arguments afterOf must not be null");
        BpmnHelper.checkNotNull(beforeOf, "Arguments beforeOf must not be null");

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf)) {
            throw new IllegalArgumentException("Argument afterOf must not be a diverging gateway");
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf)) {
            throw new IllegalArgumentException("Argument beforeOf must not be a converging gateway");
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            throw new IllegalArgumentException("Argument afterOf must not be the same as beforeOf");
        }

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        BpmnElementRemover.removeFlowNode((BpmntModelInstance) fragmentToInsert.getModelInstance(), startEvent.getId());
        BpmnElementRemover.removeFlowNode((BpmntModelInstance) fragmentToInsert.getModelInstance(), endEvent.getId());


        // Insert in series (optional node)
        if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {

            BpmnElementRemover.removeAllSequenceFlows(this, afterOf.getOutgoing());

            afterOf.builder().parallelGateway().parallelGateway().connectTo(beforeOf.getId());
            FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();
            FlowNode convergentGateway = beforeOf.getPreviousNodes().singleResult();

            BpmnElementCreator.conditionalAppendTo(conditionalGateway, firstNodeToInsert, null, condition);

            FlowNode firstCreatedFlowNode = getModelElementById(firstNodeToInsert.getId());

            for (FlowNode fn: firstNodeToInsert.getSucceedingNodes().list()) {
                BpmnElementCreator.appendTo(firstCreatedFlowNode, fn);
            }

            FlowNode lastCreatedFlowNode = getModelElementById(lastNodeToInsertId);

            lastCreatedFlowNode.builder().connectTo(convergentGateway.getId());

            return;
        }

        // Insert in parallel

        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

        BpmnElementRemover.removeAllSequenceFlows(this, afterOf.getOutgoing());
        BpmnElementRemover.removeAllSequenceFlows(this, beforeOf.getIncoming());

        afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
        previousNode.builder().parallelGateway().connectTo(beforeOf.getId());

        FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();
        FlowNode convergentGateway = beforeOf.getPreviousNodes().singleResult();

        BpmnElementCreator.conditionalAppendTo(conditionalGateway, firstNodeToInsert, null, condition);

        FlowNode firstCreatedFlowNode = getModelElementById(firstNodeToInsert.getId());

        for (FlowNode fn: firstNodeToInsert.getSucceedingNodes().list()) {
            BpmnElementCreator.appendTo(firstCreatedFlowNode, fn);
        }

        FlowNode lastCreatedFlowNode = getModelElementById(lastNodeToInsertId);

        lastCreatedFlowNode.builder().connectTo(convergentGateway.getId());

        return;


    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(
                afterOf,
                beforeOf,
                fragmentToInsert.getModelElementsByType(Process.class).iterator().next(),
                condition,
                inLoop
        );
    }

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

}