package org.prisma.processhub.bpmn.manipulation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Unit test for simple ModelComposer.
 */
public class ModelComposerTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ModelComposerTest(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ModelComposerTest.class );
    }

    public void testSerialProcessComposition() {
        System.out.println("Testing serial process composition\n");
        ModelComposer modelComposer = new ModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
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

        BpmnModelInstance resultModel = modelComposer.joinModelsInSeries(modelInstances);

        Collection<FlowNode> flowNodes = resultModel.getModelElementsByType(FlowNode.class);

        System.out.println("Flow Nodes from the result model ");
        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        System.out.println("\nResulting BPMN XML:\n");
        System.out.println(Bpmn.convertToString(resultModel));
    }

    public void testParallelProcessComposition () {
        System.out.println("Testing parallel process composition\n");
        ModelComposer modelComposer = new ModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
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

        BpmnModelInstance resultModel = modelComposer.joinModelsInParallel(modelInstances);

        Collection<FlowNode> flowNodes = resultModel.getModelElementsByType(FlowNode.class);

        System.out.println("Flow Nodes from the result model ");
        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        System.out.println("\nResulting BPMN XML:\n");
        System.out.println(Bpmn.convertToString(resultModel));
    }
    
}
