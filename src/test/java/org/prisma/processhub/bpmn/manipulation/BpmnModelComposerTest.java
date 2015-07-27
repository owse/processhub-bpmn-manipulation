package org.prisma.processhub.bpmn.manipulation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.prisma.processhub.bpmn.manipulation.composition.BpmnModelComposer;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.List;

/**
 * Unit test for simple BpmnModelComposer.
 */
public class BpmnModelComposerTest
    extends TestCase
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

        StartEvent resultModelStart = BpmnElementSearcher.findStartEvent(modelInstance1);

        FlowNode lastNodeFromModel1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);
        FlowNode firstNodeFromModel2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode lastNodeFromModel2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);

        int numberNodesFromModel1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberNodesFromModel2 = modelInstance2.getModelElementsByType(FlowNode.class).size();

        BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInSeries(modelInstance1, modelInstance2);

        // Checks if the start event from the result model is the same as the start event from the first model
        assertEquals(resultModelStart.getId(), resultModel.getModelElementsByType(StartEvent.class).iterator().next().getId());

        // Checks if the last node from the first model is connected to the first node of the second model
        assertEquals(firstNodeFromModel2.getId(), lastNodeFromModel1.getOutgoing().iterator().next().getTarget().getId());

        // Checks if the last node from the second model is connected to the end event from the result model
        assertEquals(lastNodeFromModel2.getId(), resultModel.getModelElementsByType(EndEvent.class).iterator().next().getIncoming().iterator().next().getSource().getId());

        // Checks if the resulting number of nodes is the number expected
        assertEquals(numberNodesFromModel1 + numberNodesFromModel2 - 2, resultModel.getModelElementsByType(FlowNode.class).size());

    }

    public void testParallelProcessComposition () {
        System.out.println("Testing parallel process composition\n");
        BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();

        // Loading BPMN models
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        BpmnModelInstance modelInstance4 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));

        // Extracting start events
        StartEvent startEventFromModel1 = modelInstance1.getModelElementsByType(StartEvent.class).iterator().next();
        StartEvent startEventFromModel2 = modelInstance2.getModelElementsByType(StartEvent.class).iterator().next();
        StartEvent startEventFromModel3 = modelInstance3.getModelElementsByType(StartEvent.class).iterator().next();
        StartEvent startEventFromModel4 = modelInstance4.getModelElementsByType(StartEvent.class).iterator().next();

        // Extracting end events
        EndEvent endEventFromModel1 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();
        EndEvent endEventFromModel2 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();
        EndEvent endEventFromModel3 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();
        EndEvent endEventFromModel4 = modelInstance1.getModelElementsByType(EndEvent.class).iterator().next();

        // Extracting nodes after start events
        FlowNode flowNodeAfterStart1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode flowNodeAfterStart2 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance2);
        FlowNode flowNodeAfterStart3 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance3);
        FlowNode flowNodeAfterStart4 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance4);

        // Extracting nodes before end events
        FlowNode flowNodeBeforeEnd1 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);
        FlowNode flowNodeBeforeEnd2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance2);
        FlowNode flowNodeBeforeEnd3 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance3);
        FlowNode flowNodeBeforeEnd4 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance4);

        // Determining the number of flow nodes in each model
        int numberFlowNodes1 = modelInstance1.getModelElementsByType(FlowNode.class).size();
        int numberFlowNodes2 = modelInstance2.getModelElementsByType(FlowNode.class).size();
        int numberFlowNodes3 = modelInstance3.getModelElementsByType(FlowNode.class).size();
        int numberFlowNodes4 = modelInstance4.getModelElementsByType(FlowNode.class).size();

        // Determining the number of sequence flows in each model
        int numberSequenceFlows1 = modelInstance1.getModelElementsByType(SequenceFlow.class).size();
        int numberSequenceFlows2 = modelInstance2.getModelElementsByType(SequenceFlow.class).size();
        int numberSequenceFlows3 = modelInstance3.getModelElementsByType(SequenceFlow.class).size();
        int numberSequenceFlows4 = modelInstance4.getModelElementsByType(SequenceFlow.class).size();

        // Testing case 1: simple models in parallel (no gateways in source models)
        BpmnModelInstance resultModel1 = bpmnModelComposer.joinModelsInParallel(modelInstance1, modelInstance2);

        // Gateways created
        FlowNode gatewayAfterStart1 = startEventFromModel1.getOutgoing().iterator().next().getTarget();
        FlowNode gatewayBeforeEnd1 = endEventFromModel1.getIncoming().iterator().next().getSource();

        // Start event from model 1 is maintained
        assertEquals(startEventFromModel1.getId(), resultModel1.getModelElementsByType(StartEvent.class).iterator().next().getId());

        // End event from model 1 is maintained
        assertEquals(endEventFromModel1.getId(), resultModel1.getModelElementsByType(EndEvent.class).iterator().next().getId());

        // First node after the start event is a parallel gateway
        assert gatewayAfterStart1 instanceof ParallelGateway;

        // Last node before the end event is a parallel gateway
        assert gatewayBeforeEnd1 instanceof ParallelGateway;

        // Checks if the nodes succeeding the split parallel gateway are correct
        assert flowNodeAfterStart1.getId().equals(gatewayAfterStart1.getSucceedingNodes().list().get(0).getId()) ||
                flowNodeAfterStart1.getId().equals(gatewayAfterStart1.getSucceedingNodes().list().get(1).getId());
        assert flowNodeAfterStart2.getId().equals(gatewayAfterStart1.getSucceedingNodes().list().get(0).getId()) ||
                flowNodeAfterStart2.getId().equals(gatewayAfterStart1.getSucceedingNodes().list().get(1).getId());

        // Checks if the nodes preceding the join parallel gateway are correct
        assert flowNodeBeforeEnd1.getId().equals(gatewayBeforeEnd1.getPreviousNodes().list().get(0).getId()) ||
                flowNodeBeforeEnd1.getId().equals(gatewayBeforeEnd1.getPreviousNodes().list().get(1).getId());
        assert flowNodeBeforeEnd2.getId().equals(gatewayBeforeEnd1.getPreviousNodes().list().get(0).getId()) ||
                flowNodeBeforeEnd2.getId().equals(gatewayBeforeEnd1.getPreviousNodes().list().get(1).getId());

        // Checks the number of flow nodes
        assertEquals(numberFlowNodes1 + numberFlowNodes2, resultModel1.getModelElementsByType(FlowNode.class).size());

        // Checks the number of sequence flows
        assertEquals(numberSequenceFlows1 + numberSequenceFlows2 + 2, resultModel1.getModelElementsByType(SequenceFlow.class).size());
        // End of Test Case 1



        // Testing case 2: model with parallel gateways in parallel with a simple model

        // Extracting the parallel tasks
        List<Task> parallelTasks = (List) modelInstance3.getModelElementsByType(Task.class);

        BpmnModelInstance resultModel2 = bpmnModelComposer.joinModelsInParallel(modelInstance3, modelInstance4);

        // Node after the start event
        FlowNode gatewayAfterStart2 = startEventFromModel3.getSucceedingNodes().singleResult();
        // Node before the end event
        FlowNode gatewayBeforeEnd2 = resultModel2.getModelElementById(flowNodeBeforeEnd3.getId());

        // Check if first and last nodes are both parallel gateways
        assert gatewayAfterStart2 instanceof ParallelGateway;
        assert gatewayBeforeEnd2 instanceof ParallelGateway;

        // Check that every flow node after the split gateway is correct
        assert  parallelTasks.get(0).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(0).getId()) ||
                parallelTasks.get(0).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(1).getId()) ||
                parallelTasks.get(0).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(2).getId());

        assert  parallelTasks.get(1).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(0).getId()) ||
                parallelTasks.get(1).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(1).getId()) ||
                parallelTasks.get(1).getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(2).getId());

        assert  flowNodeAfterStart4.getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(0).getId()) ||
                flowNodeAfterStart4.getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(1).getId()) ||
                flowNodeAfterStart4.getId().equals(gatewayAfterStart2.getSucceedingNodes().list().get(2).getId());

        // Check that every flow node before the join gateway is correct
        assert  parallelTasks.get(0).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(0).getId()) ||
                parallelTasks.get(0).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(1).getId()) ||
                parallelTasks.get(0).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(2).getId());

        assert  parallelTasks.get(1).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(0).getId()) ||
                parallelTasks.get(1).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(1).getId()) ||
                parallelTasks.get(1).getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(2).getId());

        assert  flowNodeBeforeEnd4.getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(0).getId()) ||
                flowNodeBeforeEnd4.getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(1).getId()) ||
                flowNodeBeforeEnd4.getId().equals(gatewayBeforeEnd2.getPreviousNodes().list().get(2).getId());

        // Checks the number of flow nodes
        assertEquals(numberFlowNodes3 + numberFlowNodes4 - 2, resultModel2.getModelElementsByType(FlowNode.class).size());

        // Checks the number of sequence flows
        assertEquals(numberSequenceFlows3 + numberSequenceFlows4, resultModel2.getModelElementsByType(SequenceFlow.class).size());

        // Checks the number of flow nodes
        assertEquals(numberFlowNodes1 + numberFlowNodes2, resultModel1.getModelElementsByType(FlowNode.class).size());

        // Checks the number of sequence flows
        assertEquals(numberSequenceFlows1 + numberSequenceFlows2 + 2, resultModel1.getModelElementsByType(SequenceFlow.class).size());

    }

}
