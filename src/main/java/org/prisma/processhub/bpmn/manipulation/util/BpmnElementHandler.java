package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


public final class BpmnElementHandler {

    private BpmnElementHandler() {}


    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Add a new single process element to the given parent element
    public static <T extends FlowElement, E extends ModelElementInstance> T contribute(BpmnModelInstance modelInstance, E parentElement, T element) {
        // Verify that parent element is part of modelInstance model
        BpmnHelper.checkElementPresent(modelInstance.equals(parentElement.getModelInstance()),
                "parentElement is not part of modelInstance BpmnModelInstance");

        // Verify that element id doesn't conflict with another one in the model
        if (modelInstance.getModelElementById(element.getId()) != null) {
            throw new IllegalArgumentException("There is another element with id \'" + element.getId() + "\' in modelInstance BpmnModelInstance");
        }

        // Create new FlowElement in model with same properties as element parameter
        T newElement = (T) modelInstance.newInstance(element.getElementType());
        newElement.setId(element.getId());
        newElement.setName(element.getName());
        parentElement.addChildElement(newElement);
        return modelInstance.getModelElementById(newElement.getId());
    }

    // Add a new single element to the first process in modelInstance model as parent
    public static <T extends FlowElement> T contribute(BpmnModelInstance modelInstance, T element) {
        return contribute(modelInstance, BpmnElementSearcher.findFirstProcess(modelInstance), element);
    }

    // Add a new single element to the first process in modelInstance model as parent
    public static <T extends FlowElement> T contribute(BpmnModelInstance modelInstance, String parentElementId, T element) {
        // Verify parent element is part of the model
        ModelElementInstance parentElement = modelInstance.getModelElementById(parentElementId);
        BpmnHelper.checkElementPresent(parentElement != null, "FlowElement with id \'" + parentElementId + "\' is not part of given BpmnModelInstance");
        return contribute(modelInstance, parentElement, element);
    }


    // Remove every element in collection
    public static <T extends FlowElement> void suppress(BpmnModelInstance modelInstance, Collection<T> elements) {
        for (FlowElement element : elements) {
            suppress(modelInstance, element);
        }
    }

    // Remove flow element leaving the rest of the model untouched
    public static <T extends FlowElement> void suppress(BpmnModelInstance modelInstance, T element) {
        // Verify if element is part of modelInstance model instance
        BpmnHelper.checkElementPresent(contains(modelInstance, element), "FlowElement with id \'" + element.getId() + "\' is not part of given BpmnModelInstance");
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) element.getParentElement();
        parentElement.removeChildElement(element);
    }

    // Remove flow element by id
    public static void suppress(BpmnModelInstance modelInstance, String elementId) {
        FlowElement targetElement = modelInstance.getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        suppress(modelInstance, targetElement);
    }

    // Modify a property of a flow element
    public static <T extends FlowElement> void modify(T element, String property, String value) {
        element.setAttributeValue(property, value);
    }

    // Modify a property of a flow element with given id
    public static void modify(BpmnModelInstance modelInstance, String elementId, String property, String value) {
        FlowElement targetElement = modelInstance.getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        modify(targetElement, property, value);
    }


    // Verify if an element is contained in given BpmnModelInstance
    public static boolean contains(BpmnModelInstance modelInstance, FlowElement element) {
        return modelInstance.getModelElementById(element.getId()) != null;
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public static void rename(BpmnModelInstance modelInstance, FlowElement element, String newName) {
        BpmnHelper.checkElementPresent(contains(modelInstance, element),
                "FlowElement with id \'" + element.getId() + "\' is not part of given BpmnModelInstance");
        element.setName(newName);
    }

    // Rename element by id
    public static void rename(BpmnModelInstance modelInstance, String elementId, String newName) {
        FlowElement element = modelInstance.getModelElementById(elementId);
        BpmnHelper.checkElementPresent(element != null, "Flow Element with id \'" + elementId + "\' not found");
        element.setName(newName);
    }


    // Delete a node, all sequence flows connected to it and also obsolete gateways
    public static void delete(BpmnModelInstance modelInstance, FlowNode node){
        // Gateways, start and end events are not allowed to be deleted
        BpmnHelper.checkInvalidArgument(node instanceof Gateway || node instanceof StartEvent || node instanceof EndEvent,
                "Argument FlowNode must not be a Gateway, StartEvent or EndEvent");

        fixGatewaysDelete(modelInstance, node.getPreviousNodes().list(), node.getSucceedingNodes().list());

        // Remove flow node and all sequence flows connected to it
        connectAllPreviousToSucceedingNodes(node);
        removeNodeAndSequenceFlows(modelInstance, node);

        // Verify model consistency with Camunda API
        Bpmn.validateModel(modelInstance);
    }

    private static void fixGatewaysDelete(BpmnModelInstance modelInstance, Collection<FlowNode> previousNodes, Collection<FlowNode> succeedingNodes) {
        Collection<FlowNode> gatewaysToDelete = new ArrayList<FlowNode>();

        // Booleans to tell if node is between two gateways
        // which is the only case where gateways should be deleted
        boolean nodeBetweenGateways = false;
        boolean gatewayBefore = false;

        // Get all nodes and gateways to be possibly deleted before the node
        for (FlowNode source : previousNodes) {
            // Identify gateways and check if they should be deleted
            if (source instanceof Gateway) {
                gatewayBefore = true;
                Gateway gateway = (Gateway) source;
                // Gateway before should be deleted if it's divergent and has 2 outgoing sequence flows
                if (BpmnHelper.isGatewayDivergent(gateway) && gateway.getOutgoing().size() == 2) {
                    gatewaysToDelete.add(gateway);
                }
            }
        }
        // Only check for succeeding gateways to be deleted if there's a previous gateway for that node
        if (gatewayBefore) {
            // Get all nodes and gateways to be deleted after the node
            for (FlowNode destination : succeedingNodes) {
                // Identify gateways and check if they should be deleted
                if (destination instanceof Gateway) {
                    nodeBetweenGateways = true;
                    Gateway gateway = (Gateway) destination;
                    // Gateway after should be deleted if it's convergent and has 2 incoming sequence flows
                    if (BpmnHelper.isGatewayConvergent(gateway) && gateway.getIncoming().size() == 2) {
                        gatewaysToDelete.add(gateway);
                    }
                }
            }
        }
        // Delete obsolete gateways if node is between gateways
        // and connect the element before with the element after it
        if (nodeBetweenGateways) {
            for (FlowNode gateway : gatewaysToDelete) {
                connectAllPreviousToSucceedingNodes(gateway);
                // Delete gateway
                removeNodeAndSequenceFlows(modelInstance, gateway);
                // Remove from previous and succeeding nodes list if its present
                succeedingNodes.remove(gateway);
                previousNodes.remove(gateway);
            }
        }
    }


    // Delete element by id
    public static void delete(BpmnModelInstance modelInstance, String nodeId) {
        FlowNode node = modelInstance.getModelElementById(nodeId);
        BpmnHelper.checkElementPresent(node != null, "Flow Node with id \'" + nodeId + "\' not found");
        delete(modelInstance, node);
    }

    // Delete range of elements from startingNode to endingNode
    public static void delete(BpmnModelInstance modelInstance, FlowNode startingNode, FlowNode endingNode) {
        Collection<FlowNode> nodesToDelete = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        // Make sure the fragment can be deleted
        BpmnFragmentHandler.validateDeleteProcessFragment(nodesToDelete);

        // Connect nodes before with nodes after fragment
        connectAllPreviousToSucceedingNodes(startingNode, endingNode);

        // Delete all nodes in the fragment
        for (FlowNode node: nodesToDelete) {
            removeNodeAndSequenceFlows(modelInstance, node);
        }
    }

    public static void delete(BpmnModelInstance modelInstance, String startingNodeId, String endingNodeId) {
        FlowNode startingNode = modelInstance.getModelElementById(startingNodeId);
        FlowNode endingNode = modelInstance.getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        delete(modelInstance, startingNode , endingNode);
    }


    public static void replace(BpmnModelInstance modelInstance, FlowNode existingNode, FlowNode replacingNode) {
        // Check null arguments
        BpmnHelper.checkNotNull(existingNode, "Argument existingNode must not be null");
        BpmnHelper.checkNotNull(replacingNode, "Argument replacingNode must not be null");

        // Gateways cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway, "Argument existingNode cannot be a gateway");

        // A start event can only be replaced by another start event and vice-versa
        BpmnHelper.checkInvalidArgument((existingNode instanceof StartEvent ^ replacingNode instanceof StartEvent),
                "A StartEvent may only be replaced by another StartEvent");

        // An end event can only be replaced by another end event and vice-versa
        BpmnHelper.checkInvalidArgument((existingNode instanceof EndEvent ^ replacingNode instanceof EndEvent),
                "An EndEvent may only be replaced by another EndEvent");

        // Add flow element to the modelInstance
        FlowNode createdReplacingNode = contribute(modelInstance, existingNode.getParentElement(), replacingNode);

        // Get previous and succeeding nodes
        int numberPreviousNodes = existingNode.getPreviousNodes().count();
        int numberSucceedingNodes = existingNode.getSucceedingNodes().count();

        if (numberPreviousNodes > 0) {
            FlowNode previousNode = existingNode.getPreviousNodes().singleResult();
            appendTo(modelInstance, previousNode, createdReplacingNode);
        }
        if (numberSucceedingNodes > 0) {
            FlowNode succeedingNode = existingNode.getSucceedingNodes().singleResult();
            appendTo(modelInstance, createdReplacingNode, succeedingNode);
        }
        removeNodeAndSequenceFlows(modelInstance, existingNode);

    }

    public static void replace(BpmnModelInstance modelInstance, String existingNodeId, FlowNode replacingNode) {
        FlowNode existingNode = modelInstance.getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(modelInstance, existingNode, replacingNode);
    }

    public static void replace(BpmnModelInstance modelInstance, FlowNode existingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(existingNode, "Argument existingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        // Gateways, start events or end events cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway || existingNode instanceof StartEvent || existingNode instanceof EndEvent,
                "Gateways, StartEvent or EndEvent existingNode cannot be replaced");

        // Copy the model to avoid changing the original fragment
        replacingFragment = copyModelInstance(replacingFragment);

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = existingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = existingNode.getSucceedingNodes().singleResult();

        // Delete start and end events as well as all incoming and outgoing sequence flows
        removeNodeAndSequenceFlows(replacingFragment, startEvent);
        removeNodeAndSequenceFlows(replacingFragment, endEvent);

        appendTo(modelInstance, previousNode, firstNode);
        FlowNode createdLastNode = modelInstance.getModelElementById(lastNode.getId());
        appendTo(modelInstance, createdLastNode, succeedingNode);

        removeNodeAndSequenceFlows(modelInstance, existingNode);
    }

    public static void replace(BpmnModelInstance modelInstance, String existingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode existingNode = modelInstance.getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(modelInstance, existingNode, replacingFragment);
    }

    public static void replace(BpmnModelInstance modelInstance, FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        // Check null arguments
        BpmnHelper.checkNotNull(startingNode, "Argument startingNode must not be null");
        BpmnHelper.checkNotNull(endingNode, "Argument endingNode must not be null");
        BpmnHelper.checkNotNull(replacingNode, "Argument replacingNode must not be null");

        // Can't replace fragment with a start or end event
        if (replacingNode instanceof StartEvent || replacingNode instanceof EndEvent) {
            throw new IllegalArgumentException("Argument replacingNode must not be a StartEvent or EndEvent");
        }

        // Use contribute(modelInstance, ) to avoid changing the replacingNode
        //isolateFlowNode(replacingNode);
        contribute(modelInstance, startingNode.getParentElement(), replacingNode);

        Collection<FlowNode> replacedNodes = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        // Use contribute() to avoid changing the replacingNode
        replacingNode = modelInstance.getModelElementById(replacingNode.getId());

        appendTo(modelInstance, previousNode, replacingNode);
        appendTo(modelInstance, replacingNode, succeedingNode);
        for (FlowNode replacedNode: replacedNodes) {
            delete(modelInstance, replacedNode.getId());
        }

    }

    public static void replace(BpmnModelInstance modelInstance, String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        FlowNode startingNode = modelInstance.getModelElementById(startingNodeId);
        FlowNode endingNode = modelInstance.getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(modelInstance, startingNode, endingNode, replacingNode);
    }

    public static void replace(BpmnModelInstance modelInstance, FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(startingNode, "Argument startingNode must not be null");
        BpmnHelper.checkNotNull(endingNode, "Argument endingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        replacingFragment = copyModelInstance(replacingFragment);

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        Collection<FlowNode> replacedNodes = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        // Delete start and end events as well as all incoming and outgoing sequence flows
        removeNodeAndSequenceFlows(replacingFragment, startEvent);
        removeNodeAndSequenceFlows(replacingFragment, endEvent);

        appendTo(modelInstance, previousNode, firstNode);
        FlowNode createdLastNode = modelInstance.getModelElementById(lastNode.getId());
        appendTo(modelInstance, createdLastNode, succeedingNode);

        for (FlowNode replacedNode: replacedNodes) {
            removeNodeAndSequenceFlows(modelInstance, replacedNode);
        }
    }

    public static void replace(BpmnModelInstance modelInstance, String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode startingNode = modelInstance.getModelElementById(startingNodeId);
        FlowNode endingNode = modelInstance.getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(modelInstance, startingNode, endingNode, replacingFragment);
    }

    public static void move(BpmnModelInstance modelInstance, FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        // Check null arguments
        BpmnHelper.checkNotNull(targetNode, "Argument targetNode must not be null");
        BpmnHelper.checkNotNull(modelInstance, "Argument modelInstance must not be null");

        // Check target element is part of model instance
        BpmnHelper.checkElementPresent(contains(modelInstance, targetNode), "Argument targetNode must be part of modelInstance");

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
                suppress(modelInstance, sf);
                nodesInSuccession = true;
                break;
            }
        }

        if (!nodesInSuccession) {
            BpmnHelper.checkInvalidArgument(true, "newPositionAfterOf must be directly connected to newPositionBeforeOf");
        }

        // Disconnect the target node
        suppress(modelInstance, targetNode.getIncoming());
        suppress(modelInstance, targetNode.getOutgoing());

        // Connect the previous node to the succeeding node at the old position
        previousNode.builder().connectTo(succeedingNode.getId());

        // Place the target node in the new position
        newPositionAfterOf.builder().connectTo(targetNode.getId()).connectTo(newPositionBeforeOf.getId());
    }

    public static void move(BpmnModelInstance modelInstance, String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {

        FlowNode targetNode = modelInstance.getModelElementById(targetNodeId);

        BpmnHelper.checkNotNull(targetNode, "targetNode not found");

        FlowNode newPositionAfterOf = modelInstance.getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = modelInstance.getModelElementById(newPositionBeforeOfId);

        BpmnHelper.checkInvalidArgument(newPositionAfterOf == newPositionBeforeOf, "New position not set");

        move(modelInstance, targetNode, newPositionAfterOf, newPositionBeforeOf);
    }

    public static void move(BpmnModelInstance modelInstance, FlowNode targetStartingNode, FlowNode targetEndingNode,
                     FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        if (targetStartingNode.equals(targetEndingNode)) {
            move(modelInstance, targetStartingNode, newPositionAfterOf, newPositionBeforeOf);
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

        // Validates the target fragment
        BpmnFragmentHandler.mapProcessFragment(targetStartingNode, targetEndingNode);

        FlowNode previousNode = targetStartingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetEndingNode.getSucceedingNodes().singleResult();

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
                suppress(modelInstance, sf);
                nodesInSuccession = true;
                break;
            }
        }
        if (!nodesInSuccession) {
            BpmnHelper.checkInvalidArgument(true, "newPositionAfterOf must be directly connected to newPositionBeforeOf");
        }

        // Disconnect the target fragment
        suppress(modelInstance, targetStartingNode.getIncoming());
        suppress(modelInstance, targetEndingNode.getOutgoing());

        // Connect the previous node to the succeeding node at the old position
        previousNode.builder().connectTo(succeedingNode.getId());

        // Place the target node in the new position
        newPositionAfterOf.builder().connectTo(targetStartingNode.getId());
        targetEndingNode.builder().connectTo(newPositionBeforeOf.getId());
    }

    public static void move(BpmnModelInstance modelInstance, String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {

        FlowNode targetStartingNode = modelInstance.getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = modelInstance.getModelElementById(targetEndingNodeId);
        FlowNode newPositionAfterOf = modelInstance.getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = modelInstance.getModelElementById(newPositionBeforeOfId);

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        move(modelInstance, targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);

    }

    public static void parallelize(BpmnModelInstance modelInstance, FlowNode targetStartingNode, FlowNode targetEndingNode) {
        BpmnHelper.checkInvalidArgument(targetStartingNode == targetEndingNode, "Unable to parallelize a single node");

        // Check if target elements are part of the BpmnModelInstance given
        BpmnHelper.checkElementPresent(contains(modelInstance, targetStartingNode),
                                        "Argument targetStartingNode must be part of modelInstance");
        BpmnHelper.checkElementPresent(contains(modelInstance, targetEndingNode),
                "Argument targetEngindNode must be part of modelInstance");
        Collection<FlowNode> fragment = BpmnFragmentHandler.mapProcessFragment(targetStartingNode, targetEndingNode);

        // Check if any node in the fragment is of an invalid type
        for (FlowNode fn: fragment) {
            BpmnHelper.checkInvalidArgument(fn instanceof StartEvent, "Fragment to parallelize cannot contain start events");
            BpmnHelper.checkInvalidArgument(fn instanceof EndEvent, "Fragment to parallelize cannot contain end events");
            BpmnHelper.checkInvalidArgument(fn instanceof Gateway, "\"Fragment to parallelize cannot contain gateways\"");
        }

        FlowNode firstNode = targetStartingNode.getPreviousNodes().singleResult();
        FlowNode lastNode = targetEndingNode.getSucceedingNodes().singleResult();

        suppress(modelInstance, targetStartingNode.getIncoming());

        for (FlowNode fn: fragment) {
            suppress(modelInstance, fn.getOutgoing());
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

    public static void parallelize(BpmnModelInstance modelInstance, String targetStartingNodeId, String targetEndingNodeId) {
        FlowNode targetStartingNode = modelInstance.getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = modelInstance.getModelElementById(targetEndingNodeId);

        // Check if elements are present
        BpmnHelper.checkElementPresent(targetStartingNode != null, "Flow Node with id \'" + targetStartingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(targetEndingNode != null, "Flow Node with id \'" + targetEndingNodeId +  "\' not found");

        parallelize(modelInstance, targetStartingNode, targetEndingNode);
    }

    public static void split(BpmnModelInstance modelInstance, Task targetTask, BpmnModelInstance newSubProcessModel){
        BpmnHelper.checkNotNull(targetTask, "Argument targetTask must not be null");
        BpmnHelper.checkNotNull(newSubProcessModel, "Argument newSubProcessModel must not be null");
        BpmnHelper.checkElementPresent(contains(modelInstance, targetTask), "Argument targetTask must be part of modelInstance");

        StartEvent sourceStartEvent = BpmnElementSearcher.findStartEvent(newSubProcessModel);
        EndEvent sourceEndEvent = BpmnElementSearcher.findEndEvent(newSubProcessModel);

        FlowNode previousNode = targetTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetTask.getSucceedingNodes().singleResult();

        // Get info from target task and delete it
        String targetTaskId = targetTask.getId();
        String targetTaskName = targetTask.getName();
        removeNodeAndSequenceFlows(modelInstance, targetTask);

        newSubProcessModel = copyModelInstance(newSubProcessModel);
        Process newSubProcess = BpmnElementSearcher.findFirstProcess(modelInstance);
        previousNode.builder().subProcess(targetTaskId).name(targetTaskName);
        SubProcess createdSubProcess = modelInstance.getModelElementById(targetTaskId);
        createdSubProcess.builder().connectTo(succeedingNode.getId());

        populateSubProcess(createdSubProcess, sourceStartEvent);
    }

    public static void split(BpmnModelInstance modelInstance, String targetTaskId, BpmnModelInstance newSubProcessModel) {
        Task targetTask = modelInstance.getModelElementById(targetTaskId);
        BpmnHelper.checkElementPresent(targetTask != null, "Flow Element with id \'" + targetTaskId +  "\' not found");
        split(modelInstance, targetTask, newSubProcessModel);
    }

    public static void insert(BpmnModelInstance modelInstance, String afterOfId, String beforeOfId, FlowNode flowNodeToInsert) {
        // Inserted node can't be a single gateway
        BpmnHelper.checkInvalidArgument(flowNodeToInsert instanceof Gateway, "Argument flowNodeToInsert must not be a gateway");

        // Check null arguments
        FlowNode afterOf = modelInstance.getModelElementById(afterOfId);
        FlowNode beforeOf = modelInstance.getModelElementById(beforeOfId);
        BpmnHelper.checkNotNull(flowNodeToInsert, "Argument flowNodeToInsert must not be null");
        BpmnHelper.checkInvalidArgument(afterOf == null && beforeOf == null,
                "Arguments afterOf and beforeOf must not both be null");

        // beforeOf and afterOf cannot be the same node
        BpmnHelper.checkInvalidArgument(beforeOf == afterOf, "Argument afterOf must not be the same as beforeOf");

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        BpmnHelper.checkInvalidArgument(afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf),
                "Argument afterOf must not be a diverging gateway");
        // beforeNode can't be a converging gateway
        BpmnHelper.checkInvalidArgument(beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf),
                "Argument beforeOf must not be a converging gateway");

        // Analyze parameters to choose a method to call
        if (afterOf == null) {
            insertBefore(modelInstance, beforeOf, flowNodeToInsert);
        } else if (beforeOf == null) {
            insertAfter(modelInstance, afterOf, flowNodeToInsert);
        } else {
            insertBetween(modelInstance, afterOf, beforeOf, flowNodeToInsert);
        }
    }

    public static void insertAfter(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode flowNodeToInsert) {
        // Add flow node to model
        FlowNode flowNodeInserted = contribute(modelInstance, afterOf.getParentElement(), flowNodeToInsert);

        // Connect flow node between afterOf and the element succeeding it
        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
        afterOf.builder().connectTo(flowNodeInserted.getId()).connectTo(succeedingNode.getId());
    }

    public static void insertBefore(BpmnModelInstance modelInstance, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        // Add flow node to model
        FlowNode flowNodeInserted = contribute(modelInstance, beforeOf.getParentElement(), flowNodeToInsert);

        // Connect flow node between beforeOf and the element preceding it
        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
        previousNode.builder().connectTo(flowNodeInserted.getId()).connectTo(beforeOf.getId());
    }

    public static void insertBetween(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        // Insert in series if afterOf precedes beforeOf
        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
        if (succeedingNode.equals(beforeOf)) {
            // Add element and connect it between afterOf and beforeOf
            FlowNode flowNodeInserted = contribute(modelInstance, afterOf.getParentElement(), flowNodeToInsert);
            afterOf.builder().connectTo(flowNodeInserted.getId()).connectTo(beforeOf.getId());
            return;
        }

        // Insert in parallel otherwise
        // Get outgoing sequence flow from afterOf
        SequenceFlow succeedingFlow = afterOf.getOutgoing().iterator().next();

        // Get incoming sequence flow from beforeOf
        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
        SequenceFlow previousFlow = beforeOf.getIncoming().iterator().next();

        if (succeedingNode instanceof ParallelGateway) {
            if (!BpmnHelper.isGatewayDivergent((Gateway) succeedingNode)) {
                suppress(modelInstance, succeedingFlow);
                afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
            }
        }
        else {
            suppress(modelInstance, succeedingFlow);
            afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
        }

        if (previousNode instanceof ParallelGateway) {
            if (!BpmnHelper.isGatewayConvergent((Gateway) previousNode)) {
                suppress(modelInstance, previousFlow);
                previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
            }
        }
        else {
            suppress(modelInstance, previousFlow);
            previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
        }

        // Add flow node to model
        FlowNode flowNodeInserted = contribute(modelInstance, afterOf.getParentElement(), flowNodeToInsert);
        // Get the parallel gateway created after afterOf node and connnect the node to be inserted after it
        FlowNode afterGateway = afterOf.getSucceedingNodes().singleResult();
        afterGateway.builder().connectTo(flowNodeInserted.getId());

        // Get the parallel gateway created before beforeOf node and connect the node to be inserted before it
        FlowNode beforeGateway = beforeOf.getPreviousNodes().singleResult();
        flowNodeInserted.builder().connectTo(beforeGateway.getId());
    }

    public static void insert(BpmnModelInstance modelInstance, String afterOfId, String beforeOfId, BpmnModelInstance fragmentToInsert) {
        // Check null arguments
        FlowNode afterOf = modelInstance.getModelElementById(afterOfId);
        FlowNode beforeOf = modelInstance.getModelElementById(beforeOfId);
        BpmnHelper.checkNotNull(fragmentToInsert, "Argument fragmentToInsert must not be null");
        BpmnHelper.checkInvalidArgument(afterOf == null && beforeOf == null,
                "Arguments afterOf and beforeOf must not both be null");

        // beforeOf and afterOf cannot be the same node
        BpmnHelper.checkInvalidArgument(beforeOf == afterOf, "Argument afterOf must not be the same as beforeOf");

        // Unable to insert a node before a start event or after an end event
        BpmnHelper.checkInvalidArgument(beforeOf instanceof StartEvent, "Argument beforeOf must not be a StartEvent");
        BpmnHelper.checkInvalidArgument(afterOf instanceof EndEvent, "Argument afterOf must not be an EndEvent");

        // afterOf can't be a diverging gateway
        BpmnHelper.checkInvalidArgument(afterOf instanceof Gateway && BpmnHelper.isGatewayDivergent((Gateway) afterOf),
                                        "Argument afterOf must not be a diverging gateway");
        // beforeNode can't be a converging gateway
        BpmnHelper.checkInvalidArgument(beforeOf instanceof Gateway && BpmnHelper.isGatewayConvergent((Gateway) beforeOf),
                                        "Argument beforeOf must not be a converging gateway");

        insert(modelInstance, afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
    }

    public static void insert(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(modelInstance, afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
    }

    public static void insert(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        // Copy fragment to insert
        BpmnModelInstance fragmentModelToInsert = copyModelInstance((BpmnModelInstance) fragmentToInsert.getModelInstance());
        fragmentToInsert = fragmentModelToInsert.getModelElementById(fragmentToInsert.getId());

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        // Delete start and end events as well as all incoming and outgoing sequence flows
        BpmnModelInstance fragmentModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        removeNodeAndSequenceFlows(fragmentModel, startEvent);
        removeNodeAndSequenceFlows(fragmentModel, endEvent);

        // Insert node in series before "beforeOf" node
        if (afterOf == null) {
            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

            suppress(modelInstance, previousNode.getOutgoing().iterator().next());

            appendTo(modelInstance, previousNode, firstNodeToInsert);
            FlowNode lastInsertedNode = modelInstance.getModelElementById(lastNodeToInsertId);
            appendTo(modelInstance, lastInsertedNode, beforeOf);
        }

        // Insert node in series after "afterOf" node
        else if (beforeOf == null) {
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();

            suppress(modelInstance, afterOf.getOutgoing().iterator().next());

            appendTo(modelInstance, afterOf, firstNodeToInsert);
            FlowNode lastInsertedNode = modelInstance.getModelElementById(lastNodeToInsertId);
            appendTo(modelInstance, lastInsertedNode, succeedingNode);
        }

        else {
            // Insert in series
            if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {
                suppress(modelInstance, afterOf.getOutgoing().iterator().next());

                appendTo(modelInstance, afterOf, firstNodeToInsert);
                FlowNode lastInsertedNode = modelInstance.getModelElementById(lastNodeToInsertId);
                appendTo(modelInstance, lastInsertedNode, beforeOf);

                return;
            }

            // Insert in parallel
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
            SequenceFlow succeedingFlow = afterOf.getOutgoing().iterator().next();

            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
            SequenceFlow previousFlow = beforeOf.getIncoming().iterator().next();


            if (succeedingNode instanceof ParallelGateway) {
                if (!BpmnHelper.isGatewayDivergent((Gateway) succeedingNode)) {
                    suppress(modelInstance, succeedingFlow);
                    afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
                }
            }
            else {
                suppress(modelInstance, succeedingFlow);
                afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
            }

            if (previousNode instanceof ParallelGateway) {
                if (!BpmnHelper.isGatewayConvergent((Gateway) previousNode)) {
                    suppress(modelInstance, previousFlow);
                    previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
                }
            }
            else {
                suppress(modelInstance, previousFlow);
                previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
            }

            appendTo(modelInstance, afterOf.getSucceedingNodes().singleResult(), firstNodeToInsert);
            FlowNode lastInsertedFlowNode = modelInstance.getModelElementById(lastNodeToInsertId);
            appendTo(modelInstance, lastInsertedFlowNode, beforeOf.getPreviousNodes().singleResult());
        }
    }

    public static void conditionalInsert(BpmnModelInstance modelInstance, String afterOfId, String beforeOfId, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        FlowNode afterOf = modelInstance.getModelElementById(afterOfId);
        FlowNode beforeOf = modelInstance.getModelElementById(beforeOfId);

        // Check if elements are present
        BpmnHelper.checkElementPresent(afterOf != null, "Flow Node with id \'" + afterOfId + "\' not found");
        BpmnHelper.checkElementPresent(beforeOf != null, "Flow Node with id \'" + beforeOfId +  "\' not found");

        conditionalInsert(modelInstance, afterOf, beforeOf, flowNodeToInsert, condition, inLoop);
    }

    public static void conditionalInsert(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
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

        //isolateFlowNode(flowNodeToInsert);
        flowNodeToInsert = contribute(modelInstance, afterOf.getParentElement(), flowNodeToInsert);

        // Insert in series (optional node)
        if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {

            suppress(modelInstance, afterOf.getOutgoing());

            afterOf.builder().parallelGateway();
            ParallelGateway conditionalGateway = (ParallelGateway) afterOf.getSucceedingNodes().singleResult();

            conditionalAppendTo(conditionalGateway, flowNodeToInsert, null, condition);
            FlowNode createdFlowNode = modelInstance.getModelElementById(flowNodeToInsert.getId());
            createdFlowNode.builder().parallelGateway().connectTo(beforeOf.getId());

            FlowNode convergentGateway = createdFlowNode.getSucceedingNodes().singleResult();
            conditionalGateway.builder().connectTo(convergentGateway.getId());

            return;
        }

        // Insert in parallel

        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();

        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

        suppress(modelInstance, afterOf.getOutgoing());
        suppress(modelInstance, beforeOf.getIncoming());

        afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
        previousNode.builder().parallelGateway().connectTo(beforeOf.getId());
        FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();

        conditionalAppendTo(conditionalGateway, flowNodeToInsert, null, condition);
        FlowNode createdFlowNode = modelInstance.getModelElementById(flowNodeToInsert.getId());
        appendTo(modelInstance, createdFlowNode, beforeOf.getPreviousNodes().singleResult());

    }

    public static void conditionalInsert(BpmnModelInstance modelInstance, String afterOfId, String beforeOfId, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        // Check if elements are present
        FlowNode afterOf = modelInstance.getModelElementById(afterOfId);
        FlowNode beforeOf = modelInstance.getModelElementById(beforeOfId);
        BpmnHelper.checkElementPresent(afterOf != null, "Flow Node with id \'" + afterOfId + "\' not found");
        BpmnHelper.checkElementPresent(beforeOf != null, "Flow Node with id \'" + beforeOfId +  "\' not found");

        conditionalInsert(modelInstance, afterOf, beforeOf, fragmentToInsert, condition, inLoop);
    }

    public static void conditionalInsert(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
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

        BpmnModelInstance fragmentModelToInsert = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsert = fragmentModelToInsert.getModelElementById(fragmentToInsert.getId());

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        // Delete start and end events as well as all incoming and outgoing sequence flows
        BpmnModelInstance fragmentModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        removeNodeAndSequenceFlows(fragmentModelToInsert, startEvent);
        removeNodeAndSequenceFlows(fragmentModelToInsert, endEvent);

        // Insert in series (optional node)
        if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {

            suppress(modelInstance, afterOf.getOutgoing());

            afterOf.builder().parallelGateway().parallelGateway().connectTo(beforeOf.getId());
            FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();
            FlowNode convergentGateway = beforeOf.getPreviousNodes().singleResult();

            conditionalAppendTo(conditionalGateway, firstNodeToInsert, null, condition);

            FlowNode firstCreatedFlowNode = modelInstance.getModelElementById(firstNodeToInsert.getId());

            for (FlowNode fn: firstNodeToInsert.getSucceedingNodes().list()) {
                appendTo(modelInstance, firstCreatedFlowNode, fn);
            }

            FlowNode lastCreatedFlowNode = modelInstance.getModelElementById(lastNodeToInsertId);

            lastCreatedFlowNode.builder().connectTo(convergentGateway.getId());

            return;
        }

        // Insert in parallel

        FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
        FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

        suppress(modelInstance, afterOf.getOutgoing());
        suppress(modelInstance, beforeOf.getIncoming());

        afterOf.builder().parallelGateway().connectTo(succeedingNode.getId());
        previousNode.builder().parallelGateway().connectTo(beforeOf.getId());

        FlowNode conditionalGateway = afterOf.getSucceedingNodes().singleResult();
        FlowNode convergentGateway = beforeOf.getPreviousNodes().singleResult();

        conditionalAppendTo(conditionalGateway, firstNodeToInsert, null, condition);

        FlowNode firstCreatedFlowNode = modelInstance.getModelElementById(firstNodeToInsert.getId());

        for (FlowNode fn: firstNodeToInsert.getSucceedingNodes().list()) {
            appendTo(modelInstance, firstCreatedFlowNode, fn);
        }

        FlowNode lastCreatedFlowNode = modelInstance.getModelElementById(lastNodeToInsertId);

        lastCreatedFlowNode.builder().connectTo(convergentGateway.getId());
    }

    public static void conditionalInsert(BpmnModelInstance modelInstance, FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(
                modelInstance,
                afterOf,
                beforeOf,
                BpmnElementSearcher.findFirstProcess(fragmentToInsert),
                condition,
                inLoop
        );
    }


    // Useful operations that extend BpmnModelInstance features
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Set a generated unique id to a single element of the model and return it
    public static String setUniqueId(FlowElement element) {
        // Generate unique prefix
        String uniquePrefix = "fe-" + (new Date()).getTime() + "-";
        return addUniquePrefix(element, uniquePrefix);
    }

    // Generate unique ids to all elements of the model
    public static void generateUniqueIds(BpmnModelInstance modelInstance) {
        String uniquePrefix = "fe-" + (new Date()).getTime() + "-";
        // Set new id for all flow elements in models
        for(FlowElement element: modelInstance.getModelElementsByType(FlowElement.class)) {
            addUniquePrefix(element, uniquePrefix);
        }
    }

    // Add a generated prefix to an element id
    private static String addUniquePrefix(FlowElement element, String uniquePrefix) {
        // Verify if id already contains a generated prefix and substitute just the generated number
        if (element.getId().startsWith("fe-")) {
            String idWithoutPrefix = element.getId().substring(element.getId().indexOf('-', 3) + 1);
            element.setId(uniquePrefix + idWithoutPrefix);
        }
        // Just prepend the prefix
        else {
            element.setId(uniquePrefix + element.getId());
        }
        return element.getId();
    }

    // Connect all nodes before the given node to the ones after it
    public static void connectAllPreviousToSucceedingNodes(FlowNode node) {
        connectAllPreviousToSucceedingNodes(node, node);
    }

    // Connect all previous nodes of a given node, to all succeeding nodes of another given node
    public static void connectAllPreviousToSucceedingNodes(FlowNode previous, FlowNode succeeding) {
        for (FlowNode previousNode: previous.getPreviousNodes().list()) {
            for (FlowNode succeedingNode: succeeding.getSucceedingNodes().list()) {
                previousNode.builder().connectTo(succeedingNode.getId());
            }
        }
    }

    // Builds and connects a new includeNode to appendNode
    // Recursive method, runs until includeNode has no outgoing sequence flows.



    public static void appendTo(BpmnModelInstance modelInstance, FlowNode appendNode, FlowNode includeNode) {

        // Check for null includeNode
        BpmnHelper.checkNotNull(includeNode == null, "Argument includeNode must not be null");
        BpmnHelper.checkNotNull(modelInstance == null, "Argument modelInstance must not be null");

        // Get model instance and parent element
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) appendNode.getParentElement();

        // If node already created, includeNode is connected to appendNode and returns
        if (modelInstance.getModelElementById(includeNode.getId()) != null){
            appendNode.builder().connectTo(includeNode.getId());
            return;
        }

        // Create new FlowNode in model with same properties as flowNode
        FlowNode newNode = contribute(modelInstance, parentElement, includeNode);
        appendNode.builder().connectTo(newNode.getId());

        // Populate subprocess
        if (includeNode instanceof SubProcess) {
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) includeNode);
            populateSubProcess((SubProcess) includeNode, subProcessStartEvent);
        }

        // Recursive call to include all includeNode succeeding nodes
        appendNode = modelInstance.getModelElementById(includeNode.getId());
        for (SequenceFlow sequenceFlow : includeNode.getOutgoing()) {
            includeNode = sequenceFlow.getTarget();
            appendTo(appendNode, includeNode);
        }
    }

    public static void removeNodeAndSequenceFlows(BpmnModelInstance modelInstance, FlowNode node) {
        suppress(modelInstance, node.getIncoming());
        suppress(modelInstance, node.getOutgoing());
        suppress(modelInstance, node.getId());
    }

    public static void appendTo(FlowNode appendNode, FlowNode includeNode) {
        appendTo((BpmnModelInstance) appendNode.getModelInstance(), appendNode, includeNode);
    }

    // Builds and connects a new includeNode to appendNode with a given condition
    public static void conditionalAppendTo(FlowNode appendNode,
                                           FlowNode includeNode,
                                           String conditionName,
                                           String conditionExpression) {

        // Check for null includeNode
        if (includeNode == null){
            throw new NullPointerException("Argument includeNode must not be null");
        }

        // Get model instance and parent element
        BpmnModelInstance modelInstance = (BpmnModelInstance) appendNode.getModelInstance();
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) appendNode.getParentElement();

        // If node already created, includeNode is connected to appendNode and returns
        if (modelInstance.getModelElementById(includeNode.getId()) != null){
            appendNode.builder().condition(conditionName, conditionExpression).connectTo(includeNode.getId());
            return;
        }

        // Create new FlowNode in model with same properties as flowNode
        FlowNode newNode = contribute(modelInstance, parentElement, includeNode);
        appendNode.builder().condition(conditionName, conditionExpression).connectTo(newNode.getId());

        // BPMN SubProcess special case
        if (includeNode instanceof SubProcess) {
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) includeNode);
            populateSubProcess((SubProcess) includeNode, subProcessStartEvent);
        }

    }

    // Insert a new flow node between two flow nodes in the model
    public static void insertFlowNodeBetweenFlowNodes(BpmnModelInstance modelInstance, FlowNode newNode, String node1Id, String node2Id) {
        FlowNode node1 = modelInstance.getModelElementById(node1Id);
        FlowNode node2 = modelInstance.getModelElementById(node2Id);

        if (node1 == null || node2 == null) {
            throw new ElementNotFoundException("No FlowNode with given id found in BpmnModelInstane");
        }

        Iterator<SequenceFlow> sequenceFlowIt = node1.getOutgoing().iterator();

        while (sequenceFlowIt.hasNext()) {
            SequenceFlow currentSequenceFlow = sequenceFlowIt.next();
            FlowNode targetNode = currentSequenceFlow.getTarget();

            if (targetNode.getId().equals(node2.getId())) {
                suppress(modelInstance, currentSequenceFlow);
                break;
            }
        }

        appendTo(modelInstance, node1, newNode);
        FlowNode addedNode = modelInstance.getModelElementById(newNode.getId());
        appendTo(modelInstance, addedNode, node2);

        return;
    }

    public static  <T extends ModelElementInstance> T copyElement(T element) {
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        //BpmnModelInstance modelInstance = (BpmnModelInstance) element.getModelInstance();
        T copiedElement = (T) modelInstance.newInstance(element.getElementType());
        copiedElement.setAttributeValue("id", element.getAttributeValue("id"), true);
        copiedElement.setAttributeValue("name", element.getAttributeValue("name"), true);
        return copiedElement;
    }

    // Populate a subprocess with flow nodes
    public static void populateSubProcess(SubProcess targetSubProcess, StartEvent sourceStartEvent) {
        targetSubProcess.builder().embeddedSubProcess().startEvent(sourceStartEvent.getId()).name(sourceStartEvent.getName());
        BpmnModelInstance modelInstance = (BpmnModelInstance) targetSubProcess.getModelInstance();
        FlowNode appendNode = modelInstance.getModelElementById(sourceStartEvent.getId());
        FlowNode includeNode = sourceStartEvent.getSucceedingNodes().singleResult();

        appendTo(modelInstance, appendNode, includeNode);

    }

    // Make a copy of a BpmnModelInstance
    public static BpmnModelInstance copyModelInstance (BpmnModelInstance modelToCopy) {
        return Bpmn.readModelFromStream(
                new ByteArrayInputStream(
                        Bpmn.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    // Make a copy of a TailorableBpmnModelInstance
    public static TailorableBpmnModelInstance copyModelInstance (TailorableBpmnModelInstance modelToCopy) {
        return TailorableBpmn.readModelFromStream(
                new ByteArrayInputStream(
                        TailorableBpmn.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    // Make a copy of a BpmntModelInstance
    public static BpmntModelInstance copyModelInstance (BpmntModelInstance modelToCopy) {
        return Bpmnt.readModelFromStream(
                new ByteArrayInputStream(
                        Bpmnt.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    // Convert a model to a subprocess
    public static <T extends ModelInstance, E extends ModelElementInstance>
    void convertModelToSubprocess(E parentElement, T modelToConvert) {

        // Create new FlowElement in model with same properties as element parameter
        Process processToConvert = modelToConvert.getModelElementsByType(Process.class).iterator().next();

        SubProcess subProcess =  parentElement.getModelInstance().newInstance(SubProcess.class);
        subProcess.setId(processToConvert.getId());
        subProcess.setName(processToConvert.getName());
        parentElement.addChildElement(subProcess);

        populateSubProcess(subProcess, BpmnElementSearcher.findStartEvent(processToConvert));
    }
}
