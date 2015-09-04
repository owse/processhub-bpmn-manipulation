package org.prisma.processhub.bpmn.manipulation.composition;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.ArrayList;
import java.util.List;




public class BpmnModelComposerTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public BpmnModelComposerTest(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BpmnModelComposerTest.class );
    }

    public void testSerialProcessComposition() {
        System.out.println("Testing serial process composition\n");
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        String startEventId = BpmnElementSearcher.findStartEvent(modelInstance1).getId();
        String endEventId = BpmnElementSearcher.findEndEvent(modelInstance1).getId();

        String lastNodeFromModel1Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1).getId();
        String firstNodeFromModel2Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeFromModel2Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();

        BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInSeries(modelInstance1, modelInstance2);

        // Checks if the start event from the result model is the same as the start event from the first model
        assertEquals(startEventId, BpmnElementSearcher.findStartEvent(resultModel).getId());

        // Checks if the last node from the first model is connected to the first node of the second model
        FlowNode lastNodeFromModel1 = resultModel.getModelElementById(lastNodeFromModel1Id);
        assertEquals(firstNodeFromModel2Id, lastNodeFromModel1.getSucceedingNodes().singleResult().getId());

        // Checks if the last node from the second model is connected to the end event from the result model
        FlowNode lastNodeFromModel2 = resultModel.getModelElementById(lastNodeFromModel2Id);
        assertEquals(endEventId, lastNodeFromModel2.getSucceedingNodes().singleResult().getId());

        // Checks if the resulting number of nodes is the number expected
        assertEquals(numberNodesFromModel1 + numberNodesFromModel2 - 2, resultModel.getModelElementsByType(FlowNode.class).size());

    }

    public void testSerialProcessCompositionList() {
        System.out.println("Testing serial process composition\n");
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        List<BpmnModelInstance> modelInstances = new ArrayList<BpmnModelInstance>();
        modelInstances.add(modelInstance1);
        modelInstances.add(modelInstance2);

        String startEventId = BpmnElementSearcher.findStartEvent(modelInstance1).getId();
        String endEventId = BpmnElementSearcher.findEndEvent(modelInstance1).getId();

        String lastNodeFromModel1Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1).getId();
        String firstNodeFromModel2Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeFromModel2Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();

        BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInSeries(modelInstances);

        // Checks if the start event from the result model is the same as the start event from the first model
        assertEquals(startEventId, BpmnElementSearcher.findStartEvent(resultModel).getId());

        // Checks if the last node from the first model is connected to the first node of the second model
        FlowNode lastNodeFromModel1 = resultModel.getModelElementById(lastNodeFromModel1Id);
        assertEquals(firstNodeFromModel2Id, lastNodeFromModel1.getSucceedingNodes().singleResult().getId());

        // Checks if the last node from the second model is connected to the end event from the result model
        FlowNode lastNodeFromModel2 = resultModel.getModelElementById(lastNodeFromModel2Id);
        assertEquals(endEventId, lastNodeFromModel2.getSucceedingNodes().singleResult().getId());

        // Checks if the resulting number of nodes is the number expected
        assertEquals(numberNodesFromModel1 + numberNodesFromModel2 - 2, resultModel.getModelElementsByType(FlowNode.class).size());

    }

    public void testParallelProcessComposition_TwoModels () {
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        String startEventId = BpmnElementSearcher.findStartEvent(modelInstance1).getId();
        String endEventId = BpmnElementSearcher.findEndEvent(modelInstance1).getId();

        String firstNodeFromModel1Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1).getId();
        String lastNodeFromModel1Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1).getId();
        String firstNodeFromModel2Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeFromModel2Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();

        BpmnModelInstance resultModel = bpmnModelComposer.newJoinModelsInParallel(modelInstance1, modelInstance2);

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);

        // Verify that the node following the start event is a divergent gateway
        FlowNode divergentGateway = startEvent.getSucceedingNodes().singleResult();
        assert divergentGateway instanceof ParallelGateway;
        assertEquals(2, divergentGateway.getSucceedingNodes().list().size());

        // Verify that the node preceding the end event is a convergent gateway
        FlowNode convergentGateway = endEvent.getPreviousNodes().singleResult();
        assert convergentGateway instanceof ParallelGateway;
        assertEquals(2, convergentGateway.getPreviousNodes().list().size());

        FlowNode firstNodeFromModel1 = resultModel.getModelElementById(firstNodeFromModel1Id);
        FlowNode firstNodeFromModel2 = resultModel.getModelElementById(firstNodeFromModel2Id);
        FlowNode lastNodeFromModel1 = resultModel.getModelElementById(lastNodeFromModel1Id);
        FlowNode lastNodeFromModel2 = resultModel.getModelElementById(lastNodeFromModel2Id);

        assertEquals(divergentGateway, firstNodeFromModel1.getPreviousNodes().singleResult());
        assertEquals(divergentGateway, firstNodeFromModel2.getPreviousNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel1.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel2.getSucceedingNodes().singleResult());

        assertEquals(numberNodesFromModel1 + numberNodesFromModel2, resultModel.getModelElementsByType(FlowNode.class).size());
    }

    public void testParallelProcessComposition_ThreeModels () {
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));


        String startEventId = BpmnElementSearcher.findStartEvent(modelInstance1).getId();
        String endEventId = BpmnElementSearcher.findEndEvent(modelInstance1).getId();

        String firstNodeFromModel1Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1).getId();
        String lastNodeFromModel1Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1).getId();
        String firstNodeFromModel2Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeFromModel2Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel3 = modelInstance3.getModelElementsByType(FlowNode.class).size();

        BpmnModelInstance resultModel = bpmnModelComposer.newJoinModelsInParallel(modelInstance1, modelInstance2, modelInstance3);

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);

        // Verify that the node following the start event is a divergent gateway
        FlowNode divergentGateway = startEvent.getSucceedingNodes().singleResult();
        assert divergentGateway instanceof ParallelGateway;
        assertEquals(3, divergentGateway.getSucceedingNodes().list().size());

        // Verify that the node preceding the end event is a convergent gateway
        FlowNode convergentGateway = endEvent.getPreviousNodes().singleResult();
        assert convergentGateway instanceof ParallelGateway;
        assertEquals(3, convergentGateway.getPreviousNodes().list().size());

        FlowNode firstNodeFromModel1 = resultModel.getModelElementById(firstNodeFromModel1Id);
        FlowNode firstNodeFromModel2 = resultModel.getModelElementById(firstNodeFromModel2Id);
        FlowNode lastNodeFromModel1 = resultModel.getModelElementById(lastNodeFromModel1Id);
        FlowNode lastNodeFromModel2 = resultModel.getModelElementById(lastNodeFromModel2Id);

        assertEquals(divergentGateway, firstNodeFromModel1.getPreviousNodes().singleResult());
        assertEquals(divergentGateway, firstNodeFromModel2.getPreviousNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel1.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel2.getSucceedingNodes().singleResult());

        assertEquals(numberNodesFromModel1 + numberNodesFromModel2 + numberNodesFromModel3 - 2, resultModel.getModelElementsByType(FlowNode.class).size());
    }

    public void testParallelProcessComposition_ThreeModels_List () {
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));


        String startEventId = BpmnElementSearcher.findStartEvent(modelInstance1).getId();
        String endEventId = BpmnElementSearcher.findEndEvent(modelInstance1).getId();

        String firstNodeFromModel1Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1).getId();
        String lastNodeFromModel1Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1).getId();
        String firstNodeFromModel2Id = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2).getId();
        String lastNodeFromModel2Id = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2).getId();

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel3 = modelInstance3.getModelElementsByType(FlowNode.class).size();

        List<BpmnModelInstance> modelsToJoin = new ArrayList<BpmnModelInstance>();
        modelsToJoin.add(modelInstance1);
        modelsToJoin.add(modelInstance2);
        modelsToJoin.add(modelInstance3);
        BpmnModelInstance resultModel = bpmnModelComposer.newJoinModelsInParallel(modelsToJoin);

        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);

        // Verify that the node following the start event is a divergent gateway
        FlowNode divergentGateway = startEvent.getSucceedingNodes().singleResult();
        assert divergentGateway instanceof ParallelGateway;
        assertEquals(3, divergentGateway.getSucceedingNodes().list().size());

        // Verify that the node preceding the end event is a convergent gateway
        FlowNode convergentGateway = endEvent.getPreviousNodes().singleResult();
        assert convergentGateway instanceof ParallelGateway;
        assertEquals(3, convergentGateway.getPreviousNodes().list().size());

        FlowNode firstNodeFromModel1 = resultModel.getModelElementById(firstNodeFromModel1Id);
        FlowNode firstNodeFromModel2 = resultModel.getModelElementById(firstNodeFromModel2Id);
        FlowNode lastNodeFromModel1 = resultModel.getModelElementById(lastNodeFromModel1Id);
        FlowNode lastNodeFromModel2 = resultModel.getModelElementById(lastNodeFromModel2Id);

        assertEquals(divergentGateway, firstNodeFromModel1.getPreviousNodes().singleResult());
        assertEquals(divergentGateway, firstNodeFromModel2.getPreviousNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel1.getSucceedingNodes().singleResult());
        assertEquals(convergentGateway, lastNodeFromModel2.getSucceedingNodes().singleResult());

        assertEquals(numberNodesFromModel1 + numberNodesFromModel2 + numberNodesFromModel3 - 2, resultModel.getModelElementsByType(FlowNode.class).size());
    }
}
