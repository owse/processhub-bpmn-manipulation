package org.prisma.processhub.bpmn.manipulation.tailoring.impl;

import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.crud.remove.BpmnElementRemover;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class BpmntModelInstanceImpl extends BpmnModelInstanceImpl implements BpmntModelInstance {

    // Constructor
    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    public void suppress (FlowElement targetElement) {

        Collection<FlowElement> flowElements = getModelElementsByType(Process.class).iterator().next().getFlowElements();
        FlowElement targetElementInModel = getModelElementById(targetElement.getId());
        if (targetElementInModel != null) {
            flowElements.remove(targetElementInModel);
        }
        return;
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // TODO: add exception "node not found"
    public void rename(String id, String newName) {
        FlowElement flowElementToRename = getModelElementById(id);
        if (flowElementToRename != null) {
            flowElementToRename.setName(newName);
        }
        return;
    }

    public void delete(FlowNode targetNode){
        if (targetNode instanceof Gateway || targetNode instanceof StartEvent || targetNode instanceof EndEvent) {
            return;
        }

        Collection<SequenceFlow> incomingSequenceFlows = targetNode.getIncoming();
        Collection<SequenceFlow> outgoingSequenceFlows = targetNode.getOutgoing();

        Collection<FlowNode> beforeNodes = new ArrayList<FlowNode>();
        Collection<FlowNode> afterNodes = new ArrayList<FlowNode>();

        Collection<Gateway> gateways = new ArrayList<Gateway>();

        for (SequenceFlow sf: incomingSequenceFlows) {
            FlowNode source = sf.getSource();
            if (source instanceof Gateway) {
                gateways.add((Gateway) source);
            }
            else {
                beforeNodes.add(source);
            }
        }

        for (SequenceFlow sf: outgoingSequenceFlows) {
            FlowNode destination = sf.getTarget();
            if (destination instanceof Gateway) {
                gateways.add((Gateway) destination);
            }
            else {
                afterNodes.add(destination);
            }
        }

        Collection<Gateway> gatewaysToDelete = new ArrayList<Gateway>();

        for (Gateway g: gateways) {
            if (g.getIncoming().size() + g.getOutgoing().size() - 1 < 3) {
                gatewaysToDelete.add(g);
            }
        }

        BpmnElementRemover.removeFlowNode(this, targetNode.getId());

        for (Gateway g: gatewaysToDelete) {
            FlowNode incomingNode = g.getIncoming().iterator().next().getSource();
            FlowNode outgoingNode = g.getOutgoing().iterator().next().getTarget();
            incomingNode.builder().connectTo(outgoingNode.getId());
            BpmnElementRemover.removeFlowNode(this, g.getId());
        }

        for (FlowNode beforeNode: beforeNodes) {
            for (FlowNode afterNode: afterNodes) {
                beforeNode.builder().connectTo(afterNode.getId());
            }
        }

        return;
    }

    // TODO: add exception "node not found"
    public void delete(String targetNodeId){
        FlowNode targetNode = getModelElementById(targetNodeId);
        if (targetNode != null) {
            delete(targetNode);
        }
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

    // TODO: add exception "node not found"
    public void delete(String startingNodeId, String endingNodeId){
        FlowNode startingNode = getModelElementById(startingNodeId);
        if (startingNode != null) {
            delete(startingNode , (FlowNode) getModelElementById(endingNodeId));
        }
        return;
    }

    // TODO: finish implementing
    public void replace(FlowNode targetNode, Process replacingFragment){
        if (targetNode == null || replacingFragment == null) {
            return;
        }

        // Being able to replace start or end events makes no sense
        if (targetNode instanceof StartEvent || targetNode instanceof EndEvent) {
            return;
        }

    }

    // TODO: add exception "node not found"
    public void replace(String targetNodeId, Process replacingFragment) {
        FlowNode targetNode = getModelElementById(targetNodeId);
        if (targetNode != null) {
            replace(targetNode, replacingFragment); ;
        }
        return;
    }

    public void replace(FlowNode targetStartingNode, FlowNode targetEndingNode, Process replacingFragment){}

    // TODO: add exception "node not found"
    public void replace(String targetStartingNodeId, String targetEndingNodeId, Process replacingFragment){
        FlowNode targetStartingNode = getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = getModelElementById(targetEndingNodeId);

        if (targetStartingNode != null && targetEndingNode != null) {
            replace(targetStartingNode, targetEndingNode, replacingFragment);
        }

        return;
    }

    public void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode){}
    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode){}
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode){}
    public void split(Task targetTask, Process newSubProcess){}
    public void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

    // Private helper methods
/*
    // Returns a BPMN model with the desired sequence flow removed
    private void removeSequenceFlow(SequenceFlow sequenceFlow) {

        if (sequenceFlow == null) {
            return;
        }

        getModelElementsByType(Process.class).iterator().next().getFlowElements().remove(sequenceFlow);
        return;
    }

    // Returns a BPMN model with the desired list of sequence flows removed
    private void removeAllSequenceFlows(Collection<SequenceFlow> sequenceFlows) {

        if (sequenceFlows == null) {
            return;
        }

        getModelElementsByType(Process.class).iterator().next().getFlowElements().removeAll(sequenceFlows);
        return;
    }

    // Removes a flow node and all sequence flows connected to it
    private void removeFlowNode(String flowNodeId) {

        if (flowNodeId == null) {
            return;
        }
        FlowNode flowNode = getModelElementById(flowNodeId);

        Collection<SequenceFlow> sequenceFlowsIn = flowNode.getIncoming();
        Collection<SequenceFlow> sequenceFlowsOut = flowNode.getOutgoing();

        removeAllSequenceFlows(sequenceFlowsIn);
        removeAllSequenceFlows(sequenceFlowsOut);
        getModelElementsByType(Process.class).iterator().next().getFlowElements().remove(flowNode);

        return;
    }
*/
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
/*
    // Returns the first start event found in the model
    private StartEvent findStartEvent() {
        return getModelElementsByType(StartEvent.class).iterator().next();
    }

    // Returns the first end event found in the model
    private EndEvent findEndEvent() {
        return getModelElementsByType(EndEvent.class).iterator().next();
    }

    // Returns the flow node connected to the start event
    private FlowNode findFlowNodeAfterStartEvent () {
        StartEvent startEvent = findStartEvent();
        return startEvent.getOutgoing().iterator().next().getTarget();
    }

    // Returns the flow node connected to the start event
    private FlowNode findFlowNodeBeforeEndEvent () {
        EndEvent endEvent = findEndEvent();
        return endEvent.getIncoming().iterator().next().getSource();
    }

    // Builds and connects a new flowNodeToInclude to flowNodeToBeAppended
    // Recursive method, runs while flowNodeToInclude has outgoing sequence flows.
    private void appendTo(FlowNode flowNodeToBeAppended, FlowNode flowNodeToInclude) {

        // Nothing to do
        if (flowNodeToInclude == null){
            return;
        }

        if (getModelElementById(flowNodeToBeAppended.getId()) == null) {
            return;
        }

        // If node already created, flowNodeToInclude is connected to flowNodeToBeAppended and returns
        if (getModelElementById(flowNodeToInclude.getId()) != null){
            flowNodeToBeAppended.builder().connectTo(flowNodeToInclude.getId());
            return;
        }

        // BPMN Tasks
        if (flowNodeToInclude instanceof Task) {
            if (flowNodeToInclude instanceof BusinessRuleTask) {
                flowNodeToBeAppended.builder().businessRuleTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ManualTask) {
                flowNodeToBeAppended.builder().manualTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ReceiveTask) {
                flowNodeToBeAppended.builder().receiveTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ScriptTask) {
                flowNodeToBeAppended.builder().scriptTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof SendTask) {
                flowNodeToBeAppended.builder().sendTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ServiceTask) {
                flowNodeToBeAppended.builder().serviceTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof UserTask) {
                flowNodeToBeAppended.builder().userTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
            // If task unspecified, set to user task
            else {
                flowNodeToBeAppended.builder().userTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN Events
        else if (flowNodeToInclude instanceof Event) {

            if (flowNodeToInclude instanceof IntermediateCatchEvent) {
                flowNodeToBeAppended.builder().intermediateCatchEvent(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof EndEvent) {
                flowNodeToBeAppended.builder().endEvent(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN Gateways
        else if (flowNodeToInclude instanceof Gateway) {
            if (flowNodeToInclude instanceof EventBasedGateway) {
                flowNodeToBeAppended.builder().eventBasedGateway();
            }

            else if (flowNodeToInclude instanceof ExclusiveGateway) {
                flowNodeToBeAppended.builder().exclusiveGateway(flowNodeToInclude.getId());
            }

            else if (flowNodeToInclude instanceof ParallelGateway) {
                flowNodeToBeAppended.builder().parallelGateway(flowNodeToInclude.getId());
            }
        }

        flowNodeToBeAppended = getModelElementById(flowNodeToInclude.getId());


        for (SequenceFlow sequenceFlow:flowNodeToInclude.getOutgoing()) {
            flowNodeToInclude = sequenceFlow.getTarget();
            appendTo(flowNodeToBeAppended, flowNodeToInclude);
        }
    }

    // Insert a new flow node between two flow nodes in the model
    private void insertFlowNodeBetweenFlowNodes(FlowNode newNode, String node1Id, String node2Id) {
        FlowNode node1 = getModelElementById(node1Id);
        FlowNode node2 = getModelElementById(node2Id);

        if (node1 == null || node2 == null) {
            return;
        }

        Iterator<SequenceFlow> sequenceFlowIt = node1.getOutgoing().iterator();

        while (sequenceFlowIt.hasNext()) {
            SequenceFlow currentSequenceFlow = sequenceFlowIt.next();
            FlowNode targetNode = currentSequenceFlow.getTarget();

            if (targetNode.getId().equals(node2.getId())) {
                removeSequenceFlow(currentSequenceFlow);
                break;
            }
        }

        appendTo(node1, newNode);
        FlowNode addedNode = getModelElementById(newNode.getId());
        appendTo(addedNode, node2);

        return;
    }
*/


}
