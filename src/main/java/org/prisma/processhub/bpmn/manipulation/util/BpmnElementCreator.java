package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import java.util.Date;
import java.util.Iterator;


public final class BpmnElementCreator {
    private BpmnElementCreator() {}

    // Builds and connects a new flowNodeToInclude to flowNodeToBeAppended
    // Recursive method, runs while flowNodeToInclude has outgoing sequence flows.
    public static void appendTo(BpmnModelInstance modelInstance, FlowNode flowNodeToBeAppended, FlowNode flowNodeToInclude) {

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

        flowNodeToBeAppended = modelInstance.getModelElementById(flowNodeToInclude.getId());


        for (SequenceFlow sequenceFlow:flowNodeToInclude.getOutgoing()) {
            flowNodeToInclude = sequenceFlow.getTarget();
            appendTo(modelInstance, flowNodeToBeAppended, flowNodeToInclude);
        }
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

        appendTo(modelInstance, node1, newNode);
        FlowNode addedNode = modelInstance.getModelElementById(newNode.getId());
        appendTo(modelInstance, addedNode, node2);

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
