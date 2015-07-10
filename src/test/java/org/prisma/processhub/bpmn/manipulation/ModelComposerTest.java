package org.prisma.processhub.bpmn.manipulation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

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
        ModelComposer modelComposer = new ModelComposer();

        // read a BPMN model from an input stream
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));

        Collection<FlowElement> flowElements1 = modelInstance1.getModelElementsByType(FlowElement.class);
        Collection<FlowElement> flowElements2 = modelInstance2.getModelElementsByType(FlowElement.class);

        List<BpmnModelInstance> modelInstances = new ArrayList<BpmnModelInstance>();
        modelInstances.add(modelInstance1);
        modelInstances.add(modelInstance2);
        modelInstances.add(modelInstance3);

        for (BpmnModelInstance mi: modelInstances) {
            Collection<FlowNode> flowNodes = mi.getModelElementsByType(FlowNode.class);
            System.out.println("Flow Nodes");
            for (FlowNode fn: flowNodes) {
                System.out.println(fn.getId());
            }
            System.out.println("\n\n");
        }


        BpmnModelInstance resultModel = modelComposer.joinModelsInSeries(modelInstances);

        Collection<FlowNode> flowNodes = resultModel.getModelElementsByType(FlowNode.class);

        Collection<FlowElement> flowElements = resultModel.getModelElementsByType(FlowElement.class);

        for (FlowNode fn: flowNodes) {
            System.out.println(fn.getId());
        }

        //System.out.println(Bpmn.convertToString(resultModel));



    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ModelComposerTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
