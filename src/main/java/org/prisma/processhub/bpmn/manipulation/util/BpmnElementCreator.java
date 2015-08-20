package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;


public final class BpmnElementCreator {
    private BpmnElementCreator() {}

    public static <T extends FlowNode> T addFlowNode(BpmnModelElementInstance parentElement, T flowNode) {
        // Create new FlowNode in model with same properties as newElement
        BpmnModelInstance modelInstance = (BpmnModelInstance) parentElement.getModelInstance();
        T newNode = (T) modelInstance.newInstance(flowNode.getElementType());
        newNode.setId(flowNode.getId());
        newNode.setName(flowNode.getName());
        parentElement.addChildElement(newNode);
        return newNode;
    }

    // Builds and connects a new includeNode to appendNode
    // Recursive method, runs while includeNode has outgoing sequence flows.
    public static void appendTo(FlowNode appendNode, FlowNode includeNode) {

        // Check for null includeNode
        if (includeNode == null){
            throw new NullPointerException("Argument includeNode must not be null");
        }

        // Get model instance and parent element
        BpmnModelInstance modelInstance = (BpmnModelInstance) appendNode.getModelInstance();
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) appendNode.getParentElement();

        // If node already created, includeNode is connected to appendNode and returns
        if (modelInstance.getModelElementById(includeNode.getId()) != null){
            appendNode.builder().connectTo(includeNode.getId());
            return;
        }

        // Create new FlowNode in model with same properties as flowNode
        FlowNode newNode = addFlowNode(parentElement, includeNode);
        appendNode.builder().connectTo(newNode.getId());

        if (includeNode instanceof SubProcess) {
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) includeNode);
            populateSubProcess((SubProcess) includeNode, subProcessStartEvent);
        }

        appendNode = modelInstance.getModelElementById(includeNode.getId());

        for (SequenceFlow sequenceFlow : includeNode.getOutgoing()) {
            includeNode = sequenceFlow.getTarget();
            appendTo(appendNode, includeNode);
        }
    }

    // Builds and connects a new includeNode to appendNode with a given condition
    public static void conditionalAppendTo(FlowNode appendNode,
                                           FlowNode includeNode,
                                           String conditionName,
                                           String conditionExpression) {

        // Check for null includeNode
        if (includeNode == null){
            throw new NullPointerException("Argument includeNode must not be null");
        }

        // Get model instance and parent element
        BpmnModelInstance modelInstance = (BpmnModelInstance) appendNode.getModelInstance();
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) appendNode.getParentElement();

        // If node already created, includeNode is connected to appendNode and returns
        if (modelInstance.getModelElementById(includeNode.getId()) != null){
            appendNode.builder().condition(conditionName, conditionExpression).connectTo(includeNode.getId());
            return;
        }

        // Create new FlowNode in model with same properties as flowNode
        FlowNode newNode = addFlowNode(parentElement, includeNode);
        appendNode.builder().condition(conditionName, conditionExpression).connectTo(newNode.getId());

        // BPMN SubProcess special case
        if (includeNode instanceof SubProcess) {
            StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent((SubProcess) includeNode);
            populateSubProcess((SubProcess) includeNode, subProcessStartEvent);
        }

    }

    // Populate a subprocess with flow nodes
    public static void populateSubProcess(SubProcess targetSubProcess, StartEvent sourceStartEvent) {
        targetSubProcess.builder().embeddedSubProcess().startEvent(sourceStartEvent.getId()).name(sourceStartEvent.getName());
        BpmnModelInstance modelInstance = (BpmnModelInstance) targetSubProcess.getModelInstance();
        FlowNode appendNode = modelInstance.getModelElementById(sourceStartEvent.getId());
        FlowNode includeNode = sourceStartEvent.getSucceedingNodes().singleResult();

        appendTo(appendNode, includeNode);

    }

    // Insert a new flow node between two flow nodes in the model
    public static void insertFlowNodeBetweenFlowNodes(BpmnModelInstance modelInstance, FlowNode newNode, String node1Id, String node2Id) {
        FlowNode node1 = modelInstance.getModelElementById(node1Id);
        FlowNode node2 = modelInstance.getModelElementById(node2Id);

        if (node1 == null || node2 == null) {
            throw new ElementNotFoundException("No FlowNode with given id found in BpmnModelInstane");
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

    // Make a copy of a BpmnModelInstance
    public static BpmnModelInstance copyModelInstance (BpmnModelInstance modelToCopy) {
        return Bpmn.readModelFromStream(
                    new ByteArrayInputStream(
                        Bpmn.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                    )
                );
    }

    // Make a copy of a TailorableBpmnModelInstance
    public static TailorableBpmnModelInstance copyModelInstance (TailorableBpmnModelInstance modelToCopy) {
        return TailorableBpmn.readModelFromStream(
                    new ByteArrayInputStream(
                            TailorableBpmn.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                    )
                );
    }

    // Make a copy of a BpmntModelInstance
    public static BpmntModelInstance copyModelInstance (BpmntModelInstance modelToCopy) {
        return Bpmnt.readModelFromStream(
                    new ByteArrayInputStream(
                        Bpmnt.convertToString(modelToCopy).getBytes(StandardCharsets.UTF_8)
                    )
                );
    }

    public static  <T extends ModelElementInstance> T copyElement(T element) {
        BpmnModelInstance modelInstance = (BpmnModelInstance) element.getModelInstance();
        T copiedElement = (T) modelInstance.newInstance(element.getElementType());
        copiedElement.setAttributeValue("id", element.getAttributeValue("id"), true);
        copiedElement.setAttributeValue("name", element.getAttributeValue("name"), true);
        return copiedElement;
    }

    // Add a new single process element to the given parent element
    public static <T extends FlowElement, E extends ModelElementInstance> T add(E parentElement, T element) {
        // Create new FlowElement in model with same properties as element parameter
        T newElement = (T) parentElement.getModelInstance().newInstance(element.getElementType());
        newElement.setId(element.getId());
        newElement.setName(element.getName());
        parentElement.addChildElement(newElement);
        return newElement;
    }

    // Convert a model to a subprocess
    public static <T extends ModelInstance, E extends ModelElementInstance>
                    void convertModelToSubprocess(E parentElement, T modelToConvert) {

        // Create new FlowElement in model with same properties as element parameter
        Process processToConvert = modelToConvert.getModelElementsByType(Process.class).iterator().next();

        SubProcess subProcess =  parentElement.getModelInstance().newInstance(SubProcess.class);
        subProcess.setId(processToConvert.getId());
        subProcess.setName(processToConvert.getName());
        parentElement.addChildElement(subProcess);

        populateSubProcess(subProcess, BpmnElementSearcher.findStartEvent(processToConvert));
    }
}
