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

    public void delete(FlowNode startingNode, FlowNode endingNode){
        Collection<FlowNode> flowNodesToDelete = mapProcessFragment(startingNode, endingNode);
        if (startingNode instanceof StartEvent || endingNode instanceof EndEvent) {
            return;
        }

        if (validateProcessFragment(flowNodesToDelete)) {
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

    public void delete(String startingNodeId, String endingNodeId) throws FlowNodeNotFoundException {
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
            BpmnElementCreator.appendTo(this, targetNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(this, createdReplacingNode, succedingNode);
        }
        // Replacing an ending node
        else if (numberSuccedingNodes == 0) {
            BpmnElementCreator.appendTo(this, previousNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(this, previousNode, createdReplacingNode);
        }

        else {
            BpmnElementCreator.appendTo(this, previousNode, replacingNode);
            FlowNode createdReplacingNode = getModelElementById(replacingNode.getId());
            BpmnElementCreator.appendTo(this, createdReplacingNode, succedingNode);
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

        BpmnElementCreator.appendTo(this, previousNode, firstNode);
        FlowNode createdLastNode = getModelElementById(lastNode.getId());
        BpmnElementCreator.appendTo(this, createdLastNode, succeedingNode);

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

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        if (startingNode == null || endingNode == null || replacingNode == null) {
            return;
        }

        // Can't replace fragment with a start or end event
        if (replacingNode instanceof StartEvent || replacingNode instanceof EndEvent) {
            return;
        }

        BpmnElementRemover.isolateFlowNode(replacingNode);

        Collection<FlowNode> replacedNodes = mapProcessFragment(startingNode, endingNode);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        BpmnElementCreator.appendTo(this, previousNode, replacingNode);
        FlowNode createdNode = getModelElementById(replacingNode.getId());
        BpmnElementCreator.appendTo(this, createdNode, succeedingNode);

        for (FlowNode fn: replacedNodes) {
            BpmnElementRemover.removeFlowNode(this, fn.getId());
        }

        return;
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) throws FlowNodeNotFoundException {
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

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment){
        if (startingNode == null || endingNode == null || replacingFragment == null) {
            return;
        }

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(replacingFragment);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(replacingFragment);

        if (startEvent == null || endEvent == null) {
            return;
        }

        Collection<FlowNode> replacedNodes = mapProcessFragment(startingNode, endingNode);

        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(replacingFragment);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(replacingFragment);

        FlowNode previousNode = startingNode.getPreviousNodes().singleResult();
        FlowNode succeedingNode = endingNode.getSucceedingNodes().singleResult();

        BpmnElementRemover.removeFlowNode(replacingFragment, startEvent.getId());
        BpmnElementRemover.removeFlowNode(replacingFragment, endEvent.getId());

        BpmnElementCreator.appendTo(this, previousNode, firstNode);
        FlowNode createdLastNode = getModelElementById(lastNode.getId());
        BpmnElementCreator.appendTo(this, createdLastNode, succeedingNode);

        for (FlowNode fn: replacedNodes) {
            BpmnElementRemover.removeFlowNode(this, fn.getId());
        }

        return;

    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) throws FlowNodeNotFoundException {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        if (startingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + startingNodeId +  "\' not found");
        }

        if (endingNode == null) {
            throw new FlowNodeNotFoundException("Flow Node with id \'" + endingNodeId +  "\' not found");
        }

        replace(startingNode , endingNode, replacingFragment);
        return;
    }

    public void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode){}
    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode){}
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode){}

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

    public void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

    // Private helper methods

    // Returns a list with all flow nodes between startingNode and endingNode (both inclusive)
    private Collection<FlowNode> mapProcessFragment(FlowNode startingNode, FlowNode endingNode) {
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
    private void mapProcessFragment(Collection<FlowNode> flowNodes, FlowNode currentNode, FlowNode endingNode) {

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

        flowNodes.add(currentNode);

        Collection<SequenceFlow> sequenceFlows = currentNode.getOutgoing();

        for (SequenceFlow sf: sequenceFlows) {
            mapProcessFragment(flowNodes, sf.getTarget(), endingNode);
        }

    }

    // Verifies if a process fragment is valid
    private boolean validateProcessFragment(Collection<FlowNode> flowNodes) {
        Collection<Gateway> gateways = new ArrayList<Gateway>();

        for (FlowNode fn: flowNodes) {
            if (fn instanceof Gateway) {
                gateways.add((Gateway) fn);
            }
        }

        for (Gateway g: gateways) {
            Collection<SequenceFlow> sequenceFlowsIncoming = g.getIncoming();
            Collection<SequenceFlow> sequenceFlowsOutgoing = g.getOutgoing();
            Collection<FlowNode> flowNodesIncoming = new ArrayList<FlowNode>();
            Collection<FlowNode> flowNodesOutgoing = new ArrayList<FlowNode>();

            for (SequenceFlow sf: sequenceFlowsIncoming) {
                flowNodesIncoming.add(sf.getSource());
            }

            for (SequenceFlow sf: sequenceFlowsOutgoing) {
                flowNodesOutgoing.add(sf.getTarget());
            }

            int incomingCount = 0;
            int outgoingCount = 0;

            for (FlowNode fni: flowNodesIncoming) {
                for (FlowNode fn: flowNodes) {
                    if (fn.getId().equals(fni.getId())) {
                        incomingCount++;
                        break;
                    }
                }
            }

            for (FlowNode fno: flowNodesOutgoing) {
                for (FlowNode fn: flowNodes) {
                    if (fn.getId().equals(fno.getId())) {
                        outgoingCount++;
                        break;
                    }
                }
            }

            if ((incomingCount != 0 && incomingCount < flowNodesIncoming.size()) ||
                    (outgoingCount != 0 && outgoingCount < flowNodesOutgoing.size())) {
                return false;
            }

        }
        return true;
    }

}
