package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BpmntModelInstanceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private TailorableBpmnModelInstance originalModel;
    private TailorableBpmnModelInstance simpleModel2;
    private TailorableBpmnModelInstance parallelModel;
    private TailorableBpmnModelInstance parallelModel2;
    private TailorableBpmnModelInstance subprocessModel;
    private TailorableBpmnModelInstance fragmentModel;

    // Load diagrams before each test
    @Before
    public void loadDiagrams() {
    	originalModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        simpleModel2 = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        parallelModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        parallelModel2 = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
        subprocessModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
        fragmentModel = TailorableBpmn.readModelFromStream(BpmntModelInstanceImplTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
    }

    // Tests naming convention: methodName_StateUnderTest_ExpectedBehavior

    // Test cases for the 'extend' method
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test
    public void extend() {
        BpmntModelInstance tailoredModel = originalModel.extend();

        Collection<FlowElement> baseFlowElements = originalModel.getModelElementsByType(FlowElement.class);

        // Verify that every flow element was successfully copied to the bpmntModelInstance
        for (FlowElement fe: baseFlowElements) {
            assert(tailoredModel.contains(fe));
        }
        
        System.out.println("############## Original Model: ##############");
        System.out.println(Bpmn.convertToString(originalModel));
        System.out.println("\n\n############## Tailored Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel));
        System.out.println("\n\n############## Bpmnt Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel.getBpmntModel()));
        
    }
    
    @Test
    public void delete() {
        BpmntModelInstance tailoredModel = originalModel.extend();
        tailoredModel.delete("UserTask_0qz0mkl");
        System.out.println("############## Original Model: ##############");
        System.out.println(Bpmn.convertToString(originalModel));
        System.out.println("\n\n############## Tailored Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel));
        System.out.println("\n\n############## Bpmnt Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel.getBpmntModel()));
        
    }
    
    
    @Test
    public void insert() {
        BpmntModelInstance tailoredModel = originalModel.extend();
        FlowNode after = originalModel.getModelElementById("UserTask_0qz0mkl");
        FlowNode before = originalModel.getModelElementById("ScriptTask_1rseic1");
        assert(after != null);
        assert(before != null);
        tailoredModel.insert(after, before, fragmentModel);
    }
    
   
    
    @Test
    public void replace() {
        BpmntModelInstance tailoredModel = originalModel.extend();
        FlowNode before = originalModel.getModelElementById("UserTask_0qz0mkl");
        FlowNode after = originalModel.getModelElementById("ScriptTask_1rseic1");
        assert(after != null);
        assert(before != null);
        tailoredModel.replace(after, before, fragmentModel);

        System.out.println("############## Original Model: ##############");
        System.out.println(Bpmn.convertToString(originalModel));
        System.out.println("\n\n############## Tailored Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel));
        System.out.println("\n\n############## Bpmnt Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel.getBpmntModel()));
        
    }
    
    @Test
    public void delete_ValidFragment_FragmentRemoved() {
        // Loading data
    	BpmntModelInstance tailoredModel = parallelModel2.extend();
        ParallelGateway splitGateway = tailoredModel.getModelElementById("ParallelGateway_1c6p3yf");
        ParallelGateway joinGateway = tailoredModel.getModelElementById("ParallelGateway_07aj32a");
        Task parallelTaskA = tailoredModel.getModelElementById("Task_1liqzit");
        Task parallelTaskB = tailoredModel.getModelElementById("Task_0dae65c");
        FlowNode firstNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(tailoredModel);
        FlowNode lastNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(tailoredModel);
        int initialNumberFlowNodes = tailoredModel.getModelElementsByType(FlowNode.class).size();


        // Make sure the fragment is valid
        Collection<FlowNode> fragment = BpmnFragmentHandler.mapProcessFragment(splitGateway, joinGateway);
        BpmnFragmentHandler.validateDeleteProcessFragment(fragment);

        // Deleting the parallel fragment
        tailoredModel.delete(splitGateway, joinGateway);

        // Verifies that every node in the selected fragment has been deleted
        assert tailoredModel.getModelElementsByType(ParallelGateway.class).isEmpty();
        assertEquals(null, tailoredModel.getModelElementById(parallelTaskA.getId()));
        assertEquals(null, tailoredModel.getModelElementById(parallelTaskB.getId()));

        // Verifies that the first task is connected to the last task
        assertEquals(lastNode, firstNode.getSucceedingNodes().singleResult());

        // Checks if the number of remaining flow nodes is correct
        assertEquals(initialNumberFlowNodes - fragment.size(), tailoredModel.getModelElementsByType(FlowNode.class).size());

        // Verify model consistency with Camunda API
        Bpmnt.validateModel(tailoredModel);
        
        
        System.out.println("\n\n############## Original Model: ##############");
        System.out.println(Bpmn.convertToString(parallelModel2));
        
        System.out.println("\n\n############## Tailored Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel));
        
        System.out.println("\n\n############## Bpmnt Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel.getBpmntModel()));

    }
    
    @Test
    public void testReplaceNodeWithNode() {
        System.out.println("Testing replace (node for node)");
        TailorableBpmnModelInstance originalModel = TailorableBpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmntModelInstance tailoredModel = originalModel.extend();
        TailorableBpmnModelInstance modelInstance2 = TailorableBpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        FlowNode replacingTask = modelInstance2.getModelElementsByType(Task.class).iterator().next();
        FlowNode replacedTask = tailoredModel.getModelElementsByType(Task.class).iterator().next();

        FlowNode previousNode = replacedTask.getPreviousNodes().singleResult();
        FlowNode succeedingNode = replacedTask.getSucceedingNodes().singleResult();

        tailoredModel.replace(replacedTask.getId(), replacingTask);

        FlowNode newTask = tailoredModel.getModelElementById(replacingTask.getId());

        // Verify that the target node has been successfully replaced
        assertEquals(previousNode, newTask.getPreviousNodes().singleResult());
        assertEquals(succeedingNode, newTask.getSucceedingNodes().singleResult());
        assertEquals(replacingTask.getId(), newTask.getId());

        Bpmnt.validateModel(tailoredModel);
        
        System.out.println("\n\n############## Original Model: ##############");
        System.out.println(Bpmn.convertToString(originalModel));
        
        System.out.println("\n\n############## Tailored Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel));
        
        System.out.println("\n\n############## Bpmnt Model: ##############");
        System.out.println(Bpmn.convertToString(tailoredModel.getBpmntModel()));

    }
    
    

}
