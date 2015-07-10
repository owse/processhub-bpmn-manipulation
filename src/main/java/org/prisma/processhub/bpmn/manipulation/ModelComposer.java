package org.prisma.processhub.bpmn.manipulation;

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
        removeFlowNode(resultModel, findEndEvent(resultModel));

        while (modelIt.hasNext()) {

            BpmnModelInstance currentModel = modelIt.next();
            StartEvent startEvent = findStartEvent(currentModel);
            FlowNode firstFlowNodeCurrent = findFlowNodeAfterStartEvent(currentModel);
            removeFlowNode(currentModel, startEvent);

            appendTo(resultModel, lastFlowNodeResult, firstFlowNodeCurrent);

            if (modelIt.hasNext()) {
                removeFlowNode(resultModel, findEndEvent(resultModel));
            }

            //currentModel = removeFlowNode(currentModel, startEvent);

            // TODO:
            //  Link the last node from result model to the first node from current model
            //  and rebuild the current model inside the result model
            //
            //  Remove the end node from the result model

        }

        // TODO:
        //  Add an end event to the result model

        return resultModel;
    }


    // TODO:
    //  public BpmnModelInstance joinModelsInParallel (List<BpmnModelInstance> modelsToJoin)



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
    public FlowNode findFlowNodeAfterStartEvent (BpmnModelInstance modelInstance) {
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
    private void removeFlowNode(BpmnModelInstance modelInstance, FlowNode flowNode) {

        if (modelInstance == null || flowNode == null) {
            return;
        }

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

}
