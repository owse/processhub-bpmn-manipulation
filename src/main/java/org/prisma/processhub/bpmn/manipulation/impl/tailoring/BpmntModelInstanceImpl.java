package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.exception.FlowElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.exception.FlowNodeNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementRemover;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
import org.prisma.processhub.bpmn.manipulation.util.BpmnFragmentHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BpmntModelInstanceImpl extends BpmnModelInstanceImpl implements BpmntModelInstance {

    // Constructor
    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    
    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
   
    // Remove flow element leaving the rest of the model untouched
    public void suppress (FlowElement targetElement) {

        Collection<FlowElement> flowElements = getModelElementsByType(Process.class).iterator().next().getFlowElements();
        flowElements.remove(targetElement);
        return;
    }

    public void suppress (String targetElementId) throws FlowElementNotFoundException {
        FlowElement targetElement = getModelElementById(targetElementId);
        if (targetElement == null) {
            throw new FlowElementNotFoundException("Flow Element with id \'" + targetElementId +  "\' not found");
        }
        suppress(targetElement);
        return;
    }

    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename element
    public void rename(String targetElementId, String newName) throws FlowElementNotFoundException {
        FlowElement flowElementToRename = getModelElementById(targetElementId);
        if (flowElementToRename == null) {
            throw new FlowElementNotFoundException("Flow Element with id \'" + targetElementId +  "\' not found");
        }
        flowElementToRename.setName(newName);
        return;
    }

    // Delete a node
    public void delete(FlowNode targetNode){
        
        // Gateways, start and end events are not allowed to be deleted
        if (targetNode instanceof Gateway || targetNode instanceof StartEvent || targetNode instanceof EndEvent) {
            return;
        }

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

    public void delete(String targetNodeId) throws FlowNodeNotFoundException {
        FlowNode targetNode = getModelElementById(targetNodeId);
        if (targetNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + targetNodeId +  "\' not found");
        }
        delete(targetNode);
        return;
    }

    public void delete(FlowNode startingNode, FlowNode endingNode) throws Exception {
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

    public void delete(String startingNodeId, String endingNodeId) throws Exception {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        delete(startingNode , endingNode);
        return;
    }

    public void replace(FlowNode targetNode, FlowNode replacingNode) {
        if (targetNode == null || replacingNode == null) {
            return;
        }

        // A gateway cannot be replaced
        if (targetNode instanceof Gateway) {
            return;
        }

        // A start event can only be replaced by another start event
        if (targetNode instanceof StartEvent && !(replacingNode instanceof StartEvent)) {
            return;
        }

        // An end event can only be replaced by another end event
        if (targetNode instanceof EndEvent && !(replacingNode instanceof EndEvent)) {
            return;
        }

        // Make sure that the replacing node has no other nodes connected to it
        BpmnElementRemover.isolateFlowNode(replacingNode);

        int numberPreviousNodes = targetNode.getPreviousNodes().count();
        int numberSuccedingNodes = targetNode.getSucceedingNodes().count();
        FlowNode previousNode = null;
        FlowNode succedingNode = null;

        if (numberPreviousNodes > 0) {
            previousNode = targetNode.getPreviousNodes().singleResult();
        }

        if (numberSuccedingNodes > 0) {
            succedingNode = targetNode.getSucceedingNodes().singleResult();
        }

        // Replacing a starting node
        if (numberPreviousNodes == 0) {
            BpmnElementCreator.appendTo(targetNode, replacingNode);
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
        BpmnElementRemover.removeFlowNode(this, targetNode.getId());

        return;
    }

    public void replace(String targetNodeId, FlowNode replacingNode) {
        FlowNode targetNode = getModelElementById(targetNodeId);
        replace(targetNode, replacingNode);
        return;
    }

    public void replace(FlowNode targetNode, BpmnModelInstance replacingFragment) {
        if (targetNode == null || replacingFragment == null) {
            return;
        }

        // Gateways, start events or end events cannot be replaced
        if (targetNode instanceof Gateway || targetNode instanceof StartEvent || targetNode instanceof EndEvent) {
            return;
        }

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        if (startEvent == null || endEvent == null) {
            return;
        }

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = targetNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetNode.getSucceedingNodes().singleResult();

        BpmnElementRemover.removeFlowNode(replacingFragment, startEvent.getId());
        BpmnElementRemover.removeFlowNode(replacingFragment, endEvent.getId());

        BpmnElementCreator.appendTo(previousNode, firstNode);
        FlowNode createdLastNode = getModelElementById(lastNode.getId());
        BpmnElementCreator.appendTo(createdLastNode, succeedingNode);

        BpmnElementRemover.removeFlowNode(this, targetNode.getId());

        return;
    }

    public void replace(String targetNodeId, BpmnModelInstance replacingFragment) throws FlowNodeNotFoundException {
        FlowNode targetNode = getModelElementById(targetNodeId);
        if (targetNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + targetNodeId +  "\' not found");
        }
        replace(targetNode, replacingFragment);
        return;
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) throws Exception {
        if (startingNode == null || endingNode == null || replacingNode == null) {
            return;
        }

        // Can't replace fragment with a start or end event
        if (replacingNode instanceof StartEvent || replacingNode instanceof EndEvent) {
            return;
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

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) throws Exception {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        replace(startingNode, endingNode, replacingNode);
        return;
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) throws Exception {
        if (startingNode == null || endingNode == null || replacingFragment == null) {
            return;
        }

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        if (startEvent == null || endEvent == null) {
            return;
        }

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

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) throws Exception {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        replace(startingNode, endingNode, replacingFragment);
        return;
    }

    public void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode){}
    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode){}

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
        if (targetTask == null || newSubProcessModel == null) {
            return;
        }

        StartEvent sourceStartEvent = BpmnElementSearcher.findStartEvent(newSubProcessModel);
        EndEvent sourceEndEvent = BpmnElementSearcher.findEndEvent(newSubProcessModel);

        if (sourceStartEvent == null || sourceEndEvent == null) {
            return;
        }

        FlowNode previousNode = targetTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = targetTask.getSucceedingNodes().singleResult();

        String targetTaskId = targetTask.getId();
        String targetTaskName = targetTask.getName();

        Process newSubProcess = newSubProcessModel.getModelElementsByType(Process.class).iterator().next();
        delete(targetTask);
        previousNode.builder().subProcess(targetTaskId).name(targetTaskName);
        SubProcess createdSubProcess = getModelElementById(targetTaskId);
        createdSubProcess.builder().connectTo(succeedingNode.getId());

        BpmnElementCreator.populateSubProcess(createdSubProcess, sourceStartEvent);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        if (flowNodeToInsert == null || (afterOf == null && beforeOf == null)) {
            return;
        }

        // Unable to insert a node before a start event or after an end event
        if (beforeOf instanceof StartEvent || afterOf instanceof EndEvent) {
            return;
        }

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway) {
            if (afterOf.getSucceedingNodes().count() > 1) {
                return;
            }
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway) {
            if (beforeOf.getPreviousNodes().count() > 1) {
                return;
            }
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            return;
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
        if (fragmentToInsert == null || (afterOf == null && beforeOf == null)) {
            return;
        }

        // Unable to insert a node before a start event or after an end event
        if (beforeOf instanceof StartEvent || afterOf instanceof EndEvent) {
            return;
        }

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway) {
            if (afterOf.getSucceedingNodes().count() > 1) {
                return;
            }
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway) {
            if (beforeOf.getPreviousNodes().count() > 1) {
                return;
            }
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            return;
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
        insert(afterOf, beforeOf, fragmentToInsert.getModelElementsByType(Process.class).iterator().next());
        return;
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        if (flowNodeToInsert == null || afterOf == null || beforeOf == null) {
            return;
        }

        // Unable to insert a node before a start event or after an end event
        if (beforeOf instanceof StartEvent || afterOf instanceof EndEvent) {
            return;
        }

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway) {
            if (afterOf.getSucceedingNodes().count() > 1) {
                return;
            }
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway) {
            if (beforeOf.getPreviousNodes().count() > 1) {
                return;
            }
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            return;
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
        if (fragmentToInsert == null || afterOf == null || beforeOf == null) {
            return;
        }

        // Unable to insert a node before a start event or after an end event
        if (beforeOf instanceof StartEvent || afterOf instanceof EndEvent) {
            return;
        }

        // afterOf can't be a diverging gateway
        if (afterOf instanceof Gateway) {
            if (afterOf.getSucceedingNodes().count() > 1) {
                return;
            }
        }

        // beforeNode can't be a converging gateway
        if (beforeOf instanceof Gateway) {
            if (beforeOf.getPreviousNodes().count() > 1) {
                return;
            }
        }

        // beforeOf and afterOf cannot be the same node
        if (beforeOf == afterOf) {
            return;
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

//    public void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
//    public void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
//    public void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

}
