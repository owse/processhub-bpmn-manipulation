package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import junit.framework.TestCase;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.Collection;
import java.util.Iterator;

public class BpmntModelInstanceImplTest extends TestCase {

    public void testSuppress() throws Exception {
        System.out.println("Testing suppress:\n");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        Iterator<FlowElement> flowElementIterator = modelInstance.getModelElementsByType(FlowElement.class).iterator();
        FlowElement flowElementToRemove = flowElementIterator.next();

        System.out.println("Flow Elements:\n");
        System.out.println(flowElementToRemove);

        while (flowElementIterator.hasNext()) {
            System.out.println(flowElementIterator.next());
        }

        modelInstance.suppress(flowElementToRemove);

        Iterator<FlowElement> flowElementIterator1 = modelInstance.getModelElementsByType(FlowElement.class).iterator();

        System.out.println("\n\nFlow elements with first element removed:\n");
        while (flowElementIterator1.hasNext()) {
            System.out.println(flowElementIterator1.next());
        }

    }

    public void testRename() throws Exception {
        System.out.println("\n\nTesting rename:\n");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        Iterator<FlowNode> flowNodeIterator = modelInstance.getModelElementsByType(FlowNode.class).iterator();
        FlowNode flowNodeToRename = flowNodeIterator.next();


        System.out.println("\nFlow Nodes:\n");
        System.out.println(flowNodeToRename.getName());

        while (flowNodeIterator.hasNext()) {
            System.out.println(flowNodeIterator.next().getName());
        }

        modelInstance.rename(flowNodeToRename.getId(), "New Name");

        Iterator<FlowNode> flowNodeIterator1 = modelInstance.getModelElementsByType(FlowNode.class).iterator();

        System.out.println("\n\nFlow nodes with first node renamed:\n");
        while (flowNodeIterator1.hasNext()) {
            System.out.println(flowNodeIterator1.next().getName());
        }

    }

    public void testDeleteSingleNode() throws Exception {
        System.out.println("\n\nTesting delete (single node):\n");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        Iterator<Task> taskIterator = modelInstance.getModelElementsByType(Task.class).iterator();
        Iterator<SequenceFlow> sequenceFlowIterator = modelInstance.getModelElementsByType(SequenceFlow.class).iterator();

        FlowNode flowNodeToDelete = taskIterator.next();

        System.out.println("Tasks:\n");
        System.out.println(flowNodeToDelete.getId());

        while (taskIterator.hasNext()) {
            System.out.println(taskIterator.next().getId());
        }

        System.out.println("\n\nSequence Flows:\n");
        while (sequenceFlowIterator.hasNext()) {
            System.out.println(sequenceFlowIterator.next().getId());
        }

        modelInstance.delete(flowNodeToDelete);

        Iterator<FlowNode> flowNodeIterator = modelInstance.getModelElementsByType(FlowNode.class).iterator();
        Iterator<SequenceFlow> sequenceFlowIterator1 = modelInstance.getModelElementsByType(SequenceFlow.class).iterator();

        System.out.println("\n\nFlow nodes with one task removed:\n");
        while (flowNodeIterator.hasNext()) {
            System.out.println(flowNodeIterator.next().getId());
        }

        System.out.println("\n\nSequence Flows:\n");
        while (sequenceFlowIterator1.hasNext()) {
            System.out.println(sequenceFlowIterator1.next().getId());
        }

    }

    public void testDeleteMultipleNodes() {
        System.out.println("\n\nTesting delete (multiple nodes)");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));

        Iterator<Gateway> gatewayIterator = modelInstance.getModelElementsByType(Gateway.class).iterator();
        FlowNode endingNode = gatewayIterator.next();
        FlowNode startingNode = gatewayIterator.next();

        Collection<FlowNode> flowNodes = modelInstance.getModelElementsByType(FlowNode.class);
        System.out.println("Flow nodes before deletion:\n");
        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        modelInstance.delete(startingNode, endingNode);
        flowNodes = modelInstance.getModelElementsByType(FlowNode.class);

        System.out.println("\n\nFlow nodes after deletion:\n");
        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        Bpmnt.validateModel(modelInstance);
    }


}
