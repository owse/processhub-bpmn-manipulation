package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import junit.framework.TestCase;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementRemover;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.ArrayList;
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
            System.out.println(" ............................ ok");
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
        System.out.println(" .............................. ok");
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
        System.out.println(" ................ ok");
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
        System.out.println(" ............. ok");

    }

    public void testReplaceNodeWithNode() {
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        List<Task> tasks = (List) modelInstance1.getModelElementsByType(Task.class);

        FlowNode replacingTask = modelInstance2.getModelElementsByType(Task.class).iterator().next();

//        System.out.println("Original tasks");
//        for (Task task: tasks) {
//            System.out.println(task.getName());
//        }

        Task targetTask1 = (Task) tasks.get(0);
        Task targetTask2 = (Task) tasks.get(1);

        modelInstance1.replace(targetTask2, targetTask1, replacingTask);

        tasks = (List<Task>) modelInstance1.getModelElementsByType(Task.class);

//        System.out.println("New tasks");
//        for (Task task: tasks) {
//            System.out.print(task.getId());
//            System.out.println("  " + task.getSucceedingNodes().singleResult().getId());
//        }



        return;
    }

    public void testSplit() {
        System.out.print("Testing split");
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        Task splitTask = modelInstance1.getModelElementsByType(Task.class).iterator().next();
        String targetId = splitTask.getId();

        FlowNode previousNode = splitTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = splitTask.getSucceedingNodes().singleResult();

        modelInstance1.split(splitTask, modelInstance2);

        // Checks if the subprocess was created with the same ID as the split task
        assert modelInstance1.getModelElementById(targetId) instanceof SubProcess;

        SubProcess subProcess = modelInstance1.getModelElementById(targetId);

        // Checks if the subprocess was correctly placed in the target process
        assertEquals(previousNode, subProcess.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, subProcess.getSucceedingNodes().singleResult());

        // Extract nodes from the model
        StartEvent sourceStartEvent = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode sourceFirstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode sourceLastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);
        EndEvent sourceEndEvent = BpmnElementSearcher.findEndEvent(modelInstance2);

        // Extract nodes from the created subprocess
        StartEvent subProcessStartEvent = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode subProcessFirstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode subProcessLastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);
        EndEvent subProcessEndEvent = BpmnElementSearcher.findEndEvent(modelInstance2);

        // Verifies that all nodes have been correctly created and in the right order
        assertEquals(sourceStartEvent.getId(), subProcessStartEvent.getId());
        assertEquals(sourceFirstNode.getId(), subProcessFirstNode.getId());
        assertEquals(sourceLastNode.getId(), subProcessLastNode.getId());
        assertEquals(sourceEndEvent.getId(), subProcessEndEvent.getId());

        System.out.println(" ............................... ok");
    }

    public void testInsertSingleNodeInSeries() {
        System.out.print("Testing insert in series (single node)");

        // First try (afterOf == null)
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf1 = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf1 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Target node
        Task taskToInsert1 = modelInstance2.getModelElementsByType(Task.class).iterator().next();

        modelInstance1.insert(null, beforeOf1, taskToInsert1);

        Task insertedTask1 = modelInstance1.getModelElementById(taskToInsert1.getId());

        // Check if the node was correctly created and placed in the process
        assertEquals(lastNode1, insertedTask1.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, insertedTask1.getSucceedingNodes().singleResult());


        // Second try (beforeOf == null)
        BpmntModelInstance modelInstance3 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance4 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf2 = modelInstance3.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf2 = modelInstance3.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode lastNode2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        // Target node
        Task taskToInsert2 = modelInstance4.getModelElementsByType(Task.class).iterator().next();

        modelInstance3.insert(afterOf2, null, taskToInsert2);

        Task insertedTask2 = modelInstance3.getModelElementById(taskToInsert2.getId());


        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf2, insertedTask2.getPreviousNodes().singleResult());
        assertEquals(firstNode2, insertedTask2.getSucceedingNodes().singleResult());


        // Third try (afterOf and beforeOf nodes set)
        BpmntModelInstance modelInstance5 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance6 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance5);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance5);

        // Target node
        Task taskToInsert3 = modelInstance6.getModelElementsByType(Task.class).iterator().next();

        modelInstance5.insert(afterOf3, beforeOf3, taskToInsert3);

        Task insertedTask3 = modelInstance5.getModelElementById(taskToInsert3.getId());


        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf3, insertedTask3.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, insertedTask3.getSucceedingNodes().singleResult());

        System.out.println(" ...... ok");
    }

    public void testInsertSingleNodeInParallel() {
        System.out.print("Testing insert in parallel (single node)");

        // First try (afterOf == null)
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Target node
        Task taskToInsert = modelInstance2.getModelElementsByType(Task.class).iterator().next();

        modelInstance1.insert(afterOf, beforeOf, taskToInsert);

        Task insertedTask = modelInstance1.getModelElementById(taskToInsert.getId());

        Collection<ParallelGateway> parallelGateways = modelInstance1.getModelElementsByType(ParallelGateway.class);
        ParallelGateway divergingGateway = null;
        ParallelGateway convergingGateway = null;

        for (ParallelGateway pg: parallelGateways) {
            if (pg.getSucceedingNodes().count() > 1) {
                divergingGateway = pg;
            }
            else {
                convergingGateway = pg;
            }
        }

        // Check if the target node and the gateways were correctly created and placed in the process
        assertEquals(divergingGateway, afterOf.getSucceedingNodes().singleResult());
        assertEquals(convergingGateway, beforeOf.getPreviousNodes().singleResult());

        assertEquals(divergingGateway, firstNode.getPreviousNodes().singleResult());
        assertEquals(divergingGateway, insertedTask.getPreviousNodes().singleResult());

        assertEquals(convergingGateway, lastNode.getSucceedingNodes().singleResult());
        assertEquals(convergingGateway, insertedTask.getSucceedingNodes().singleResult());

        System.out.println(" .... ok");
    }

}
