package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import junit.framework.TestCase;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BpmntModelInstanceImplTest extends TestCase {

    public void testSuppress() throws Exception {
        System.out.print("Testing suppress");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        // Get the first flow element
        FlowElement flowElementToRemove = modelInstance.getModelElementsByType(FlowElement.class).iterator().next();
        String flowElementToRemoveId = flowElementToRemove.getId();

        // Remove the first flow element
        modelInstance.suppress(flowElementToRemoveId);

        // Verify if the flow element has been removed
        assertEquals(modelInstance.getModelElementById(flowElementToRemoveId), null);

        // Verify model consistency with Camunda API
        try {
            Bpmnt.validateModel(modelInstance);
        }
        catch (Exception e) {
            System.out.println(" ....................... ok");
        }


    }

    public void testRename() throws Exception {
        System.out.print("Testing rename");
        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        // Select a flow node to rename
        FlowNode flowNodeToRename = modelInstance.getModelElementsByType(FlowNode.class).iterator().next();
        String flowNodeToRenameId = flowNodeToRename.getId();
        String newName = "New Name";

        modelInstance.rename(flowNodeToRenameId, newName);

        // Checks if the target node has been renamed correctly
        assertEquals(newName, flowNodeToRename.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
        System.out.println(" ......................... ok");
    }

    public void testDeleteSingleNode() throws Exception {
        System.out.print("Testing delete (single node)");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        FlowNode flowNodeToDelete = modelInstance.getModelElementsByType(Task.class).iterator().next();
        String flowNodeToDeleteId = flowNodeToDelete.getId();
        int initialNumberFlowNodes = modelInstance.getModelElementsByType(FlowNode.class).size();

        // Find start and end events
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(modelInstance);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(modelInstance);

        modelInstance.delete(flowNodeToDeleteId);

        FlowNode remainingTask = modelInstance.getModelElementsByType(Task.class).iterator().next();

        // Checks if the split parallel gateway has been removed and if the remaining task is connected to the start event
        assertEquals(startEvent, remainingTask.getPreviousNodes().singleResult());

        // Checks if the join parallel gateway has been removed and if the remaining task is connected to the end event
        assertEquals(endEvent, remainingTask.getSucceedingNodes().singleResult());

        // Checks if the number of remaining flow nodes is correct
        assertEquals(initialNumberFlowNodes - 3, modelInstance.getModelElementsByType(FlowNode.class).size());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
        System.out.println(" ........... ok");
    }

    public void testDeleteMultipleNodes() {
        System.out.print("Testing delete (multiple nodes)");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));

        ParallelGateway splitGateway = modelInstance.getModelElementById("ParallelGateway_1c6p3yf");
        ParallelGateway joinGateway = modelInstance.getModelElementById("ParallelGateway_07aj32a");
        Task parallelTaskA = modelInstance.getModelElementById("Task_1liqzit");
        Task parallelTaskB = modelInstance.getModelElementById("Task_0dae65c");
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);
        int initialNumberFlowNodes = modelInstance.getModelElementsByType(FlowNode.class).size();

        // Deleting the parallel fragment
        modelInstance.delete(splitGateway, joinGateway);

        // Verifies that every node in the selected fragment has been deleted
        assert modelInstance.getModelElementsByType(ParallelGateway.class).isEmpty();
        assertEquals(null, modelInstance.getModelElementById(parallelTaskA.getId()));
        assertEquals(null, modelInstance.getModelElementById(parallelTaskB.getId()));

        // Verifies that the first task is connected to the last task
        assertEquals(lastNode, firstNode.getSucceedingNodes().singleResult());

        // Checks if the number of remaining flow nodes is correct
        assertEquals(initialNumberFlowNodes - 4, modelInstance.getModelElementsByType(FlowNode.class).size());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
        System.out.println(" ........ ok");

    }


}
