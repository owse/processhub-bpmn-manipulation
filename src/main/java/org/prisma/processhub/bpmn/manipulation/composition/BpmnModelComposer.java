package org.prisma.processhub.bpmn.manipulation.composition;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.*;

// Restrictions:
//  Input models must have exactly one "start event" and one "end event"
//  Input models must have only one process


public class BpmnModelComposer
{
    // Public methods

    // Concatenates models in series
    public BpmnModelInstance joinModelsInSeries (BpmnModelInstance... modelsToJoin) {

        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.length == 1) {
            BpmnElementHandler.generateUniqueIds(modelsToJoin[0]);
        }


        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            BpmnElementHandler.generateUniqueIds(mi);
        }

        BpmnModelInstance resultModel = modelsToJoin[0];
        FlowNode lastFlowNodeResult = BpmnElementSearcher.findFlowNodeBeforeEndEvent(resultModel);
        BpmnElementHandler.delete(resultModel, BpmnElementSearcher.findEndEvent(resultModel).getId());

        for (int i = 1; i < modelsToJoin.length; i++) {
            StartEvent startEvent = BpmnElementSearcher.findStartEvent(modelsToJoin[i]);
            FlowNode currentFirstFlowNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelsToJoin[i]);
            BpmnElementHandler.delete(modelsToJoin[i], startEvent.getId());
            BpmnElementHandler.appendTo(lastFlowNodeResult, currentFirstFlowNode);

            if (i+1 < modelsToJoin.length) {
                BpmnElementHandler.delete(resultModel, BpmnElementSearcher.findEndEvent(resultModel).getId());
            }
        }

        return resultModel;
    }

    public BpmnModelInstance joinModelsInSeries (List<BpmnModelInstance> modelsToJoin) {

        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.size() == 1) {
            BpmnElementHandler.generateUniqueIds(modelsToJoin.iterator().next());
        }

        List<BpmnModelInstance> uniqueModelsToJoin = new ArrayList<BpmnModelInstance>();

        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            BpmnElementHandler.generateUniqueIds(mi);
        }

        Iterator<BpmnModelInstance> modelIt = modelsToJoin.iterator();


        BpmnModelInstance resultModel = modelIt.next();
        FlowNode lastFlowNodeResult = BpmnElementSearcher.findFlowNodeBeforeEndEvent(resultModel);
        BpmnElementHandler.delete(resultModel, BpmnElementSearcher.findEndEvent(resultModel).getId());

        while (modelIt.hasNext()) {

            BpmnModelInstance currentModel = modelIt.next();
            StartEvent startEvent = BpmnElementSearcher.findStartEvent(currentModel);
            FlowNode firstFlowNodeCurrent = BpmnElementSearcher.findFlowNodeAfterStartEvent(currentModel);
            BpmnElementHandler.delete(currentModel, startEvent.getId());

            BpmnElementHandler.appendTo(lastFlowNodeResult, firstFlowNodeCurrent);

            if (modelIt.hasNext()) {
                BpmnElementHandler.delete(resultModel, BpmnElementSearcher.findEndEvent(resultModel).getId());
            }
        }
        return resultModel;
    }

    // Concatenates models in parallel
    public BpmnModelInstance joinModelsInParallel (BpmnModelInstance... modelsToJoin) {
        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.length == 1) {
            BpmnElementHandler.generateUniqueIds(modelsToJoin[0]);
            return modelsToJoin[0];
        }

        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            BpmnElementHandler.generateUniqueIds(mi);
        }

        // Select the base model
        BpmnModelInstance resultModel = modelsToJoin[0];

        // Initialize entities related to the split gateway
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        FlowNode firstFlowNodeResult = BpmnElementSearcher.findFlowNodeAfterStartEvent(resultModel);
        ParallelGateway splitGateway;

        // If parallel gateway already exists, reuse it
        if (firstFlowNodeResult instanceof ParallelGateway) {
            splitGateway = (ParallelGateway) firstFlowNodeResult;
        }
        // If not, create a parallel gateway after the start event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            BpmnElementHandler.generateUniqueIds(tempModel);
            splitGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            BpmnElementHandler.suppress(tempModel, splitGateway.getIncoming());
            BpmnElementHandler.suppress(tempModel, splitGateway.getOutgoing());
            BpmnElementHandler.insertFlowNodeBetweenFlowNodes(resultModel, splitGateway, startEvent.getId(), firstFlowNodeResult.getId());
            splitGateway = resultModel.getModelElementById(splitGateway.getId());
        }


        // Initialize entities related to the join gateway
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);
        FlowNode lastFlowNodeResult = BpmnElementSearcher.findFlowNodeBeforeEndEvent(resultModel);
        ParallelGateway joinGateway;

        // If parallel gateway already exists, reuse it
        if (lastFlowNodeResult instanceof ParallelGateway) {
            joinGateway = (ParallelGateway) lastFlowNodeResult;
        }
        // If not, create a parallel gateway before the end event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            BpmnElementHandler.generateUniqueIds(tempModel);
            joinGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            BpmnElementHandler.suppress(tempModel, joinGateway.getIncoming());
            BpmnElementHandler.suppress(tempModel, joinGateway.getOutgoing());
            BpmnElementHandler.insertFlowNodeBetweenFlowNodes(resultModel, joinGateway, lastFlowNodeResult.getId(), endEvent.getId());
            joinGateway = resultModel.getModelElementById(joinGateway.getId());
        }

        List<FlowNode> afterStartNodes = new ArrayList<FlowNode>();
        List<FlowNode> beforeEndNodes = new ArrayList<FlowNode>();

        for (int i = 1; i < modelsToJoin.length; i++) {
            BpmnModelInstance mi = modelsToJoin[i];

            FlowNode flowNodeAfterStart = BpmnElementSearcher.findFlowNodeAfterStartEvent(mi);
            FlowNode flowNodeBeforeEnd = BpmnElementSearcher.findFlowNodeBeforeEndEvent(mi);

            BpmnElementHandler.delete(mi, BpmnElementSearcher.findStartEvent(mi).getId());
            BpmnElementHandler.delete(mi, BpmnElementSearcher.findEndEvent(mi).getId());

            if (flowNodeAfterStart instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeAfterStart.getOutgoing();

                for (SequenceFlow sf: sequenceFlows) {
                    afterStartNodes.add(sf.getTarget());
                }

                BpmnElementHandler.delete(mi, flowNodeAfterStart.getId());
            }
            else {
                afterStartNodes.add(flowNodeAfterStart);
            }

            if (flowNodeBeforeEnd instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeBeforeEnd.getIncoming();

                for (SequenceFlow sf: sequenceFlows) {
                    beforeEndNodes.add(sf.getSource());
                }

                BpmnElementHandler.delete(mi, flowNodeBeforeEnd.getId());
            }
            else {
                beforeEndNodes.add(flowNodeBeforeEnd);
            }

            // Connects the processes to the split gateway
            for (FlowNode fn: afterStartNodes) {
                BpmnElementHandler.appendTo(splitGateway, fn);
            }

            // Connects the last nodes to the join gateway
            for (FlowNode fn: beforeEndNodes) {
                FlowNode ln = resultModel.getModelElementById(fn.getId());
                BpmnElementHandler.appendTo(ln, joinGateway);
            }
        }

        return resultModel;
    }

    public BpmnModelInstance joinModelsInParallel (List<BpmnModelInstance> modelsToJoin) {
        if (modelsToJoin == null) {
            return null;
        }

        if (modelsToJoin.size() == 1) {
            BpmnModelInstance modelInstance = modelsToJoin.iterator().next();
            BpmnElementHandler.generateUniqueIds(modelInstance);
            return modelInstance;
        }

        // Generates unique IDs for every flow node in every model
        for (BpmnModelInstance mi: modelsToJoin) {

            // Imposes restriction of one start event and one end event per model
            if (countStartEvents(mi) != 1 || countEndEvents(mi) != 1) {
                return null;
            }
            BpmnElementHandler.generateUniqueIds(mi);
        }

        // Select the base model
        Iterator<BpmnModelInstance> modelIt = modelsToJoin.iterator();
        BpmnModelInstance resultModel = modelIt.next();

        // Initialize entities related to the split gateway
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        FlowNode firstFlowNodeResult = BpmnElementSearcher.findFlowNodeAfterStartEvent(resultModel);
        ParallelGateway splitGateway;

        // If parallel gateway already exists, reuse it
        if (firstFlowNodeResult instanceof ParallelGateway) {
            splitGateway = (ParallelGateway) firstFlowNodeResult;
        }
        // If not, create a parallel gateway after the start event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            BpmnElementHandler.generateUniqueIds(tempModel);
            splitGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            BpmnElementHandler.suppress(tempModel, splitGateway.getIncoming());
            BpmnElementHandler.suppress(tempModel, splitGateway.getOutgoing());
            BpmnElementHandler.insertFlowNodeBetweenFlowNodes(resultModel, splitGateway, startEvent.getId(), firstFlowNodeResult.getId());
            splitGateway = resultModel.getModelElementById(splitGateway.getId());
        }


        // Initialize entities related to the join gateway
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);
        FlowNode lastFlowNodeResult = BpmnElementSearcher.findFlowNodeBeforeEndEvent(resultModel);
        ParallelGateway joinGateway;

        // If parallel gateway already exists, reuse it
        if (lastFlowNodeResult instanceof ParallelGateway) {
            joinGateway = (ParallelGateway) lastFlowNodeResult;
        }
        // If not, create a parallel gateway before the end event
        else {
            BpmnModelInstance tempModel = Bpmn.createProcess().startEvent().parallelGateway().endEvent().done();
            BpmnElementHandler.generateUniqueIds(tempModel);
            joinGateway = tempModel.getModelElementsByType(ParallelGateway.class).iterator().next();
            BpmnElementHandler.suppress(tempModel, joinGateway.getIncoming());
            BpmnElementHandler.suppress(tempModel, joinGateway.getOutgoing());
            BpmnElementHandler.insertFlowNodeBetweenFlowNodes(resultModel, joinGateway, lastFlowNodeResult.getId(), endEvent.getId());
            joinGateway = resultModel.getModelElementById(joinGateway.getId());
        }

        List<FlowNode> afterStartNodes = new ArrayList<FlowNode>();
        List<FlowNode> beforeEndNodes = new ArrayList<FlowNode>();

        while (modelIt.hasNext()){
            BpmnModelInstance mi = modelIt.next();


            FlowNode flowNodeAfterStart = BpmnElementSearcher.findFlowNodeAfterStartEvent(mi);
            FlowNode flowNodeBeforeEnd = BpmnElementSearcher.findFlowNodeBeforeEndEvent(mi);

            BpmnElementHandler.delete(mi, BpmnElementSearcher.findStartEvent(mi).getId());
            BpmnElementHandler.delete(mi, BpmnElementSearcher.findEndEvent(mi).getId());

            if (flowNodeAfterStart instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeAfterStart.getOutgoing();

                for (SequenceFlow sf: sequenceFlows) {
                    afterStartNodes.add(sf.getTarget());
                }

                BpmnElementHandler.delete(mi, flowNodeAfterStart.getId());
            }
            else {
                afterStartNodes.add(flowNodeAfterStart);
            }

            if (flowNodeBeforeEnd instanceof ParallelGateway) {
                Collection<SequenceFlow> sequenceFlows = flowNodeBeforeEnd.getIncoming();

                for (SequenceFlow sf: sequenceFlows) {
                    beforeEndNodes.add(sf.getSource());
                }

                BpmnElementHandler.delete(mi, flowNodeBeforeEnd.getId());
            }
            else {
                beforeEndNodes.add(flowNodeBeforeEnd);
            }

            // Connects the processes to the split gateway
            for (FlowNode fn: afterStartNodes) {
                BpmnElementHandler.appendTo(splitGateway, fn);
            }

            // Connects the last nodes to the join gateway
            for (FlowNode fn: beforeEndNodes) {
                FlowNode ln = resultModel.getModelElementById(fn.getId());
                BpmnElementHandler.appendTo(ln, joinGateway);
            }
        }

        return resultModel;
    }


    // Private methods
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
}
