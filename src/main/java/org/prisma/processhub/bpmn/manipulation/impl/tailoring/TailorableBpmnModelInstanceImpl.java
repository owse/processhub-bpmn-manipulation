package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.BpmntOperation;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.Extend;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class TailorableBpmnModelInstanceImpl extends BpmnModelInstanceImpl implements TailorableBpmnModelInstance {

    // Constructor
    public TailorableBpmnModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    // Useful operations that extend BpmnModelInstance features
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Verify if an element is contained in this BpmnModelInstance
    public boolean contains(FlowElement element) {
        return getModelElementById(element.getId()) != null;
    }

    // Set a generated unique id to a single element of the model and return it
    public String setUniqueId(FlowElement element) {
        // Generate unique prefix
        String uniquePrefix = "fe-" + (new Date()).getTime() + "-";
        return addUniquePrefix(element, uniquePrefix);
    }

    // Generate unique ids to all elements of the model
    public void generateUniqueIds() {
        String uniquePrefix = "fe-" + (new Date()).getTime() + "-";
        // Set new id for all flow elements in models
        for(FlowElement element: getModelElementsByType(FlowElement.class)) {
            addUniquePrefix(element, uniquePrefix);
        }
    }

    // Add a generated prefix to an element id
    private String addUniquePrefix(FlowElement element, String uniquePrefix) {
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
    public void connectAllPreviousToSucceedingNodes(FlowNode node) {
        connectAllPreviousToSucceedingNodes(node, node);
    }

    // Connect all previous nodes of a given node, to all succeeding nodes of another given node
    public void connectAllPreviousToSucceedingNodes(FlowNode previous, FlowNode succeeding) {
        for (FlowNode previousNode: previous.getPreviousNodes().list()) {
            for (FlowNode succeedingNode: succeeding.getSucceedingNodes().list()) {
                previousNode.builder().connectTo(succeedingNode.getId());
            }
        }
    }

    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Create a BPMNt model from a tailorable model
    public BpmntModelInstance extend() {

        // Copy the tailorable model to a BPMNt model
        InputStream stream = new ByteArrayInputStream(TailorableBpmn.convertToString(this).getBytes(StandardCharsets.UTF_8));
        BpmntModelInstance bpmntModelInstance = Bpmnt.readModelFromStream(stream);

        Process process = BpmnElementSearcher.findFirstProcess(bpmntModelInstance);

        // Instance and populate the list of operations
        List<BpmntOperation> bpmntLog = new ArrayList<BpmntOperation>();
        Extend ext = new Extend(process.getId());
        bpmntLog.add(ext);
        bpmntModelInstance.setBpmntLog(bpmntLog);

        process.setId(ext.getNewProcessId());

        return bpmntModelInstance;
    }

    // Add a new single process element to the given parent element
    public <T extends FlowElement, E extends ModelElementInstance> T contribute(E parentElement, T element) {
        // Verify that parent element is part of this model
        BpmnHelper.checkElementPresent(this.equals(parentElement.getModelInstance()),
                                       "parentElement is not part of this TailorableBpmnModelInstance");

        // Verify that element id doesn't conflict with another one in the model
        if (getModelElementById(element.getId()) != null) {
            throw new IllegalArgumentException("There is another element with id \'" + element.getId() + "\' in this TailorableBpmnModelInstance");
        }

        // Create new FlowElement in model with same properties as element parameter
        T newElement = (T) newInstance(element.getElementType());
        newElement.setId(element.getId());
        newElement.setName(element.getName());
        parentElement.addChildElement(newElement);
        return newElement;
    }

    // Add a new single element to the first process in this model as parent
    public <T extends FlowElement> T contribute(T element) {
        return contribute(BpmnElementSearcher.findFirstProcess(this), element);
    }


    // Remove flow element leaving the rest of the model untouched
    public <T extends FlowElement> void suppress(T element) {
        // Verify if element is part of this model instance
        BpmnHelper.checkElementPresent(contains(element), "FlowElement with id \'" + element.getId() + "\' is not part of this TailorableBpmnModelInstance");
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) element.getParentElement();
        parentElement.removeChildElement(element);
    }

    // Remove every element in collection
    public <T extends FlowElement> void suppress(Collection<T> elements) {
        for (FlowElement element : elements) {
            suppress(element);
        }
    }

    // Remove flow element by id
    public void suppress(String elementId) {
        FlowElement targetElement = getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        suppress(targetElement);
    }

    // Modify a property of a flow element
    public <T extends FlowElement> void modify(T element, String property, String value) {
        element.setAttributeValue(property, value);
    }

    // Modify a property of a flow element with given id
    public void modify(String elementId, String property, String value) {
        FlowElement targetElement = getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        modify(targetElement, property, value);
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void rename(FlowElement element, String newName) {
        BpmnHelper.checkElementPresent(contains(element),
                "FlowElement with id \'" + element.getId() + "\' is not part of this TailorableBpmnModelInstance");
        element.setName(newName);
    }

    // Rename element by id
    public void rename(String elementId, String newName) {
        FlowElement element = getModelElementById(elementId);
        BpmnHelper.checkElementPresent(element != null, "Flow Element with id \'" + elementId + "\' not found");
        element.setName(newName);
    }


    // Delete a node, all sequence flows connected to it and also obsolete gateways
    public void delete(FlowNode node){
        // Gateways, start and end events are not allowed to be deleted
        BpmnHelper.checkInvalidArgument(node instanceof Gateway || node instanceof StartEvent || node instanceof EndEvent,
                "Argument FlowNode must not be a Gateway, StartEvent or EndEvent");

        fixGatewaysDelete(node.getPreviousNodes().list(), node.getSucceedingNodes().list());

        // Remove flow node and all sequence flows connected to it
        connectAllPreviousToSucceedingNodes(node);
        suppress(node.getIncoming());
        suppress(node.getOutgoing());
        suppress(node);

        // Verify model consistency with Camunda API
        TailorableBpmn.validateModel(this);
    }

    public void fixGatewaysDelete(Collection<FlowNode> previousNodes, Collection<FlowNode> succeedingNodes) {
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
                suppress(gateway.getIncoming());
                suppress(gateway.getOutgoing());
                suppress(gateway);
                // Remove from previous and succeeding nodes list if its present
                succeedingNodes.remove(gateway);
                previousNodes.remove(gateway);
            }
        }
    }


    // Delete element by id
    public void delete(String nodeId) {
        FlowNode node = getModelElementById(nodeId);
        BpmnHelper.checkElementPresent(node != null, "Flow Node with id \'" + nodeId +  "\' not found");
        delete(node);
    }

    // Delete range of elements from startingNode to endingNode
    public void delete(FlowNode startingNode, FlowNode endingNode) {
        Collection<FlowNode> nodesToDelete = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        // Make sure the fragment can be deleted
        BpmnFragmentHandler.validateDeleteProcessFragment(nodesToDelete);

        // Connect nodes before with nodes after fragment
        connectAllPreviousToSucceedingNodes(startingNode, endingNode);

        // Delete all nodes in the fragment
        for (FlowNode node: nodesToDelete) {
            suppress(node.getIncoming());
            suppress(node.getOutgoing());
            suppress(node);
        }
    }

    public void delete(String startingNodeId, String endingNodeId) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        delete(startingNode , endingNode);
    }

    public void replace(FlowNode existingNode, FlowNode replacingNode) {
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

        // Make sure that the replacing node has no other nodes connected to it
        // Avoid changing the original object
        //BpmnElementRemover.isolateFlowNode(replacingNode);

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
            //BpmnElementCreator.appendTo(existingNode, replacingNode);     Replaced by contribute()
            contribute(existingNode.getParentElement(), replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(createdReplacingNode, succedingNode);
        }
        // Replacing an ending node
        else if (numberSuccedingNodes == 0) {
            //BpmnElementCreator.appendTo(previousNode, replacingNode);     Replaced by contribute()
            contribute(existingNode.getParentElement(), replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(previousNode, createdReplacingNode);
        }

        else {
            //BpmnElementCreator.appendTo(previousNode, replacingNode);     Replaced by contribute()
            contribute(existingNode.getParentElement(), replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(previousNode, replacingNode);
            BpmnElementCreator.appendTo(createdReplacingNode, succedingNode);
        }
        BpmnElementRemover.removeFlowNode(this, existingNode.getId());

    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingNode);
    }

    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(existingNode, "Argument existingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        // Gateways, start events or end events cannot be replaced
        BpmnHelper.checkInvalidArgument(existingNode instanceof Gateway || existingNode instanceof StartEvent || existingNode instanceof EndEvent,
                "Gateways, StartEvent or EndEvent existingNode cannot be replaced");

        // Copy the model to avoid changing the original fragment
        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);

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
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingFragment);
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

        // Use contribute() to avoid changing the replacingNode
        //BpmnElementRemover.isolateFlowNode(replacingNode);
        contribute(startingNode.getParentElement(), replacingNode);

        Collection<FlowNode> replacedNodes = BpmnFragmentHandler.mapProcessFragment(startingNode, endingNode);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        // Use contribute() to avoid changing the replacingNode
        replacingNode = getModelElementById(replacingNode.getId());

        BpmnElementCreator.appendTo(previousNode, replacingNode);
        //FlowNode createdNode = getModelElementById(replacingNode.getId());
        //BpmnElementCreator.appendTo(createdNode, succeedingNode);
        BpmnElementCreator.appendTo(replacingNode, succeedingNode);

        for (FlowNode fn: replacedNodes) {
            BpmnElementRemover.removeFlowNode(this, fn.getId());
        }

    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(startingNode, endingNode, replacingNode);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        // Check null arguments
        BpmnHelper.checkNotNull(startingNode, "Argument startingNode must not be null");
        BpmnHelper.checkNotNull(endingNode, "Argument endingNode must not be null");
        BpmnHelper.checkNotNull(replacingFragment, "Argument replacingFragment must not be null");

        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);

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
    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(startingNode, endingNode, replacingFragment);
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
                BpmnElementRemover.removeSequenceFlow(this, sf);
                nodesInSuccession = true;
                break;
            }
        }

        if (!nodesInSuccession) {
            BpmnHelper.checkInvalidArgument(true, "newPositionAfterOf must be directly connected to newPositionBeforeOf");
        }

        // Disconnect the target fragment
        BpmnElementRemover.removeAllSequenceFlows(this, targetStartingNode.getIncoming());
        BpmnElementRemover.removeAllSequenceFlows(this, targetEndingNode.getOutgoing());

        // Connect the previous node to the succeeding node at the old position
        previousNode.builder().connectTo(succeedingNode.getId());

        // Place the target node in the new position
        newPositionAfterOf.builder().connectTo(targetStartingNode.getId());
        targetEndingNode.builder().connectTo(newPositionBeforeOf.getId());
    }

    public void move(String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {

        FlowNode targetStartingNode = getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = getModelElementById(targetEndingNodeId);
        FlowNode newPositionAfterOf = getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = getModelElementById(newPositionBeforeOfId);

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        move(targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);

    }

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

        newSubProcessModel = BpmnElementCreator.copyModelInstance(newSubProcessModel);

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

        //BpmnElementRemover.isolateFlowNode(flowNodeToInsert);

        // Insert node in series before "beforeOf" node
        if (afterOf == null) {
            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();
            FlowNode flowNodeInserted = contribute(beforeOf.getParentElement(), flowNodeToInsert);

            //BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, previousNode.getId(), beforeOf.getId());
            //BpmnElementCreator.appendTo(previousNode, flowNodeInserted);
            //BpmnElementCreator.appendTo(flowNodeInserted, beforeOf);
            previousNode.builder().connectTo(flowNodeInserted.getId()).connectTo(beforeOf.getId());
            //flowNodeInserted.builder().connectTo(beforeOf.getId());
        }

        // Insert node in series after "afterOf" node
        else if (beforeOf == null) {
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();
            FlowNode flowNodeInserted = contribute(afterOf.getParentElement(), flowNodeToInsert);

            //BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, afterOf.getId(), succeedingNode.getId());
            //BpmnElementCreator.appendTo(afterOf, flowNodeInserted);
            //BpmnElementCreator.appendTo(flowNodeInserted, succeedingNode);
            afterOf.builder().connectTo(flowNodeInserted.getId()).connectTo(succeedingNode.getId());
            //flowNodeInserted.builder().connectTo(succeedingNode.getId());
        }

        else {
            // Insert in series
            if (afterOf.getSucceedingNodes().singleResult().equals(beforeOf)) {
                FlowNode flowNodeInserted = contribute(afterOf.getParentElement(), flowNodeToInsert);
                //BpmnElementCreator.insertFlowNodeBetweenFlowNodes(this, flowNodeToInsert, afterOf.getId(), beforeOf.getId());
                //BpmnElementCreator.appendTo(afterOf, flowNodeInserted);
                //BpmnElementCreator.appendTo(flowNodeInserted, beforeOf);
                afterOf.builder().connectTo(flowNodeInserted.getId()).connectTo(beforeOf.getId());
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

            FlowNode flowNodeInserted = contribute(afterOf.getParentElement(), flowNodeToInsert);

            //BpmnElementCreator.appendTo(afterOf.getSucceedingNodes().singleResult(), flowNodeToInsert);
            //FlowNode createdFlowNode = getModelElementById(flowNodeToInsert.getId());
            //BpmnElementCreator.appendTo(createdFlowNode, beforeOf.getPreviousNodes().singleResult());
            afterOf.getSucceedingNodes().singleResult().builder().connectTo(flowNodeInserted.getId());
            flowNodeInserted.builder().connectTo(beforeOf.getPreviousNodes().singleResult().getId());
        }
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

        BpmnModelInstance fragmentModelToInsert = BpmnElementCreator.copyModelInstance((BpmnModelInstance) fragmentToInsert.getModelInstance());
        fragmentToInsert = fragmentModelToInsert.getModelElementById(fragmentToInsert.getId());

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        BpmnElementRemover.removeFlowNode((BpmnModelInstance) fragmentToInsert.getModelInstance(), startEvent.getId());
        BpmnElementRemover.removeFlowNode((BpmnModelInstance) fragmentToInsert.getModelInstance(), endEvent.getId());

        // Insert node in series before "beforeOf" node
        if (afterOf == null) {
            FlowNode previousNode = beforeOf.getPreviousNodes().singleResult();

            suppress(previousNode.getOutgoing().iterator().next());

            BpmnElementCreator.appendTo(previousNode, firstNodeToInsert);
            FlowNode lastInsertedNode = getModelElementById(lastNodeToInsertId);
            BpmnElementCreator.appendTo(lastInsertedNode, beforeOf);
        }

        // Insert node in series after "afterOf" node
        else if (beforeOf == null) {
            FlowNode succeedingNode = afterOf.getSucceedingNodes().singleResult();

            suppress(afterOf.getOutgoing().iterator().next());

            BpmnElementCreator.appendTo(afterOf, firstNodeToInsert);
            FlowNode lastInsertedNode = getModelElementById(lastNodeToInsertId);
            BpmnElementCreator.appendTo(lastInsertedNode, succeedingNode);
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
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
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

        //BpmnElementRemover.isolateFlowNode(flowNodeToInsert);
        flowNodeToInsert = contribute(afterOf.getParentElement(), flowNodeToInsert);

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

        BpmnModelInstance fragmentModelToInsert = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsert = fragmentModelToInsert.getModelElementById(fragmentToInsert.getId());

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(fragmentToInsert);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(fragmentToInsert);
        FlowNode firstNodeToInsert = BpmnElementSearcher.findFlowNodeAfterStartEvent(fragmentToInsert);
        String lastNodeToInsertId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(fragmentToInsert).getId();

        BpmnElementRemover.removeFlowNode((TailorableBpmnModelInstance) fragmentToInsert.getModelInstance(), startEvent.getId());
        BpmnElementRemover.removeFlowNode((TailorableBpmnModelInstance) fragmentToInsert.getModelInstance(), endEvent.getId());


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

}