package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;

import java.util.Collection;

public final class BpmnElementSearcher {
    private BpmnElementSearcher() {}

    // Returns the first start event found in a model
    public static StartEvent findStartEvent(BpmnModelInstance modelInstance) {
        Process process = findFirstProcess(modelInstance);
        return findStartEvent(process);
    }

    // Returns the first start event found in a process
    public static StartEvent findStartEvent(Process process) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) process.getModelInstance();
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        if (startEvents.isEmpty()) {
            throw new ElementNotFoundException("No StartEvent found in BpmnModelInstance");
        }
        for (StartEvent startEvent: startEvents) {
            // It is expected there's at least one StartEvent whose parent is Process
            ModelElementInstance parent = startEvent.getParentElement();
            if (parent instanceof Process) {
                String processId = ((Process) parent).getId();
                if (processId.equals(process.getId())) {
                    return startEvent;
                }
            }
        }
        throw new ElementNotFoundException("No StartEvent found in given Process");
    }

    // Returns the first start event found in a subprocess
    public static StartEvent findStartEvent(SubProcess subProcess) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) subProcess.getModelInstance();
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        if (startEvents.isEmpty()) {
            throw new ElementNotFoundException("No StartEvent found in BpmnModelInstance");
        }
        for (StartEvent startEvent: startEvents) {
            ModelElementInstance parent = startEvent.getParentElement();
            if (parent instanceof SubProcess) {
                String subProcessId = ((SubProcess) parent).getId();
                if (subProcessId.equals(subProcess.getId())) {
                    return startEvent;
                }
            }
        }
        throw new ElementNotFoundException("No StartEvent found in given SubProcess");
    }

    // Returns the first end event found in a model
    public static EndEvent findEndEvent(BpmnModelInstance modelInstance) {
        Process process = findFirstProcess(modelInstance);
        return findEndEvent(process);
    }

    // Returns the first end event found in a process
    public static EndEvent findEndEvent(Process process) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) process.getModelInstance();
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        if (endEvents.isEmpty()) {
            throw new ElementNotFoundException("No EndEvents found in BpmnModelInstance");
        }
        for (EndEvent endEvent: endEvents) {
            ModelElementInstance parent = endEvent.getParentElement();
            if (parent instanceof Process) {
                String processId = ((Process) parent).getId();
                if (processId.equals(process.getId())) {
                    return endEvent;
                }
            }
        }
        throw new ElementNotFoundException("No EndEvent found in given Process");
    }

    // Returns the first end event found in a subprocess
    public static EndEvent findEndEvent(SubProcess subProcess) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) subProcess.getModelInstance();
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        if (endEvents.isEmpty()) {
            throw new ElementNotFoundException("No EndEvents found in BpmnModelInstance");
        }
        for (EndEvent endEvent: endEvents) {
            ModelElementInstance parent = endEvent.getParentElement();
            if (parent instanceof SubProcess) {
                String subProcessId = ((SubProcess) parent).getId();
                if (subProcessId.equals(subProcess.getId())) {
                    return endEvent;
                }
            }
        }
        throw new ElementNotFoundException("No EndEvent found in given SubProcess");
    }

    // Returns the flow node connected to the start event of the first process of a model
    public static FlowNode findFlowNodeAfterStartEvent (BpmnModelInstance modelInstance) {
        StartEvent startEvent = findStartEvent(modelInstance);
        if (startEvent.getSucceedingNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found after StartEvent");
        }
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the start event of a process
    public static FlowNode findFlowNodeAfterStartEvent (Process process) {
        StartEvent startEvent = findStartEvent(process);
        if (startEvent.getSucceedingNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found after StartEvent");
        }
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the start event of a subprocess
    public static FlowNode findFlowNodeAfterStartEvent (SubProcess subProcess) {
        StartEvent startEvent = findStartEvent(subProcess);
        if (startEvent.getSucceedingNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found after StartEvent");
        }
        return startEvent.getSucceedingNodes().singleResult();
    }

    // Returns the flow node connected to the end event of the first process of a model
    public static FlowNode findFlowNodeBeforeEndEvent (BpmnModelInstance modelInstance) {
        EndEvent endEvent = findEndEvent(modelInstance);
        if (endEvent.getPreviousNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found before EndEvent");
        }
        return endEvent.getPreviousNodes().singleResult();
    }

    // Returns the flow node connected to the end event of a process
    public static FlowNode findFlowNodeBeforeEndEvent (Process process) {
        EndEvent endEvent = findEndEvent(process);
        if (endEvent.getPreviousNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found before EndEvent");
        }
        return endEvent.getPreviousNodes().singleResult();
    }

    // Returns the flow node connected to the end event of a subprocess
    public static FlowNode findFlowNodeBeforeEndEvent (SubProcess subProcess) {
        EndEvent endEvent = findEndEvent(subProcess);
        if (endEvent.getPreviousNodes().count() == 0) {
            throw new ElementNotFoundException("No FlowNode found before EndEvent");
        }
        return endEvent.getPreviousNodes().singleResult();
    }

    public static Process findFirstProcess(BpmnModelInstance modelInstance) {
        // Check for null argument
        BpmnHelper.checkNotNull(modelInstance, "Argument modelInstance must not be null");

        // Check if there is at least one process
        Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
        if (processes.isEmpty()) {
            throw new ElementNotFoundException("No Process found in BpmnModelInstance");
        }

        return processes.iterator().next();
    }
}