package org.prisma.processhub.bpmn.manipulation;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.util.*;

// Restrictions:
//  Input models must have exactly one "start event" and one "end event"
//  Input models must have only one process


public class ModelComposer
{
    // Public methods

    // Concatenates models in series
    public BpmnModelInstance joinModelsInSeries (List<BpmnModelInstance> modelsToJoin) {

        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.size() == 1) {
            return generateUniqueIds(modelsToJoin.iterator().next());
        }

        List<BpmnModelInstance> uniqueModelsToJoin = new ArrayList<BpmnModelInstance>();

        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            uniqueModelsToJoin.add(generateUniqueIds(mi));
        }

        Iterator<BpmnModelInstance> modelIt = uniqueModelsToJoin.iterator();

        BpmnModelInstance resultModel = modelIt.next();
        FlowNode lastFlowNodeResult = findFlowNodeBeforeEndEvent(resultModel);
        removeFlowNode(resultModel, findEndEvent(resultModel).getId());

        while (modelIt.hasNext()) {

            BpmnModelInstance currentModel = modelIt.next();
            StartEvent startEvent = findStartEvent(currentModel);
            FlowNode firstFlowNodeCurrent = findFlowNodeAfterStartEvent(currentModel);
            removeFlowNode(currentModel, startEvent.getId());

            appendTo(resultModel, lastFlowNodeResult, firstFlowNodeCurrent);

            if (modelIt.hasNext()) {
                removeFlowNode(resultModel, findEndEvent(resultModel).getId());
            }
        }
        return resultModel;
    }

    // Concatenates models in parallel
    public BpmnModelInstance joinModelsInParallel (List<BpmnModelInstance> modelsToJoin) {
        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.size() == 1) {
            return generateUniqueIds(modelsToJoin.iterator().next());
        }

        List<BpmnModelInstance> uniqueModelsToJoin = new ArrayList<BpmnModelInstance>();

        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            uniqueModelsToJoin.add(generateUniqueIds(mi));
        }

        // Select the base model
        Iterator<BpmnModelInstance> modelIt = uniqueModelsToJoin.iterator();
        BpmnModelInstance resultModel = modelIt.next();

        // Initialize entities related to the split gateway
        StartEvent startEvent = findStartEvent(resultModel);
        FlowNode firstFlowNodeResult = findFlowNodeAfterStartEvent(resultModel);
        ParallelGateway splitGateway;

        // If parallel gateway already exists, reuse it
        if (firstFlowNodeResult instanceof ParallelGateway) {
            splitGateway = (ParallelGateway) firstFlowNodeResult;
        }
        // If not, create a parallel gateway after the start event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            tempModel = generateUniqueIds(tempModel);
            splitGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            removeAllSequenceFlows(tempModel, splitGateway.getIncoming());
            removeAllSequenceFlows(tempModel, splitGateway.getOutgoing());
            insertFlowNodeBetweenFlowNodes(resultModel, splitGateway, startEvent.getId(), firstFlowNodeResult.getId());
            splitGateway = resultModel.getModelElementById(splitGateway.getId());
        }


        // Initialize entities related to the join gateway
        EndEvent endEvent = findEndEvent(resultModel);
        FlowNode lastFlowNodeResult = findFlowNodeBeforeEndEvent(resultModel);
        ParallelGateway joinGateway;

        // If parallel gateway already exists, reuse it
        if (lastFlowNodeResult instanceof ParallelGateway) {
            joinGateway = (ParallelGateway) lastFlowNodeResult;
        }
        // If not, create a parallel gateway before the end event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            tempModel = generateUniqueIds(tempModel);
            joinGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            removeAllSequenceFlows(tempModel, joinGateway.getIncoming());
            removeAllSequenceFlows(tempModel, joinGateway.getOutgoing());
            insertFlowNodeBetweenFlowNodes(resultModel, joinGateway, lastFlowNodeResult.getId(), endEvent.getId());
            joinGateway = resultModel.getModelElementById(joinGateway.getId());
        }

        List<FlowNode> afterStartNodes = new ArrayList<FlowNode>();
        List<FlowNode> beforeEndNodes = new ArrayList<FlowNode>();

        while (modelIt.hasNext()){
            BpmnModelInstance mi = modelIt.next();


            FlowNode flowNodeAfterStart = findFlowNodeAfterStartEvent(mi);
            FlowNode flowNodeBeforeEnd = findFlowNodeBeforeEndEvent(mi);

            removeFlowNode(mi, findStartEvent(mi).getId());
            removeFlowNode(mi, findEndEvent(mi).getId());

            if (flowNodeAfterStart instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeAfterStart.getOutgoing();

                for (SequenceFlow sf: sequenceFlows) {
                    afterStartNodes.add(sf.getTarget());
                }

                removeFlowNode(mi, flowNodeAfterStart.getId());
            }
            else {
                afterStartNodes.add(flowNodeAfterStart);
            }

            if (flowNodeBeforeEnd instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeBeforeEnd.getIncoming();

                for (SequenceFlow sf: sequenceFlows) {
                    beforeEndNodes.add(sf.getSource());
                }

                removeFlowNode(mi, flowNodeBeforeEnd.getId());
            }
            else {
                beforeEndNodes.add(flowNodeBeforeEnd);
            }

            // Connects the processes to the split gateway
            for (FlowNode fn: afterStartNodes) {
                appendTo(resultModel, splitGateway, fn);
            }

            // Connects the last nodes to the join gateway
            for (FlowNode fn: beforeEndNodes) {
                FlowNode ln = resultModel.getModelElementById(fn.getId());
                appendTo(resultModel, ln, joinGateway);
            }
        }

        return resultModel;
    }



    // Private methods

    // Adds the current time as prefix to the id of every flow element
    // If already prefixed, the id time is updated
    private BpmnModelInstance generateUniqueIds(BpmnModelInstance modelInstance) {
        String uniqueFlowElementPrefix = "fe-" + (new Date()).getTime() + "-";

        for(FlowElement fe: modelInstance.getModelElementsByType(FlowElement.class)) {
            if (fe.getId().startsWith("fe-")) {
                String idWithoutPrefix = fe.getId().substring(fe.getId().indexOf('-', 3) + 1);
                fe.setId(uniqueFlowElementPrefix + idWithoutPrefix);
            }
            else {
                fe.setId(uniqueFlowElementPrefix + fe.getId());
            }
        }

        return modelInstance;
    }

    // Returns the number of start events found in the model
    private int countStartEvents(BpmnModelInstance modelInstance) {
        List<StartEvent> startEvents = (List) modelInstance.getModelElementsByType(StartEvent.class);
        return startEvents.size();
    }

    // Returns the number of end events found in the model
    private int countEndEvents(BpmnModelInstance modelInstance) {
        List<EndEvent> endEvents = (List) modelInstance.getModelElementsByType(EndEvent.class);
        return endEvents.size();
    }

    // Returns the first start event found in the model
    private StartEvent findStartEvent(BpmnModelInstance modelInstance) {
        return modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
    }

    // Returns the first end event found in the model
    private EndEvent findEndEvent(BpmnModelInstance modelInstance) {
        return modelInstance.getModelElementsByType(EndEvent.class).iterator().next();
    }

    // Returns the flow node connected to the start event
    private FlowNode findFlowNodeAfterStartEvent (BpmnModelInstance modelInstance) {
        StartEvent startEvent = findStartEvent(modelInstance);
        return startEvent.getOutgoing().iterator().next().getTarget();
    }

    // Returns the flow node connected to the start event
    private FlowNode findFlowNodeBeforeEndEvent (BpmnModelInstance modelInstance) {
        EndEvent endEvent = findEndEvent(modelInstance);
        return endEvent.getIncoming().iterator().next().getSource();
    }

    // Returns a BPMN model with the desired sequence flow removed
    private void removeSequenceFlow(BpmnModelInstance modelInstance, SequenceFlow sequenceFlow) {

        if (modelInstance == null || sequenceFlow == null) {
            return;
        }

        modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements().remove(sequenceFlow);
        return;
    }

    // Returns a BPMN model with the desired list of sequence flows removed
    private void removeAllSequenceFlows(BpmnModelInstance modelInstance, Collection<SequenceFlow> sequenceFlows) {

        if (modelInstance == null || sequenceFlows == null) {
            return;
        }

        modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements().removeAll(sequenceFlows);
        return;
    }

    // Removes a flow node and all sequence flows connected to it
    private void removeFlowNode(BpmnModelInstance modelInstance, String flowNodeId) {

        if (modelInstance == null || flowNodeId == null) {
            return;
        }
        FlowNode flowNode = modelInstance.getModelElementById(flowNodeId);

        Collection<SequenceFlow> sequenceFlowsIn = flowNode.getIncoming();
        Collection<SequenceFlow> sequenceFlowsOut = flowNode.getOutgoing();

        removeAllSequenceFlows(modelInstance, sequenceFlowsIn);
        removeAllSequenceFlows(modelInstance, sequenceFlowsOut);
        modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements().remove(flowNode);

        return;
    }

    // Builds and connects a new flowNodeToInclude to flowNodeToBeAppended
    // Recursive method, runs while flowNodeToInclude has outgoing sequence flows.
    private void appendTo(BpmnModelInstance modelInstance, FlowNode flowNodeToBeAppended, FlowNode flowNodeToInclude) {

        // Nothing to do
        if (flowNodeToInclude == null){
            return;
        }

        if (modelInstance.getModelElementById(flowNodeToBeAppended.getId()) == null) {
            return;
        }

        // If node already created, flowNodeToInclude is connected to flowNodeToBeAppended and returns
        if (modelInstance.getModelElementById(flowNodeToInclude.getId()) != null){
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

        flowNodeToBeAppended = modelInstance.getModelElementById(flowNodeToInclude.getId());


        for (SequenceFlow sequenceFlow:flowNodeToInclude.getOutgoing()) {
            flowNodeToInclude = sequenceFlow.getTarget();
            appendTo(modelInstance, flowNodeToBeAppended, flowNodeToInclude);
        }
    }

    // Insert a new flow node between two flow nodes in the model
    private void insertFlowNodeBetweenFlowNodes(BpmnModelInstance modelInstance, FlowNode newNode, String node1Id, String node2Id) {
        FlowNode node1 = modelInstance.getModelElementById(node1Id);
        FlowNode node2 = modelInstance.getModelElementById(node2Id);

        if (node1 == null || node2 == null) {
            return;
        }

        Iterator<SequenceFlow> sequenceFlowIt = node1.getOutgoing().iterator();

        while (sequenceFlowIt.hasNext()) {
            SequenceFlow currentSequenceFlow = sequenceFlowIt.next();
            FlowNode targetNode = currentSequenceFlow.getTarget();

            if (targetNode.getId().equals(node2.getId())) {
                removeSequenceFlow(modelInstance, currentSequenceFlow);
                break;
            }
        }

        appendTo(modelInstance, node1, newNode);
        FlowNode addedNode = modelInstance.getModelElementById(newNode.getId());
        appendTo(modelInstance, addedNode, node2);

        return;
    }


}
