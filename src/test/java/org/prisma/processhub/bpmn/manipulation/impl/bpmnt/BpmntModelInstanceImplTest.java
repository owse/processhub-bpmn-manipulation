package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
import org.prisma.processhub.bpmn.manipulation.util.BpmnFragmentHandler;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BpmntModelInstanceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private TailorableBpmnModelInstance tailorableSimpleModel;
    private TailorableBpmnModelInstance tailorableSimpleModel2;
    private TailorableBpmnModelInstance tailorableParallelModel;
    private TailorableBpmnModelInstance tailorableParallelModel2;

    private BpmntModelInstance simpleModel;
    private BpmntModelInstance simpleModel2;
    private BpmntModelInstance parallelModel;
    private BpmntModelInstance parallelModel2;
    private BpmntModelInstance subprocessModel;
    private BpmntModelInstance fragmentModel;

    // Load diagrams before each test
    @Before
    public void loadDiagrams() {
        tailorableSimpleModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        tailorableSimpleModel2 = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        tailorableParallelModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        tailorableParallelModel2 = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));

        simpleModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        simpleModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        parallelModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        parallelModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
        subprocessModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
        fragmentModel = Bpmnt.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
    }

    // Tests naming convention: methodName_StateUnderTest_ExpectedBehavior

    // Test cases for the 'extend' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void extend_ModelExtended_BpmntModelCreated() {
        BpmntModelInstance bpmntModelInstance = tailorableSimpleModel.extend();

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(bpmntModelInstance);

        // Check if the process ID was correctly set
        assertEquals(
                "BPMNt_" + BpmnElementSearcher.findFirstProcess(simpleModel).getId(),
                BpmnElementSearcher.findFirstProcess(bpmntModelInstance).getId()
        );

        // Check if only one Extend operation object was added to the BPMNt log
        assert(bpmntModelInstance.getBpmntLog().iterator().next() instanceof Extend);
        assertEquals(1, bpmntModelInstance.getBpmntLog().size());

        Collection<FlowElement> baseFlowElements = simpleModel.getModelElementsByType(FlowElement.class);

        // Verify that every flow element was successfully copied to the bpmntModelInstance
        for (FlowElement fe: baseFlowElements) {
            assert(bpmntModelInstance.contains(fe));
        }

    }

    // Test cases for the 'contribute' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void contribute_ElementFromSameModel_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Add element from same model
        FlowElement element = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);
        exception.expect(IllegalArgumentException.class);
        modelInstance.contribute(element);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
    }

    @Test
    public void contribute_ParentFromOtherModel_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Add element from this model with parent from another
        FlowElement element = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        exception.expect(ElementNotFoundException.class);
        modelInstance.contribute(foreignElement.getParentElement(), element);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
    }

    @Test
    public void contribute_ElementFromOtherModel_ElementAdded() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Add element from another model
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        modelInstance.contribute(foreignElement);

        // Verify new element in model has same properties
        FlowElement newElement = modelInstance.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);
    }

    @Test
    public void contribute_CreatedElement_ElementAdded() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        assert modelInstance.getBpmntLog().iterator().next() instanceof Extend;
        assertEquals(1, modelInstance.getBpmntLog().size());

        // Add newly created element
        UserTask newTask = parallelModel.newInstance(UserTask.class);
        newTask.setId("my_new_id");
        newTask.setName("new_name");
        modelInstance.contribute(newTask);

        // Verify new element has been created in model
        FlowElement newElement = modelInstance.getModelElementById(newTask.getId());
        assertEquals(newElement.getId(), newTask.getId());
        assertEquals(newElement.getName(), newTask.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Contribute;

        FlowNode newElementFromLog = (FlowNode) ((Contribute) modelInstance.getBpmntLog().get(1)).getNewElement();

        assertEquals(newElement.getId(), newElementFromLog.getId());
        assertEquals(newElement.getName(), newElementFromLog.getName());
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    @Test
    public void contribute_SameElementTwice_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Add foreign element
        FlowElement foreignElement = BpmnElementSearcher.findFlowNodeAfterStartEvent(parallelModel);
        modelInstance.contribute(foreignElement);

        // Verify element was added with same properties
        FlowElement newElement = modelInstance.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());

        // Add with different id
        String oldId = foreignElement.getId();
        modelInstance.setUniqueId(foreignElement);
        modelInstance.contribute(foreignElement);

        // Verify element was added with same properties
        newElement = modelInstance.getModelElementById(foreignElement.getId());
        assertEquals(newElement.getId(), foreignElement.getId());
        assertEquals(newElement.getName(), foreignElement.getName());
        assertNotEquals(oldId, newElement.getId());

        // Add again without changing id
        exception.expect(IllegalArgumentException.class);
        modelInstance.contribute(foreignElement);

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Contribute;
        assert modelInstance.getBpmntLog().get(2) instanceof Contribute;

        FlowNode newElementFromLog1 = (FlowNode) ((Contribute) modelInstance.getBpmntLog().get(1)).getNewElement();
        FlowNode newElementFromLog2 = (FlowNode) ((Contribute) modelInstance.getBpmntLog().get(2)).getNewElement();

        assertEquals(oldId, newElementFromLog1.getId());
        assertEquals(newElement.getId(), newElementFromLog2.getId());
        assertEquals(3, modelInstance.getBpmntLog().size());
    }


    // Test cases for the 'suppress' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void suppress_ElementFromModel_ElementRemoved() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Get the first flow element
        FlowElement flowElementToRemove = modelInstance.getModelElementsByType(FlowElement.class).iterator().next();
        String flowElementToRemoveId = flowElementToRemove.getId();

        // Remove the first flow element
        modelInstance.suppress(flowElementToRemoveId);

        // Verify if the flow element has been removed
        assertEquals(modelInstance.getModelElementById(flowElementToRemoveId), null);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Suppress;

        String suppressedElementId = ((Suppress) modelInstance.getBpmntLog().get(1)).getSuppressedElementId();

        assertEquals(flowElementToRemoveId, suppressedElementId);
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void suppress_ElementFromOtherModel_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel2.extend();

        // Get first element from other model
        FlowElement foreignElement = tailorableParallelModel.getModelElementsByType(FlowElement.class).iterator().next();

        exception.expect(ElementNotFoundException.class);
        modelInstance.suppress(foreignElement);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assertEquals(1, modelInstance.getBpmntLog().size());
    }

    @Test
    public void suppress_ElementFromModelTwice_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Get the first flow element
        FlowElement flowElementToRemove = modelInstance.getModelElementsByType(FlowElement.class).iterator().next();
        String flowElementToRemoveId = flowElementToRemove.getId();

        // Remove element
        modelInstance.suppress(flowElementToRemoveId);
        assertEquals(null, modelInstance.getModelElementById(flowElementToRemoveId));

        // Remove twice
        exception.expect(ElementNotFoundException.class);
        modelInstance.suppress(flowElementToRemove);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Suppress;

        String suppressedElementId = ((Suppress) modelInstance.getBpmntLog().get(1)).getSuppressedElementId();

        assertEquals(flowElementToRemoveId, suppressedElementId);
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    // Test cases for the 'modify' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void modify_ExistingProperty_PropertyModified() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        FlowElement element = modelInstance.getModelElementsByType(FlowElement.class).iterator().next();
        String property = "name";
        String name = "my new name";
        modelInstance.modify(element, property, name);
        assertEquals(element.getName(), name);
        assertEquals(element.getAttributeValue(property), name);

        Bpmn.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Modify;

        String modifiedElementId = ((Modify) modelInstance.getBpmntLog().get(1)).getModifiedElementId();
        String propertyFromLog = ((Modify) modelInstance.getBpmntLog().get(1)).getProperty();
        String valueFromLog = ((Modify) modelInstance.getBpmntLog().get(1)).getValue();

        assertEquals(element.getId(), modifiedElementId);
        assertEquals(property, propertyFromLog);
        assertEquals(name, valueFromLog);
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    @Test
    public void modify_NewProperty_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        FlowElement element = modelInstance.getModelElementsByType(FlowElement.class).iterator().next();
        String property = "newProperty";
        String name = "my new name";
        modelInstance.modify(element.getId(), property, name);
        assertEquals(element.getAttributeValue(property), name);

        exception.expect(ModelValidationException.class);
        Bpmn.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assertEquals(1, modelInstance.getBpmntLog().size());
    }




    // Test cases for the 'rename' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void rename_ElementFromModel_ElementRenamed() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Select a flow node to rename
        FlowNode flowNodeToRename = modelInstance.getModelElementsByType(FlowNode.class).iterator().next();
        String flowNodeToRenameId = flowNodeToRename.getId();
        String newName = "New Name";

        // Checks if the target node has been renamed correctly
        modelInstance.rename(flowNodeToRename, newName);
        FlowElement renamedElement = modelInstance.getModelElementById(flowNodeToRenameId);
        assertEquals(newName, renamedElement.getName());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);

        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof Rename;

        String elementId = ((Rename) modelInstance.getBpmntLog().get(1)).getElementId();
        String newNameFromLog = ((Rename) modelInstance.getBpmntLog().get(1)).getNewName();

        assertEquals(renamedElement.getId(), elementId);
        assertEquals(newName, newNameFromLog);
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    @Test
    public void rename_ElementFromOtherModel_ExceptionThrown() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        // Select a flow node to rename
        FlowNode flowNodeToRename = simpleModel2.getModelElementsByType(FlowNode.class).iterator().next();
        String newName = "New Name";

        exception.expect(ElementNotFoundException.class);
        modelInstance.rename(flowNodeToRename, newName);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assertEquals(1, modelInstance.getBpmntLog().size());
    }


    // Test cases for the 'delete' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void delete_SingleElement_ElementRemoved() {
        BpmntModelInstance modelInstance = tailorableParallelModel.extend();

        // Load data
        FlowNode flowNodeToDelete = modelInstance.getModelElementsByType(Task.class).iterator().next();
        String flowNodeToDeleteId = flowNodeToDelete.getId();
        int initialNumberFlowNodes = modelInstance.getModelElementsByType(FlowNode.class).size();

        // Find start and end events
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(modelInstance);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(modelInstance);

        modelInstance.delete(flowNodeToDeleteId);
        FlowNode remainingTask = modelInstance.getModelElementsByType(Task.class).iterator().next();

        // Checks if the number of remaining flow nodes is correct
        assertEquals(initialNumberFlowNodes - 3, modelInstance.getModelElementsByType(FlowNode.class).size());

        // Checks if the split parallel gateway has been removed and if the remaining task is connected to the start event
        assertEquals(startEvent, remainingTask.getPreviousNodes().singleResult());

        // Checks if the join parallel gateway has been removed and if the remaining task is connected to the end event
        assertEquals(endEvent, remainingTask.getSucceedingNodes().singleResult());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof DeleteNode;

        String deletedNodeId = ((DeleteNode) modelInstance.getBpmntLog().get(1)).getNodeId();

        assertEquals(flowNodeToDeleteId, deletedNodeId);
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    @Test
    public void delete_ValidFragment_FragmentRemoved() {
        BpmntModelInstance modelInstance = tailorableParallelModel2.extend();

        // Loading data
        ParallelGateway splitGateway = modelInstance.getModelElementById("ParallelGateway_1c6p3yf");
        ParallelGateway joinGateway = modelInstance.getModelElementById("ParallelGateway_07aj32a");
        Task parallelTaskA = modelInstance.getModelElementById("Task_1liqzit");
        Task parallelTaskB = modelInstance.getModelElementById("Task_0dae65c");
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);
        int initialNumberFlowNodes = modelInstance.getModelElementsByType(FlowNode.class).size();


        // Make sure the fragment is valid
        Collection<FlowNode> fragment = BpmnFragmentHandler.mapProcessFragment(splitGateway, joinGateway);
        BpmnFragmentHandler.validateDeleteProcessFragment(fragment);

        // Deleting the parallel fragment
        modelInstance.delete(splitGateway, joinGateway);

        // Verifies that every node in the selected fragment has been deleted
        assert modelInstance.getModelElementsByType(ParallelGateway.class).isEmpty();
        assertEquals(null, modelInstance.getModelElementById(parallelTaskA.getId()));
        assertEquals(null, modelInstance.getModelElementById(parallelTaskB.getId()));

        // Verifies that the first task is connected to the last task
        assertEquals(lastNode, firstNode.getSucceedingNodes().singleResult());

        // Checks if the number of remaining flow nodes is correct
        assertEquals(initialNumberFlowNodes - fragment.size(), modelInstance.getModelElementsByType(FlowNode.class).size());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof DeleteFragment;

        String deletedStartNodeId = ((DeleteFragment) modelInstance.getBpmntLog().get(1)).getStartingNodeId();
        String deletedEndNodeId = ((DeleteFragment) modelInstance.getBpmntLog().get(1)).getEndingNodeId();

        assertEquals("ParallelGateway_1c6p3yf", deletedStartNodeId);
        assertEquals("ParallelGateway_07aj32a", deletedEndNodeId);
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testReplaceNodeWithNode() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        FlowNode replacingTask = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();
        FlowNode replacedTask = modelInstance.getModelElementsByType(Task.class).iterator().next();

        String replacingTaskId =replacingTask.getId();
        String replacedTaskId =replacedTask.getId();

        FlowNode previousNode = replacedTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask.getSucceedingNodes().singleResult();

        modelInstance.replace(replacedTask.getId(), replacingTask);

        FlowNode newTask = modelInstance.getModelElementById(replacingTask.getId());

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask.getSucceedingNodes().singleResult());
        assertEquals(replacingTask.getId(), newTask.getId());

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof ReplaceNodeWithNode;

        String replacedNodeId = ((ReplaceNodeWithNode) modelInstance.getBpmntLog().get(1)).getReplacedNodeId();
        FlowNode replacingNode = ((ReplaceNodeWithNode) modelInstance.getBpmntLog().get(1)).getReplacingNode();

        assertEquals(replacedTaskId, replacedNodeId);
        assertEquals(replacingTaskId, replacingNode.getId());
        assertEquals(replacingTask.getClass(), replacingNode.getClass());
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testReplaceNodeWithFragment() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        FlowNode replacedTask = modelInstance.getModelElementsByType(Task.class).iterator().next();
        String replacedTaskId = replacedTask.getId();

        FlowNode previousNode = replacedTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask.getSucceedingNodes().singleResult();

        String startingNodeId = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String endingNodeId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        modelInstance.replace(replacedTask.getId(), tailorableSimpleModel2);

        FlowNode startingNode = modelInstance.getModelElementById(startingNodeId);
        FlowNode endingNode = modelInstance.getModelElementById(endingNodeId);

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, startingNode.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, endingNode.getSucceedingNodes().singleResult());
        assertEquals(startingNodeId, startingNode.getId());
        assertEquals(endingNodeId, endingNode.getId());
        assertEquals(3, modelInstance.getModelElementsByType(Task.class).size());

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof ReplaceNodeWithFragment;

        String replacedNodeId = ((ReplaceNodeWithFragment) modelInstance.getBpmntLog().get(1)).getReplacedNodeId();
        BpmnModelInstance replacingFragment = ((ReplaceNodeWithFragment) modelInstance.getBpmntLog().get(1)).getReplacingFragment();

        assertEquals(replacedTaskId, replacedNodeId);
        assertEquals(
                BpmnElementSearcher.findFirstProcess(tailorableSimpleModel2).getId(),
                BpmnElementSearcher.findFirstProcess(replacingFragment).getId()
        );
        assertEquals(
                tailorableSimpleModel2.getModelElementsByType(FlowElement.class).size(),
                replacingFragment.getModelElementsByType(FlowElement.class).size()
        );
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testReplaceFragmentWithNode() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        FlowNode replacingTask = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();
        FlowNode replacedTask1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode replacedTask2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);

        String replacedTask1Id = replacedTask1.getId();
        String replacedTask2Id = replacedTask2.getId();

        FlowNode previousNode = replacedTask1.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask2.getSucceedingNodes().singleResult();

        modelInstance.replace(replacedTask1.getId(), replacedTask2.getId(), replacingTask);

        FlowNode newTask = modelInstance.getModelElementById(replacingTask.getId());

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask.getSucceedingNodes().singleResult());
        assertEquals(replacingTask.getId(), newTask.getId());
        assertEquals(1, modelInstance.getModelElementsByType(Task.class).size());

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof ReplaceFragmentWithNode;

        String replacedNode1Id = ((ReplaceFragmentWithNode) modelInstance.getBpmntLog().get(1)).getStartingNodeId();
        String replacedNode2Id = ((ReplaceFragmentWithNode) modelInstance.getBpmntLog().get(1)).getEndingNodeId();
        FlowNode replacingNode = ((ReplaceFragmentWithNode) modelInstance.getBpmntLog().get(1)).getReplacingNode();

        assertEquals(replacedTask1Id, replacedNode1Id);
        assertEquals(replacedTask2Id, replacedNode2Id);
        assertEquals(replacingTask.getClass(), replacingNode.getClass());
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testReplaceFragmentWithFragment() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        String replacingTaskId1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String replacingTaskId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        FlowNode replacedTask1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode replacedTask2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);

        String replacedTask1Id = replacedTask1.getId();
        String replacedTask2Id = replacedTask2.getId();

        FlowNode previousNode = replacedTask1.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask2.getSucceedingNodes().singleResult();

        modelInstance.replace(replacedTask1.getId(), replacedTask2.getId(), tailorableSimpleModel2);

        FlowNode newTask1 = modelInstance.getModelElementById(replacingTaskId1);
        FlowNode newTask2 = modelInstance.getModelElementById(replacingTaskId2);

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask1.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask2.getSucceedingNodes().singleResult());
        assertEquals(replacingTaskId1, newTask1.getId());
        assertEquals(replacingTaskId2, newTask2.getId());
        assertEquals(2, modelInstance.getModelElementsByType(Task.class).size());
        assertEquals(3, modelInstance.getModelElementsByType(SequenceFlow.class).size());

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof ReplaceFragmentWithFragment;

        String replacedNode1Id = ((ReplaceFragmentWithFragment) modelInstance.getBpmntLog().get(1)).getStartingNodeId();
        String replacedNode2Id = ((ReplaceFragmentWithFragment) modelInstance.getBpmntLog().get(1)).getEndingNodeId();
        BpmnModelInstance replacingFragment = ((ReplaceFragmentWithFragment) modelInstance.getBpmntLog().get(1)).getReplacingFragment();

        assertEquals(replacedTask1Id, replacedNode1Id);
        assertEquals(replacedTask2Id, replacedNode2Id);
        assertEquals(
                BpmnElementSearcher.findFirstProcess(tailorableSimpleModel2).getId(),
                BpmnElementSearcher.findFirstProcess(replacingFragment).getId()
        );
        assertEquals(
                tailorableSimpleModel2.getModelElementsByType(FlowElement.class).size(),
                replacingFragment.getModelElementsByType(FlowElement.class).size()
        );
        assertEquals(2, modelInstance.getBpmntLog().size());
    }

    @Test
    public void testMoveSingleNode() {

        // First try: both position arguments set
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

        StartEvent afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode target1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Switching the first task with the second task
        modelInstance1.move(target1.getId(), afterOf1.getId(), beforeOf1.getId());

        // Verifies if the tasks were successfully switched
        assertEquals(target1, afterOf1.getSucceedingNodes().singleResult());
        assertEquals(target1, beforeOf1.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, BpmnElementSearcher.findEndEvent(modelInstance1).getPreviousNodes().singleResult());

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof MoveNode;

        String target1Id = ((MoveNode) modelInstance1.getBpmntLog().get(1)).getNodeId();
        String afterOf1Id = ((MoveNode) modelInstance1.getBpmntLog().get(1)).getNewPositionAfterOfId();
        String beforeOf1Id = ((MoveNode) modelInstance1.getBpmntLog().get(1)).getNewPositionBeforeOfId();

        assertEquals(target1.getId(), target1Id);
        assertEquals(afterOf1.getId(), afterOf1Id);
        assertEquals(beforeOf1.getId(), beforeOf1Id);
        assertEquals(2, modelInstance1.getBpmntLog().size());


        // Second try: afterOf position set
        BpmntModelInstance modelInstance2 = tailorableSimpleModel.extend();

        StartEvent afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode beforeOf2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode target2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        // Switching the first task with the second task
        modelInstance2.move(target2.getId(), afterOf2.getId(), null);

        // Verifies if the tasks were successfully switched
        assertEquals(target2, afterOf2.getSucceedingNodes().singleResult());
        assertEquals(target2, beforeOf2.getPreviousNodes().singleResult());
        assertEquals(beforeOf2, BpmnElementSearcher.findEndEvent(modelInstance2).getPreviousNodes().singleResult());

        // Verify BPMNt log consistency
        assert modelInstance2.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance2.getBpmntLog().get(1) instanceof MoveNode;

        String target2Id = ((MoveNode) modelInstance2.getBpmntLog().get(1)).getNodeId();
        String afterOf2Id = ((MoveNode) modelInstance2.getBpmntLog().get(1)).getNewPositionAfterOfId();
        String beforeOf2Id = ((MoveNode) modelInstance2.getBpmntLog().get(1)).getNewPositionBeforeOfId();

        assertEquals(target2.getId(), target2Id);
        assertEquals(afterOf2.getId(), afterOf2Id);
        assertEquals(null, beforeOf2Id);
        assertEquals(2, modelInstance2.getBpmntLog().size());


        // Third try: beforeOf position set
        BpmntModelInstance modelInstance3 = tailorableSimpleModel.extend();

        StartEvent afterOf3 = BpmnElementSearcher.findStartEvent(modelInstance3);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode target3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        // Switching the first task with the second task
        modelInstance3.move(target3.getId(), null, beforeOf3.getId());

        // Verifies if the tasks were successfully switched
        assertEquals(target3, afterOf3.getSucceedingNodes().singleResult());
        assertEquals(target3, beforeOf3.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, BpmnElementSearcher.findEndEvent(modelInstance3).getPreviousNodes().singleResult());

        // Verify BPMNt log consistency
        assert modelInstance3.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance3.getBpmntLog().get(1) instanceof MoveNode;

        String target3Id = ((MoveNode) modelInstance3.getBpmntLog().get(1)).getNodeId();
        String afterOf3Id = ((MoveNode) modelInstance3.getBpmntLog().get(1)).getNewPositionAfterOfId();
        String beforeOf3Id = ((MoveNode) modelInstance3.getBpmntLog().get(1)).getNewPositionBeforeOfId();

        assertEquals(target3.getId(), target3Id);
        assertEquals(null, afterOf3Id);
        assertEquals(beforeOf3.getId(), beforeOf3Id);
        assertEquals(2, modelInstance1.getBpmntLog().size());


    }

    @Test
    public void testMoveFragment() {
        BpmntModelInstance modelInstance = tailorableParallelModel2.extend();

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

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof MoveFragment;

        String targetStartingNodeId = ((MoveFragment) modelInstance.getBpmntLog().get(1)).getStartingNodeId();
        String targetEndingNodeId = ((MoveFragment) modelInstance.getBpmntLog().get(1)).getEndingNodeId();
        String afterOfId = ((MoveFragment) modelInstance.getBpmntLog().get(1)).getNewPositionAfterOfId();
        String beforeOfId = ((MoveFragment) modelInstance.getBpmntLog().get(1)).getNewPositionBeforeOfId();

        assertEquals(targetStartingNode.getId(), targetStartingNodeId);
        assertEquals(targetEndingNode.getId(), targetEndingNodeId);
        assertEquals(afterOf.getId(), afterOfId);
        assertEquals(beforeOf.getId(), beforeOfId);
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testParallelize() {
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

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

        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof Parallelize;

        String parallelizedStartNodeId = ((Parallelize) modelInstance1.getBpmntLog().get(1)).getStartingNodeId();
        String parallelizedEndNodeId = ((Parallelize) modelInstance1.getBpmntLog().get(1)).getEndingNodeId();

        assertEquals(startingNodeId, parallelizedStartNodeId);
        assertEquals(endingNodeId, parallelizedEndNodeId);
        assertEquals(2, modelInstance1.getBpmntLog().size());



    }

    @Test
    public void testSplit() {
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();
        BpmntModelInstance modelInstance2 = simpleModel2;

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

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof Split;

        String splitTaskId = ((Split) modelInstance1.getBpmntLog().get(1)).getTaskId();
        BpmnModelInstance subProcessModel = ((Split) modelInstance1.getBpmntLog().get(1)).getNewSubProcessModel();

        assertEquals(targetId, splitTaskId);
        assertEquals(subProcess.getFlowElements().size(), subProcess.getFlowElements().size());
        assertEquals(2, modelInstance1.getBpmntLog().size());
    }

    @Test
    public void testInsertSingleNodeInSeries() {

        // First try (afterOf == null)
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

        StartEvent afterOf1 = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf1 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        // Target node
        Task taskToInsert1 = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

        modelInstance1.insert(null, beforeOf1, taskToInsert1);

        Task insertedTask1 = modelInstance1.getModelElementById(taskToInsert1.getId());

        // Check if the node was correctly created and placed in the process
        assertEquals(lastNode1, insertedTask1.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, insertedTask1.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance1);

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof InsertNode;

        String afterOf1Id = ((InsertNode) modelInstance1.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf1Id = ((InsertNode) modelInstance1.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode1 = ((InsertNode) modelInstance1.getBpmntLog().get(1)).getFlowNodeToInsert();

        assertEquals(null, afterOf1Id);
        assertEquals(beforeOf1.getId(), beforeOf1Id);
        assertEquals(insertedTask1.getClass(), insertedNode1.getClass());
        assertEquals(insertedTask1.getId(), insertedNode1.getId());
        assertEquals(2, modelInstance1.getBpmntLog().size());


        // Second try (beforeOf == null)
        BpmntModelInstance modelInstance2 = tailorableSimpleModel.extend();

        StartEvent afterOf2 = modelInstance2.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf2 = modelInstance2.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode lastNode2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        // Target node
        Task taskToInsert2 = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

        modelInstance2.insert(afterOf2, null, taskToInsert2);

        Task insertedTask2 = modelInstance2.getModelElementById(taskToInsert2.getId());


        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf2, insertedTask2.getPreviousNodes().singleResult());
        assertEquals(firstNode2, insertedTask2.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance2);

        // Verify BPMNt log consistency
        assert modelInstance2.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance2.getBpmntLog().get(1) instanceof InsertNode;

        String afterOf2Id = ((InsertNode) modelInstance2.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf2Id = ((InsertNode) modelInstance2.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode2 = ((InsertNode) modelInstance2.getBpmntLog().get(1)).getFlowNodeToInsert();

        assertEquals(afterOf2.getId(), afterOf2Id);
        assertEquals(null, beforeOf2Id);
        assertEquals(insertedTask2.getClass(), insertedNode2.getClass());
        assertEquals(insertedTask2.getId(), insertedNode2.getId());
        assertEquals(2, modelInstance2.getBpmntLog().size());


        // Third try (afterOf and beforeOf nodes set)
        BpmntModelInstance modelInstance3 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        // Target node
        Task taskToInsert3 = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

        modelInstance3.insert(afterOf3, beforeOf3, taskToInsert3);

        Task insertedTask3 = modelInstance3.getModelElementById(taskToInsert3.getId());


        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf3, insertedTask3.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, insertedTask3.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance3);


        // Verify BPMNt log consistency
        assert modelInstance3.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance3.getBpmntLog().get(1) instanceof InsertNode;

        String afterOf3Id = ((InsertNode) modelInstance3.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf3Id = ((InsertNode) modelInstance3.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode3 = ((InsertNode) modelInstance3.getBpmntLog().get(1)).getFlowNodeToInsert();

        assertEquals(afterOf3.getId(), afterOf3Id);
        assertEquals(beforeOf3.getId(), beforeOf3Id);
        assertEquals(insertedTask3.getClass(), insertedNode3.getClass());
        assertEquals(insertedTask3.getId(), insertedNode3.getId());
        assertEquals(2, modelInstance3.getBpmntLog().size());

    }

    @Test
    public void testInsertSingleNodeInParallel() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        StartEvent afterOf = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf = modelInstance.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);

        // Target node
        Task taskToInsert = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

        modelInstance.insert(afterOf, beforeOf, taskToInsert);

        Task insertedTask = modelInstance.getModelElementById(taskToInsert.getId());

        Collection<ParallelGateway> parallelGateways = modelInstance.getModelElementsByType(ParallelGateway.class);
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

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof InsertNode;

        String afterOfId = ((InsertNode) modelInstance.getBpmntLog().get(1)).getAfterOfId();
        String beforeOfId = ((InsertNode) modelInstance.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode = ((InsertNode) modelInstance.getBpmntLog().get(1)).getFlowNodeToInsert();

        assertEquals(afterOf.getId(), afterOfId);
        assertEquals(beforeOfId, beforeOfId);
        assertEquals(insertedTask.getClass(), insertedNode.getClass());
        assertEquals(insertedTask.getId(), insertedNode.getId());
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testInsertFragmentInSeries() {
        // First try (afterOf == null)
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

        StartEvent afterOf1 = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf1 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the models
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        String firstNodeId2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastNodeId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        modelInstance1.insert(null, beforeOf1, tailorableSimpleModel2);

        FlowNode firstNode2 = modelInstance1.getModelElementById(firstNodeId2);
        FlowNode lastNode2 = modelInstance1.getModelElementById(lastNodeId2);

        // Check if the fragment was correctly created and placed in the process
        assertEquals(lastNode1, firstNode2.getPreviousNodes().singleResult());
        assertEquals(beforeOf1, lastNode2.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance1);

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof InsertFragment;

        String afterOf1Id = ((InsertFragment) modelInstance1.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf1Id = ((InsertFragment) modelInstance1.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment1 = ((InsertFragment) modelInstance1.getBpmntLog().get(1)).getFragmentToInsert();

        assertEquals(null, afterOf1Id);
        assertEquals(beforeOf1.getId(), beforeOf1Id);
        assert insertedFragment1.getModelElementById(firstNode2.getId()) != null;
        assert insertedFragment1.getModelElementById(lastNode2.getId()) != null;
        assertEquals(2, modelInstance1.getBpmntLog().size());


        // Second try (beforeOf == null)
        BpmntModelInstance modelInstance2 = tailorableSimpleModel.extend();

        StartEvent afterOf2 = modelInstance2.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf2 = modelInstance2.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the models
        FlowNode firstNode3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode lastNode3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        String firstNodeId4 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastNodeId4 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        modelInstance2.insert(afterOf2, null, tailorableSimpleModel2);

        FlowNode firstNode4 = modelInstance2.getModelElementById(firstNodeId4);
        FlowNode lastNode4 = modelInstance2.getModelElementById(lastNodeId4);

        // Check if the fragment was correctly created and placed in the process
        assertEquals(afterOf2, firstNode4.getPreviousNodes().singleResult());
        assertEquals(firstNode3, lastNode4.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance2);

        // Verify BPMNt log consistency
        assert modelInstance2.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance2.getBpmntLog().get(1) instanceof InsertFragment;

        String afterOf2Id = ((InsertFragment) modelInstance2.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf2Id = ((InsertFragment) modelInstance2.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment2 = ((InsertFragment) modelInstance2.getBpmntLog().get(1)).getFragmentToInsert();

        assertEquals(afterOf2.getId(), afterOf2Id);
        assertEquals(null, beforeOf2Id);
        assert insertedFragment2.getModelElementById(firstNode4.getId()) != null;
        assert insertedFragment2.getModelElementById(lastNode4.getId()) != null;
        assertEquals(2, modelInstance2.getBpmntLog().size());


        // Third try (afterOf and beforeOf nodes set)
        BpmntModelInstance modelInstance3 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode beforeOf3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);

        String firstNodeId6 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastNodeId6 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        modelInstance3.insert(afterOf3, beforeOf3, tailorableSimpleModel2);

        FlowNode firstNode6 =  modelInstance3.getModelElementById(firstNodeId6);
        FlowNode lastNode6 =  modelInstance3.getModelElementById(lastNodeId6);

        // Check if the node was correctly created and placed in the process
        assertEquals(afterOf3, firstNode6.getPreviousNodes().singleResult());
        assertEquals(beforeOf3, lastNode6.getSucceedingNodes().singleResult());

        Bpmnt.validateModel(modelInstance3);

        // Verify BPMNt log consistency
        assert modelInstance3.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance3.getBpmntLog().get(1) instanceof InsertFragment;

        String afterOf3Id = ((InsertFragment) modelInstance3.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf3Id = ((InsertFragment) modelInstance3.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment3 = ((InsertFragment) modelInstance3.getBpmntLog().get(1)).getFragmentToInsert();

        assertEquals(afterOf3.getId(), afterOf3Id);
        assertEquals(beforeOf3.getId(), beforeOf3Id);
        assert insertedFragment3.getModelElementById(firstNode6.getId()) != null;
        assert insertedFragment3.getModelElementById(lastNode6.getId()) != null;
        assertEquals(2, modelInstance3.getBpmntLog().size());

    }

    @Test
    public void testInsertFragmentInParallel() {
        BpmntModelInstance modelInstance = tailorableSimpleModel.extend();

        StartEvent afterOf = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
        EndEvent beforeOf = modelInstance.getModelElementsByType(EndEvent.class).iterator().next();

        // Extract nodes from the model
        FlowNode firstNode1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode lastNode1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance);

        String firstNodeId2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastNodeId2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        modelInstance.insert(afterOf, beforeOf, tailorableSimpleModel2);

        FlowNode firstNode2 = modelInstance.getModelElementById(firstNodeId2);
        FlowNode lastNode2 = modelInstance.getModelElementById(lastNodeId2);

        Collection<ParallelGateway> parallelGateways = modelInstance.getModelElementsByType(ParallelGateway.class);
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

        Bpmnt.validateModel(modelInstance);

        // Verify BPMNt log consistency
        assert modelInstance.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance.getBpmntLog().get(1) instanceof InsertFragment;

        String afterOfId = ((InsertFragment) modelInstance.getBpmntLog().get(1)).getAfterOfId();
        String beforeOfId = ((InsertFragment) modelInstance.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment = ((InsertFragment) modelInstance.getBpmntLog().get(1)).getFragmentToInsert();

        assertEquals(afterOf.getId(), afterOfId);
        assertEquals(beforeOf.getId(), beforeOfId);
        assert insertedFragment.getModelElementById(firstNodeId2) != null;
        assert insertedFragment.getModelElementById(lastNodeId2) != null;
        assertEquals(2, modelInstance.getBpmntLog().size());

    }

    @Test
    public void testConditionalInsertSingleNode() {
        // First try (nodes in succession)
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);

        // Target node
        Task taskToInsert1 = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

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

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof ConditionalInsertNode;

        String afterOf1Id = ((ConditionalInsertNode) modelInstance1.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf1Id = ((ConditionalInsertNode) modelInstance1.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode1 = ((ConditionalInsertNode) modelInstance1.getBpmntLog().get(1)).getFlowNodeToInsert();
        String insertedCondition1 = ((ConditionalInsertNode) modelInstance1.getBpmntLog().get(1)).getCondition();
        boolean inLoopCondition1 = ((ConditionalInsertNode) modelInstance1.getBpmntLog().get(1)).isInLoop();

        assertEquals(afterOf1.getId(), afterOf1Id);
        assertEquals(beforeOf1.getId(), beforeOf1Id);
        assertEquals(condition1, insertedCondition1);
        assert inLoopCondition1;
        assertEquals(insertedTask1.getClass(), insertedNode1.getClass());
        assertEquals(insertedTask1.getId(), insertedNode1.getId());
        assertEquals(2, modelInstance1.getBpmntLog().size());


        // Second try (nodes not in succession)
        BpmntModelInstance modelInstance2 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode beforeOf2 = BpmnElementSearcher.findEndEvent(modelInstance2);
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        // Target node
        Task taskToInsert2 = tailorableSimpleModel2.getModelElementsByType(Task.class).iterator().next();

        String condition2 = "Some condition";

        modelInstance2.conditionalInsert(afterOf2, beforeOf2, taskToInsert2, condition2, true);

        Task insertedTask2 = modelInstance2.getModelElementById(taskToInsert2.getId());

        FlowNode conditionalGateway2 = afterOf2.getSucceedingNodes().singleResult();
        FlowNode convergentGateway2 = beforeOf2.getPreviousNodes().singleResult();

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway2, insertedTask2.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, insertedTask2.getSucceedingNodes().singleResult());
        assertEquals(conditionalGateway2, firstNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastNode.getSucceedingNodes().singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition2, insertedTask2.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance2);

        // Verify BPMNt log consistency
        assert modelInstance2.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance2.getBpmntLog().get(1) instanceof ConditionalInsertNode;

        String afterOf2Id = ((ConditionalInsertNode) modelInstance2.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf2Id = ((ConditionalInsertNode) modelInstance2.getBpmntLog().get(1)).getBeforeOfId();
        FlowNode insertedNode2 = ((ConditionalInsertNode) modelInstance2.getBpmntLog().get(1)).getFlowNodeToInsert();
        String insertedCondition2 = ((ConditionalInsertNode) modelInstance2.getBpmntLog().get(1)).getCondition();
        boolean inLoopCondition2 = ((ConditionalInsertNode) modelInstance2.getBpmntLog().get(1)).isInLoop();

        assertEquals(afterOf2.getId(), afterOf2Id);
        assertEquals(beforeOf2.getId(), beforeOf2Id);
        assertEquals(condition2, insertedCondition2);
        assert inLoopCondition2;
        assertEquals(insertedTask2.getClass(), insertedNode2.getClass());
        assertEquals(insertedTask2.getId(), insertedNode2.getId());
        assertEquals(2, modelInstance2.getBpmntLog().size());
    }

    @Test
    public void testConditionalInsertFragment() {
        // First try (nodes in succession)
        BpmntModelInstance modelInstance1 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf1 = BpmnElementSearcher.findStartEvent(modelInstance1);
        FlowNode beforeOf1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);

        String firstInsertedNodeId1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastInsertedNodeId1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        String condition = "Some condition";

        modelInstance1.conditionalInsert(afterOf1, beforeOf1, tailorableSimpleModel2, condition, true);

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

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof ConditionalInsertFragment;

        String afterOf1Id = ((ConditionalInsertFragment) modelInstance1.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf1Id = ((ConditionalInsertFragment) modelInstance1.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment1 = ((ConditionalInsertFragment) modelInstance1.getBpmntLog().get(1)).getFragmentToInsert();
        String insertedCondition1 = ((ConditionalInsertFragment) modelInstance1.getBpmntLog().get(1)).getCondition();
        boolean inLoopCondition1 = ((ConditionalInsertFragment) modelInstance1.getBpmntLog().get(1)).isInLoop();

        assertEquals(afterOf1.getId(), afterOf1Id);
        assertEquals(beforeOf1.getId(), beforeOf1Id);
        assert insertedFragment1.getModelElementById(firstInsertedNodeId1) != null;
        assert insertedFragment1.getModelElementById(lastInsertedNodeId1) != null;
        assertEquals(condition, insertedCondition1);
        assert inLoopCondition1;
        assertEquals(2, modelInstance1.getBpmntLog().size());


        // Second try (nodes not in succession)
        BpmntModelInstance modelInstance2 = tailorableSimpleModel.extend();

        // Extract nodes from the model
        FlowNode afterOf2 = BpmnElementSearcher.findStartEvent(modelInstance2);
        FlowNode beforeOf2 = BpmnElementSearcher.findEndEvent(modelInstance2);
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        String firstInsertedNodeId = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailorableSimpleModel2).getId();
        String lastInsertedNodeId = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailorableSimpleModel2).getId();

        String condition2 = "Some condition";

        modelInstance2.conditionalInsert(afterOf2, beforeOf2, tailorableSimpleModel2, condition2, true);

        FlowNode firstInsertedNode = modelInstance2.getModelElementById(firstInsertedNodeId);
        FlowNode lastInsertedNode = modelInstance2.getModelElementById(lastInsertedNodeId);

        FlowNode conditionalGateway2 = afterOf2.getSucceedingNodes().singleResult();
        FlowNode convergentGateway2 = beforeOf2.getPreviousNodes().singleResult();

        // Checks if the node and gateways were correctly created and placed in the process
        assertEquals(conditionalGateway2, firstInsertedNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastInsertedNode.getSucceedingNodes().singleResult());
        assertEquals(conditionalGateway2, firstNode.getPreviousNodes().singleResult());
        assertEquals(convergentGateway2, lastNode.getSucceedingNodes().singleResult());

        // Checks if the condition has been correctly assigned
        assertEquals(condition2, firstInsertedNode.getIncoming().iterator().next().getConditionExpression().getTextContent());

        Bpmnt.validateModel(modelInstance2);

        // Verify BPMNt log consistency
        assert modelInstance1.getBpmntLog().get(0) instanceof Extend;
        assert modelInstance1.getBpmntLog().get(1) instanceof ConditionalInsertFragment;

        String afterOf2Id = ((ConditionalInsertFragment) modelInstance2.getBpmntLog().get(1)).getAfterOfId();
        String beforeOf2Id = ((ConditionalInsertFragment) modelInstance2.getBpmntLog().get(1)).getBeforeOfId();
        BpmnModelInstance insertedFragment2 = ((ConditionalInsertFragment) modelInstance2.getBpmntLog().get(1)).getFragmentToInsert();
        String insertedCondition2 = ((ConditionalInsertFragment) modelInstance2.getBpmntLog().get(1)).getCondition();
        boolean inLoopCondition2 = ((ConditionalInsertFragment) modelInstance2.getBpmntLog().get(1)).isInLoop();

        assertEquals(afterOf2.getId(), afterOf2Id);
        assertEquals(beforeOf2.getId(), beforeOf2Id);
        assert insertedFragment2.getModelElementById(firstInsertedNodeId) != null;
        assert insertedFragment2.getModelElementById(lastInsertedNodeId) != null;
        assertEquals(condition2, insertedCondition2);
        assert inLoopCondition2;
        assertEquals(2, modelInstance2.getBpmntLog().size());

    }


}
