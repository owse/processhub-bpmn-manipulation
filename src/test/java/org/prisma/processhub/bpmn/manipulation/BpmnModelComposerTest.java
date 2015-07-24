package org.prisma.processhub.bpmn.manipulation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.prisma.processhub.bpmn.manipulation.composition.BpmnModelComposer;
import org.prisma.processhub.bpmn.manipulation.exception.FlowElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.ArrayList;
import java.util.Collection;
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

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("loop_diagram.bpmn"));

        // Populate the list of models to concatenate
        List<BpmnModelInstance> modelInstances = new ArrayList<BpmnModelInstance>();
        modelInstances.add(modelInstance1);
        modelInstances.add(modelInstance2);
        modelInstances.add(modelInstance3);

        int count = 1;
        for (BpmnModelInstance mi: modelInstances) {
            Collection<FlowNode> flowNodes = mi.getModelElementsByType(FlowNode.class);
            System.out.println("Flow Nodes from model " + count + ":");
            for (FlowNode fn: flowNodes) {
                System.out.println(fn.getId());
            }
            System.out.println("\n");
            count++;
        }

        //BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInParallel(modelInstances);
        BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInParallel(modelInstance1, modelInstance2, modelInstance3);

        Collection<FlowNode> flowNodes = resultModel.getModelElementsByType(FlowNode.class);

        System.out.println("Flow Nodes from the result model ");
        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        System.out.println("\nResulting BPMN XML:\n");
        System.out.println(Bpmn.convertToString(resultModel));
    }

}
