package org.prisma.processhub.bpmn.manipulation.tailoring.impl;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.ArrayList;
import java.util.Collection;

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

    public void rename(FlowElement targetElement) {
        FlowElement flowElementToRename = getModelElementById(targetElement.getId());
        flowElementToRename.setName(targetElement.getName());
        return;
    }

    public void rename(String id, String newName) {
        FlowElement flowElementToRename = getModelElementById(id);
        if (flowElementToRename != null) {
            flowElementToRename.setName(newName);
        }
        return;
    }

    public void delete(FlowNode targetNode){
        FlowNode target = getModelElementById(targetNode.getId());
        if (target == null) {
            return;
        }

        if (target instanceof Gateway || target instanceof StartEvent || target instanceof EndEvent) {
            return;
        }

        Collection<SequenceFlow> incomingSequenceFlows = target.getIncoming();
        Collection<SequenceFlow> outgoingSequenceFlows = target.getOutgoing();

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

        removeFlowNode(target.getId());

        for (Gateway g: gatewaysToDelete) {
            FlowNode incomingNode = g.getIncoming().iterator().next().getSource();
            FlowNode outgoingNode = g.getOutgoing().iterator().next().getTarget();
            incomingNode.builder().connectTo(outgoingNode.getId());
            removeFlowNode(g.getId());
        }

        for (FlowNode beforeNode: beforeNodes) {
            for (FlowNode afterNode: afterNodes) {
                beforeNode.builder().connectTo(afterNode.getId());
            }
        }

        return;
    }

    public void delete(String targetNodeId){
        FlowNode targetNode = getModelElementById(targetNodeId);
        delete(targetNode);
    }

    public void delete(FlowNode startingNode, FlowNode endingNode){}

    public void delete(String startingNodeId, String endingNodeId){
        delete((FlowNode) getModelElementById(startingNodeId), (FlowNode) getModelElementById(endingNodeId));
        return;
    }


    public void replace(FlowNode targetNode, Process replacingFragment){}
    public void replace(FlowNode targetStartingNode, FlowNode targetEndingNode, Process replacingFragment){}
    public void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode){}
    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode){}
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode){}
    public void split(Task targetTask, Process newSubProcess){}
    public void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}

    // TODO: insert appendTo code from BpmnModelComposer here
    /*
    public void contribute (FlowElement targetElement) {
        Collection<FlowElement> flowElements = modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements();
        Iterator<FlowElement> flowElementIterator = flowElements.iterator();
        while (flowElementIterator.hasNext()) {
            FlowElement currentElement = flowElementIterator.next();
            if (currentElement.getId().equals(targetElement.getId())) {
                return;
            }
        }
        flowElements.add(targetElement);
        return;
    }
    */

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

    // Private helper methods

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



}
