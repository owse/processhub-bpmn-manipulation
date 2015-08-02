package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.Date;
import java.util.Iterator;


public final class BpmnElementCreator {
    private BpmnElementCreator() {}

    // Builds and connects a new flowNodeToInclude to flowNodeToBeAppended
    // Recursive method, runs while flowNodeToInclude has outgoing sequence flows.
    public static void appendTo(FlowNode flowNodeToBeAppended, FlowNode flowNodeToInclude) {

        // Nothing to do
        if (flowNodeToInclude == null){
            return;
        }

        BpmnModelInstance modelInstance = (BpmnModelInstance) flowNodeToBeAppended.getModelInstance();

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
            // If task unspecified, set to user task
            else {
                flowNodeToBeAppended.builder().userTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN Events
        else if (flowNodeToInclude instanceof Event) {
            if (flowNodeToInclude instanceof StartEvent) {
                modelInstance.getModelElementsByType(Process.class)
                        .iterator().next().builder()
                            .startEvent(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof IntermediateCatchEvent) {
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

        // BPMN SubProcess
        else if (flowNodeToInclude instanceof SubProcess) {
            flowNodeToBeAppended.builder().subProcess(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) flowNodeToInclude);
            populateSubProcess((SubProcess) flowNodeToInclude, subProcessStartEvent);
        }

        flowNodeToBeAppended = modelInstance.getModelElementById(flowNodeToInclude.getId());

        for (SequenceFlow sequenceFlow:flowNodeToInclude.getOutgoing()) {
            flowNodeToInclude = sequenceFlow.getTarget();
            appendTo(flowNodeToBeAppended, flowNodeToInclude);
        }
    }

    // Builds and connects a new flowNodeToInclude to flowNodeToBeAppended with a given condition
    public static void conditionalAppendTo(FlowNode flowNodeToBeAppended,
                                           FlowNode flowNodeToInclude,
                                           String conditionName,
                                           String conditionExpression) {

        // Nothing to do
        if (flowNodeToInclude == null){
            return;
        }

        BpmnModelInstance modelInstance = (BpmnModelInstance) flowNodeToBeAppended.getModelInstance();

        // If node already created, flowNodeToInclude is connected to flowNodeToBeAppended and returns
        if (modelInstance.getModelElementById(flowNodeToInclude.getId()) != null){
            flowNodeToBeAppended.builder().condition(conditionName, conditionExpression).connectTo(flowNodeToInclude.getId());
            return;
        }

        // BPMN Tasks
        if (flowNodeToInclude instanceof Task) {
            if (flowNodeToInclude instanceof BusinessRuleTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .businessRuleTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ManualTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .manualTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ReceiveTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .receiveTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ScriptTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .scriptTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof SendTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .sendTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ServiceTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .serviceTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof UserTask) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .userTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
            // If task unspecified, set to user task
            else {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .userTask(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN Events
        else if (flowNodeToInclude instanceof Event) {
            if (flowNodeToInclude instanceof IntermediateCatchEvent) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .intermediateCatchEvent(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof EndEvent) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .endEvent(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN Gateways
        else if (flowNodeToInclude instanceof Gateway) {
            if (flowNodeToInclude instanceof EventBasedGateway) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .eventBasedGateway().name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ExclusiveGateway) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .exclusiveGateway(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }

            else if (flowNodeToInclude instanceof ParallelGateway) {
                flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                        .parallelGateway(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            }
        }

        // BPMN SubProcess
        else if (flowNodeToInclude instanceof SubProcess) {
            flowNodeToBeAppended.builder().condition(conditionName, conditionExpression)
                    .subProcess(flowNodeToInclude.getId()).name(flowNodeToInclude.getName());
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) flowNodeToInclude);
            populateSubProcess((SubProcess) flowNodeToInclude, subProcessStartEvent);
        }

    }

    // Populate a subprocess with flow nodes
    public static void populateSubProcess(SubProcess targetSubProcess, StartEvent sourceStartEvent) {
        targetSubProcess.builder().embeddedSubProcess().startEvent(sourceStartEvent.getId()).name(sourceStartEvent.getName());
        BpmnModelInstance modelInstance = (BpmnModelInstance) targetSubProcess.getModelInstance();
        FlowNode flowNodeToBeAppended = modelInstance.getModelElementById(sourceStartEvent.getId());
        FlowNode flowNodeToInclude = sourceStartEvent.getSucceedingNodes().singleResult();

        appendTo(flowNodeToBeAppended, flowNodeToInclude);

    }

    // Insert a new flow node between two flow nodes in the model
    public static void insertFlowNodeBetweenFlowNodes(BpmnModelInstance modelInstance, FlowNode newNode, String node1Id, String node2Id) {
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
                BpmnElementRemover.removeSequenceFlow(modelInstance, currentSequenceFlow);
                break;
            }
        }

        appendTo(node1, newNode);
        FlowNode addedNode = modelInstance.getModelElementById(newNode.getId());
        appendTo(addedNode, node2);

        return;
    }

    public static void generateUniqueIds(BpmnModelInstance modelInstance) {
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
        return;
    }

}
