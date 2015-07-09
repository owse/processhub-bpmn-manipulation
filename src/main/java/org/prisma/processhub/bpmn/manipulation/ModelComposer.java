package org.prisma.processhub.bpmn.manipulation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

// Restrictions:
//  Input models must have exactly one "start event" and one "end event"
//  Input models must have only one process


public class ModelComposer
{

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

        BpmnModelInstance resultModel = uniqueModelsToJoin.get(0);
        uniqueModelsToJoin.remove(resultModel);
        FlowNode lastFlowNode = findFlowNodeBeforeEndEvent(resultModel);
        EndEvent endEvent = findEndEvent(resultModel);
        resultModel = removeFlowNode(resultModel, endEvent);
        Iterator<BpmnModelInstance> modelIt = uniqueModelsToJoin.iterator();

        while (modelIt.hasNext()) {

            BpmnModelInstance currentModel = modelIt.next();
            StartEvent startEvent = findStartEvent(currentModel);
            endEvent = findEndEvent(currentModel);
            FlowNode firstFlowNode = findFlowNodeAfterStartEvent(currentModel);
            currentModel = removeFlowNode(currentModel, startEvent);

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
    private BpmnModelInstance removeSequenceFlow(BpmnModelInstance modelInstance, SequenceFlow sequenceFlow) {

        if (modelInstance == null) {
            return null;
        }

        if (sequenceFlow == null) {
            return modelInstance;
        }

        sequenceFlow.getSource().getOutgoing().remove(sequenceFlow);
        sequenceFlow.getTarget().getIncoming().remove(sequenceFlow);

        return modelInstance;
    }

    // Returns a BPMN model with the desired list of sequence flows removed
    private BpmnModelInstance removeAllSequenceFlows(BpmnModelInstance modelInstance, List<SequenceFlow> sequenceFlows) {

        if (modelInstance == null) {
            return null;
        }

        if (sequenceFlows == null) {
            return modelInstance;
        }

        for (SequenceFlow sf: sequenceFlows) {
            sf.getSource().getOutgoing().remove(sf);
            sf.getTarget().getIncoming().remove(sf);
        }

        return modelInstance;
    }

    // Removes a flow node and all sequence flows connected to it
    private BpmnModelInstance removeFlowNode(BpmnModelInstance modelInstance, FlowNode flowNode) {

        if (modelInstance == null) {
            return null;
        }

        if (flowNode == null) {
            return modelInstance;
        }

        List<SequenceFlow> sequenceFlows = (List) modelInstance.getModelElementsByType(SequenceFlow.class);

        for (SequenceFlow sf: sequenceFlows) {
            FlowNode source = sf.getSource();
            FlowNode target = sf.getTarget();

            if (source.getName().equals(flowNode.getName())) {
                modelInstance = removeSequenceFlow(modelInstance, sf);
                List<FlowNode> flowNodes = (List) modelInstance.getModelElementsByType(FlowNode.class).iterator();
                flowNodes.remove(source);
                break;
            }

            if (target.getName().equals(flowNode.getName())) {
                modelInstance = removeSequenceFlow(modelInstance, sf);
                List<FlowNode> flowNodes = (List) modelInstance.getModelElementsByType(FlowNode.class).iterator();
                flowNodes.remove(target);
                break;
            }
        }

        return modelInstance;
    }

}
