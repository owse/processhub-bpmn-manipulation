package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.Collection;

public final class BpmnElementSearcher {
    private BpmnElementSearcher() {}

    // Returns the first start event found in a model
    public static StartEvent findStartEvent(BpmnModelInstance modelInstance) {
        return findStartEvent(modelInstance.getModelElementsByType(Process.class).iterator().next());
    }

    // Returns the first start event found in a process
    public static StartEvent findStartEvent(Process process) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) process.getModelInstance();
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        for (StartEvent startEvent: startEvents) {
            ModelElementInstance parent = startEvent.getParentElement();
            if (parent instanceof Process) {
                String processId = ((Process) parent).getId();
                if (processId.equals(process.getId())) {
                    return startEvent;
                }
            }
        }
        return null;
    }

    // Returns the first start event found in a subprocess
    public static StartEvent findStartEvent(SubProcess subProcess) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) subProcess.getModelInstance();
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        for (StartEvent startEvent: startEvents) {
            ModelElementInstance parent = startEvent.getParentElement();
            if (parent instanceof SubProcess) {
                String subProcessId = ((SubProcess) parent).getId();
                if (subProcessId.equals(subProcess.getId())) {
                    return startEvent;
                }
            }
        }
        return null;
    }

    // Returns the first end event found in a model
    public static EndEvent findEndEvent(BpmnModelInstance modelInstance) {
        return findEndEvent(modelInstance.getModelElementsByType(Process.class).iterator().next());
    }

    // Returns the first end event found in a process
    public static EndEvent findEndEvent(Process process) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) process.getModelInstance();
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        for (EndEvent endEvent: endEvents) {
            ModelElementInstance parent = endEvent.getParentElement();
            if (parent instanceof Process) {
                String processId = ((Process) parent).getId();
                if (processId.equals(process.getId())) {
                    return endEvent;
                }
            }
        }
        return null;
    }

    // Returns the first end event found in a subprocess
    public static EndEvent findEndEvent(SubProcess subProcess) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) subProcess.getModelInstance();
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        for (EndEvent endEvent: endEvents) {
            ModelElementInstance parent = endEvent.getParentElement();
            if (parent instanceof SubProcess) {
                String subProcessId = ((SubProcess) parent).getId();
                if (subProcessId.equals(subProcess.getId())) {
                    return endEvent;
                }
            }
        }
        return null;
    }

    // Returns the flow node connected to the start event of the first process of a model
    public static FlowNode findFlowNodeAfterStartEvent (BpmnModelInstance modelInstance) {
        StartEvent startEvent = findStartEvent(modelInstance);
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the start event of a process
    public static FlowNode findFlowNodeAfterStartEvent (Process process) {
        StartEvent startEvent = findStartEvent(process);
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the start event of a subprocess
    public static FlowNode findFlowNodeAfterStartEvent (SubProcess subProcess) {
        StartEvent startEvent = findStartEvent(subProcess);
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the end event of the first process of a model
    public static FlowNode findFlowNodeBeforeEndEvent (BpmnModelInstance modelInstance) {
        EndEvent endEvent = findEndEvent(modelInstance);
        return endEvent.getPreviousNodes().singleResult();
    }

    // Returns the flow node connected to the end event of a process
    public static FlowNode findFlowNodeBeforeEndEvent (Process process) {
        EndEvent endEvent = findEndEvent(process);
        return endEvent.getPreviousNodes().singleResult();
    }

    // Returns the flow node connected to the end event of a subprocess
    public static FlowNode findFlowNodeBeforeEndEvent (SubProcess subProcess) {
        EndEvent endEvent = findEndEvent(subProcess);
        return endEvent.getPreviousNodes().singleResult();
    }



}