package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.instance.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.Collection;

import static org.junit.Assert.*;

public class BpmntModelInstanceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private BpmntModelInstance simpleModel;
    private BpmntModelInstance simpleModel2;
    private BpmntModelInstance parallelModel;
    private BpmntModelInstance parallelModel2;
    private BpmntModelInstance subprocessModel;
    private BpmntModelInstance fragmentModel;

    // Load diagrams before each test
    @Before
    public void loadDiagrams() {
        simpleModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        simpleModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        parallelModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        parallelModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
        subprocessModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
        fragmentModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
    }

    // Tests naming convention: methodName_StateUnderTest_ExpectedBehavior

    // Test cases for the add method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void add_ElementFromSameModel_ExceptionThrown() {
        // Add element from same model
        FlowElement element = BpmnElementSearcher.findFlowNodeBeforeEndEvent(simpleModel);
        exception.expect(IllegalArgumentException.class);
        simpleModel.add(element);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(simpleModel);
    }

    @Test
    public void add_ParentFromOtherModel_ExceptionThrown() {
        // Add element from this model with parent from another
        FlowElement element = BpmnElementSearcher.findFlowNodeBeforeEndEvent(simpleModel);
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        exception.expect(ElementNotFoundException.class);
        simpleModel.add(foreignElement.getParentElement(), element);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(simpleModel);
    }

    @Test
    public void add_ElementFromOtherModel_ElementAdded() {
        // Add element from another model
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        simpleModel.add(foreignElement);

        // Verify new element in model has same properties
        FlowElement newElement = simpleModel.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(simpleModel);
    }

    @Test
    public void add_CreatedElement_ElementAdded() {
        // Add newly created element
        UserTask newTask = parallelModel.newInstance(UserTask.class);
        newTask.setId("my_new_id");
        newTask.setName("new_name");
        simpleModel.add(newTask);

        // Verify new element has been created in model
        FlowElement newElement = simpleModel.getModelElementById(newTask.getId());
        assertEquals(newElement.getId(), newTask.getId());
        assertEquals(newElement.getName(), newTask.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(simpleModel);
    }

    @Test
    public void add_SameElementTwice_ExceptionThrown() {
        // Add foreign element
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        simpleModel.add(foreignElement);

        // Verify element was added with same properties
        FlowElement newElement = simpleModel.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());

        // Add with different id
        String oldId = foreignElement.getId();
        simpleModel.setUniqueId(foreignElement);
        simpleModel.add(foreignElement);

        // Verify element was added with same properties
        newElement = simpleModel.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());
        assertNotEquals(oldId, newElement.getId());

        // Add again without changing id
        exception.expect(IllegalArgumentException.class);
        simpleModel.add(foreignElement);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(simpleModel);
    }


    // Test cases for the suppress method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void suppress_ElementFromModel_ElementRemoved() {
        // Get the first flow element
        FlowElement flowElementToRemove = simpleModel.getModelElementsByType(FlowElement.class).iterator().next();
        String flowElementToRemoveId = flowElementToRemove.getId();

        // Remove the first flow element
        simpleModel.suppress(flowElementToRemoveId);

        // Verify if the flow element has been removed
        assertEquals(simpleModel.getModelElementById(flowElementToRemoveId), null);

    }

    @Test
    public void suppress_ElementFromOtherModel_ExceptionThrown() {
        // Get first element from other model
        FlowElement foreignElement = parallelModel.getModelElementsByType(FlowElement.class).iterator().next();

        exception.expect(ElementNotFoundException.class);
        simpleModel2.suppress(foreignElement);
    }

    @Test
    public void suppress_ElementFromModelTwice_ExceptionThrown() {
        // Get the first flow element
        FlowElement flowElementToRemove = simpleModel.getModelElementsByType(FlowElement.class).iterator().next();
        String flowElementToRemoveId = flowElementToRemove.getId();

        // Remove element
        simpleModel.suppress(flowElementToRemoveId);
        assertEquals(simpleModel.getModelElementById(flowElementToRemoveId), null);

        // Remove twice
        exception.expect(ElementNotFoundException.class);
        simpleModel.suppress(flowElementToRemove);
    }


    // Test cases for the rename method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void testRename() {
        System.out.println("Testing rename");
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
    }

    @Test
    public void testDeleteSingleNode() {
        System.out.println("Testing delete (single node)");

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
    }

    @Test
    public void testDeleteMultipleNodes() {
        System.out.println("Testing delete (multiple nodes)");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));

        ParallelGateway splitGateway = modelInstance.getModelElementById("ParallelGateway_1c6p3yf");
        ParallelGateway joinGateway = modelInstance.getModelElementById("ParallelGateway_07aj32a");
        Task parallelTaskA = modelInstance.getModelElementById("Task_1liqzit");
        Task parallelTaskB = modelInstance.getModelElementById("Task_0dae65c");
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);
        int initialNumberFlowNodes = modelInstance.getModelElementsByType(FlowNode.class).size();

        // Deleting the parallel fragment
        try {
            modelInstance.delete(splitGateway, joinGateway);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    }

    public void testReplaceNodeWithNode() {
        System.out.println("Testing replace (node for node)");
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        FlowNode replacingTask = modelInstance2.getModelElementsByType(Task.class).iterator().next();
        FlowNode replacedTask = modelInstance1.getModelElementsByType(Task.class).iterator().next();

        FlowNode previousNode = replacedTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask.getSucceedingNodes().singleResult();

        modelInstance1.replace(replacedTask.getId(), replacingTask);

        FlowNode newTask = modelInstance1.getModelElementById(replacingTask.getId());

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask.getSucceedingNodes().singleResult());
        assertEquals(replacingTask.getId(), newTask.getId());

        Bpmnt.validateModel(modelInstance1);

    }

    public void testReplaceNodeWithFragment() {
        System.out.println("Testing replace (node for fragment)");

        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        FlowNode replacedTask = modelInstance1.getModelElementsByType(Task.class).iterator().next();

        FlowNode previousNode = replacedTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask.getSucceedingNodes().singleResult();

        String startingNodeId = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String endingNodeId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        modelInstance1.replace(replacedTask.getId(), modelInstance2);

        FlowNode startingNode = modelInstance1.getModelElementById(startingNodeId);
        FlowNode endingNode = modelInstance1.getModelElementById(endingNodeId);

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, startingNode.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, endingNode.getSucceedingNodes().singleResult());
        assertEquals(startingNodeId, startingNode.getId());
        assertEquals(endingNodeId, endingNode.getId());
        assertEquals(3, modelInstance1.getModelElementsByType(Task.class).size());

        Bpmnt.validateModel(modelInstance1);
    }

    public void testReplaceFragmentWithNode() {
        System.out.println("Testing replace (fragment for node)");
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        FlowNode replacingTask = modelInstance2.getModelElementsByType(Task.class).iterator().next();
        FlowNode replacedTask1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode replacedTask2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        FlowNode previousNode = replacedTask1.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask2.getSucceedingNodes().singleResult();

        modelInstance1.replace(replacedTask1.getId(), replacedTask2.getId(), replacingTask);

        FlowNode newTask = modelInstance1.getModelElementById(replacingTask.getId());

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask.getSucceedingNodes().singleResult());
        assertEquals(replacingTask.getId(), newTask.getId());
        assertEquals(1, modelInstance1.getModelElementsByType(Task.class).size());

        Bpmnt.validateModel(modelInstance1);
    }

    public void testReplaceFragmentWithFragment() {
        System.out.println("Testing replace (fragment for fragment)");
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        String replacingTaskId1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String replacingTaskId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        FlowNode replacedTask1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode replacedTask2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        FlowNode previousNode = replacedTask1.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask2.getSucceedingNodes().singleResult();

        modelInstance1.replace(replacedTask1.getId(), replacedTask2.getId(), modelInstance2);

        FlowNode newTask1 = modelInstance1.getModelElementById(replacingTaskId1);
        FlowNode newTask2 = modelInstance1.getModelElementById(replacingTaskId2);

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask1.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask2.getSucceedingNodes().singleResult());
        assertEquals(replacingTaskId1, newTask1.getId());
        assertEquals(replacingTaskId2, newTask2.getId());
        assertEquals(2, modelInstance1.getModelElementsByType(Task.class).size());
        assertEquals(3, modelInstance1.getModelElementsByType(SequenceFlow.class).size());

        Bpmnt.validateModel(modelInstance1);
    }

    @Test
    public void testMoveSingleNode() {
        System.out.println("Testing move (single node)");

        // First try: both position arguments set
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        StartEvent afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode target1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Switching the first task with the second task
        modelInstance1.move(target1.getId(), afterOf1.getId(), beforeOf1.getId());

        // Verifies if the tasks were successfully switched
        assertEquals(target1, afterOf1.getSucceedingNodes().singleResult());
        assertEquals(target1, beforeOf1.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, BpmnElementSearcher.findEndEvent(modelInstance1).getPreviousNodes().singleResult());


        // Second try: afterOf position set
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        StartEvent afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode beforeOf2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode target2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        // Switching the first task with the second task
        modelInstance2.move(target2.getId(), afterOf2.getId(), null);

        // Verifies if the tasks were successfully switched
        assertEquals(target2, afterOf2.getSucceedingNodes().singleResult());
        assertEquals(target2, beforeOf2.getPreviousNodes().singleResult());
        assertEquals(beforeOf2, BpmnElementSearcher.findEndEvent(modelInstance2).getPreviousNodes().singleResult());


        // Third try: beforeOf position set
        BpmntModelInstance modelInstance3 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        StartEvent afterOf3 = BpmnElementSearcher.findStartEvent(modelInstance3);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode target3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        // Switching the first task with the second task
        modelInstance3.move(target3.getId(), afterOf3.getId(), beforeOf3.getId());

        // Verifies if the tasks were successfully switched
        assertEquals(target3, afterOf3.getSucceedingNodes().singleResult());
        assertEquals(target3, beforeOf3.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, BpmnElementSearcher.findEndEvent(modelInstance3).getPreviousNodes().singleResult());

    }

    @Test
    public void testMoveFragment() {
        System.out.println("Testing move (fragment)");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));

        StartEvent afterOf = BpmnElementSearcher.findStartEvent(modelInstance);
        FlowNode beforeOf = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode targetStartingNode = beforeOf.getSucceedingNodes().singleResult();
        FlowNode targetEndingNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance).getPreviousNodes().singleResult();

        // Moving the parallel fragment to the start of the process
        modelInstance.move(targetStartingNode.getId(), targetEndingNode.getId(), afterOf.getId(), beforeOf.getId());

        // Verifies if the fragment was successfully inserted in the new position
        assertEquals(targetStartingNode, afterOf.getSucceedingNodes().singleResult());
        assertEquals(targetEndingNode, beforeOf.getPreviousNodes().singleResult());
        assertEquals(beforeOf, BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance).getPreviousNodes().singleResult());
    }

    @Test
    public void testParallelize() {
        System.out.print("Testing parallelize");

        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        FlowNode startingNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode endingNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        String startingNodeId = startingNode.getId();
        String endingNodeId = endingNode.getId();

        try {
            modelInstance1.parallelize(startingNodeId, endingNodeId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        FlowNode divergentGateway = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode convergentGateway = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Verifies that the parallel gateways were correctly created
        assert divergentGateway instanceof ParallelGateway;
        assert convergentGateway instanceof  ParallelGateway;

        // Verifies that all nodes inside the input fragment are connected only to the created gateways
        assertEquals(divergentGateway, startingNode.getPreviousNodes().singleResult());
        assertEquals(divergentGateway, endingNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway, startingNode.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway, endingNode.getSucceedingNodes().singleResult());

    }

    @Test
    public void testSplit() {
        System.out.println("Testing split");
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

        Bpmnt.validateModel(modelInstance1);
    }

    @Test
    public void testInsertSingleNodeInSeries() {
        System.out.println("Testing insert in series (single node)");

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

        Bpmnt.validateModel(modelInstance1);

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

        Bpmnt.validateModel(modelInstance3);


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

        Bpmnt.validateModel(modelInstance5);
    }

    @Test
    public void testInsertSingleNodeInParallel() {
        System.out.println("Testing insert in parallel (single node)");

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

        Bpmnt.validateModel(modelInstance1);
    }

    @Test
    public void testInsertFragmentInSeries() {
        System.out.println("Testing insert in series (fragment)");

        // First try (afterOf == null)
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf1 = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf1 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the models
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        String firstNodeId2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        modelInstance1.insert(null, beforeOf1, modelInstance2);

        FlowNode firstNode2 = modelInstance1.getModelElementById(firstNodeId2);
        FlowNode lastNode2 = modelInstance1.getModelElementById(lastNodeId2);

        // Check if the fragment was correctly created and placed in the process
        assertEquals(lastNode1, firstNode2.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, lastNode2.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance1);

        // Second try (beforeOf == null)
        BpmntModelInstance modelInstance3 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance4 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf2 = modelInstance3.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf2 = modelInstance3.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the models
        FlowNode firstNode3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode lastNode3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        String firstNodeId4 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance4).getId();
        String lastNodeId4 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance4).getId();

        modelInstance3.insert(afterOf2, null, modelInstance4);

        FlowNode firstNode4 = modelInstance3.getModelElementById(firstNodeId4);
        FlowNode lastNode4 = modelInstance3.getModelElementById(lastNodeId4);

        // Check if the fragment was correctly created and placed in the process
        assertEquals(afterOf2, firstNode4.getPreviousNodes().singleResult());
        assertEquals(firstNode3, lastNode4.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance3);

        // Third try (afterOf and beforeOf nodes set)
        BpmntModelInstance modelInstance5 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance6 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance5);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance5);

        String firstNodeId6 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance6).getId();
        String lastNodeId6 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance6).getId();

        modelInstance5.insert(afterOf3, beforeOf3, modelInstance6);

        FlowNode firstNode6 =  modelInstance5.getModelElementById(firstNodeId6);
        FlowNode lastNode6 =  modelInstance5.getModelElementById(lastNodeId6);

        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf3, firstNode6.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, lastNode6.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance5);
    }

    @Test
    public void testInsertFragmentInParallel() {
        System.out.println("Testing insert in parallel (fragment)");

        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        StartEvent afterOf = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        String firstNodeId2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        modelInstance1.insert(afterOf, beforeOf, modelInstance2);

        FlowNode firstNode2 = modelInstance1.getModelElementById(firstNodeId2);
        FlowNode lastNode2 = modelInstance1.getModelElementById(lastNodeId2);

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

        assertEquals(divergingGateway, firstNode1.getPreviousNodes().singleResult());
        assertEquals(divergingGateway, firstNode2.getPreviousNodes().singleResult());

        assertEquals(convergingGateway, lastNode1.getSucceedingNodes().singleResult());
        assertEquals(convergingGateway, lastNode2.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance1);
   }

    @Test
    public void testConditionalInsertSingleNode() {
        System.out.println("Testing conditional insert (single node)");

        // First try (nodes in succession)
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);

        // Target node
        Task taskToInsert1 = modelInstance2.getModelElementsByType(Task.class).iterator().next();

        String condition1 = "Some condition";

        modelInstance1.conditionalInsert(afterOf1, beforeOf1, taskToInsert1, condition1, true);

        Task insertedTask1 = modelInstance1.getModelElementById(taskToInsert1.getId());

        FlowNode conditionalGateway1 = afterOf1.getSucceedingNodes().singleResult();
        FlowNode convergentGateway1 = beforeOf1.getPreviousNodes().singleResult();

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway1, insertedTask1.getPreviousNodes().singleResult());
        assertEquals(convergentGateway1, insertedTask1.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway1, conditionalGateway1.getSucceedingNodes().filterByType(ParallelGateway.class).singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition1, insertedTask1.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance1);


        // Second try (nodes not in succession)
        BpmntModelInstance modelInstance3 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance4 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance3);
        FlowNode beforeOf2 = BpmnElementSearcher.findEndEvent(modelInstance3);
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        // Target node
        Task taskToInsert2 = modelInstance4.getModelElementsByType(Task.class).iterator().next();

        String condition2 = "Some condition";

        modelInstance3.conditionalInsert(afterOf2, beforeOf2, taskToInsert2, condition2, true);

        Task insertedTask2 = modelInstance3.getModelElementById(taskToInsert2.getId());

        FlowNode conditionalGateway2 = afterOf2.getSucceedingNodes().singleResult();
        FlowNode convergentGateway2 = beforeOf2.getPreviousNodes().singleResult();

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway2, insertedTask2.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, insertedTask2.getSucceedingNodes().singleResult());
        assertEquals(conditionalGateway2, firstNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastNode.getSucceedingNodes().singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition2, insertedTask2.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance3);
    }

    @Test
    public void testConditionalInsertFragment() {
        System.out.println("Testing conditional insert (fragment)");

        // First try (nodes in succession)
        BpmntModelInstance modelInstance1 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance2 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);

        String firstInsertedNodeId1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastInsertedNodeId1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        String condition = "Some condition";

        modelInstance1.conditionalInsert(afterOf1, beforeOf1, modelInstance2, condition, true);

        FlowNode conditionalGateway1 = afterOf1.getSucceedingNodes().singleResult();
        FlowNode convergentGateway1 = beforeOf1.getPreviousNodes().singleResult();

        FlowNode firstInsertedNode1 = modelInstance1.getModelElementById(firstInsertedNodeId1);
        FlowNode lastInsertedNode1 = modelInstance1.getModelElementById(lastInsertedNodeId1);

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway1, firstInsertedNode1.getPreviousNodes().singleResult());
        assertEquals(convergentGateway1, lastInsertedNode1.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway1, conditionalGateway1.getSucceedingNodes().filterByType(ParallelGateway.class).singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition, firstInsertedNode1.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance1);


        // Second try (nodes not in succession)
        BpmntModelInstance modelInstance3 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance modelInstance4 = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        // Extract nodes from the model
        FlowNode afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance3);
        FlowNode beforeOf2 = BpmnElementSearcher.findEndEvent(modelInstance3);
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        String firstInsertedNodeId = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance4).getId();
        String lastInsertedNodeId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance4).getId();

        String condition2 = "Some condition";

        modelInstance3.conditionalInsert(afterOf2, beforeOf2, modelInstance4, condition2, true);

        FlowNode firstInsertedNode = modelInstance3.getModelElementById(firstInsertedNodeId);
        FlowNode lastInsertedNode = modelInstance3.getModelElementById(lastInsertedNodeId);

        FlowNode conditionalGateway2 = afterOf2.getSucceedingNodes().singleResult();
        FlowNode convergentGateway2 = beforeOf2.getPreviousNodes().singleResult();

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway2, firstInsertedNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastInsertedNode.getSucceedingNodes().singleResult());
        assertEquals(conditionalGateway2, firstNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastNode.getSucceedingNodes().singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition2, firstInsertedNode.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance3);
    }

    @Test
    public void testFragmentValidation() {
        System.out.println("Testing fragment validation");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));

        StartEvent afterOf = BpmnElementSearcher.findStartEvent(modelInstance);
        FlowNode beforeOf = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode targetStartingNode = beforeOf.getSucceedingNodes().singleResult().getSucceedingNodes().singleResult();
        FlowNode targetEndingNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance).getPreviousNodes().singleResult();

        // Moving the parallel fragment to the start of the process
        modelInstance.move(targetStartingNode.getId(), targetEndingNode.getId(), afterOf.getId(), beforeOf.getId());
    }


}
