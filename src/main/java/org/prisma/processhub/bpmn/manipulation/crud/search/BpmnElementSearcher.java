package org.prisma.processhub.bpmn.manipulation.crud.search;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

public final class BpmnElementSearcher {
    private BpmnElementSearcher() {}

    // Returns the first start event found in the model
    public static StartEvent findStartEvent(BpmnModelInstance modelInstance) {
        return modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
    }

    // Returns the first end event found in the model
    public static EndEvent findEndEvent(BpmnModelInstance modelInstance) {
        return modelInstance.getModelElementsByType(EndEvent.class).iterator().next();
    }

    // Returns the flow node connected to the start event
    public static FlowNode findFlowNodeAfterStartEvent (BpmnModelInstance modelInstance) {
        StartEvent startEvent = findStartEvent(modelInstance);
        return startEvent.getOutgoing().iterator().next().getTarget();
    }

    // Returns the flow node connected to the start event
    public static FlowNode findFlowNodeBeforeEndEvent (BpmnModelInstance modelInstance) {
        EndEvent endEvent = findEndEvent(modelInstance);
        return endEvent.getIncoming().iterator().next().getSource();
    }
}