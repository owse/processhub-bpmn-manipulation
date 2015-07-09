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
        BpmnModelInstance modelInstance = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("diagram.bpmn"));

        List<Task> tasks =  (List) modelInstance.getModelElementsByType(Task.class);

        System.out.println("Tasks: ");
        for (Task task1: tasks) {
            System.out.println(task1.getName());
        }

        Process process = modelInstance.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class).iterator().next();
        Task task = modelInstance.getModelElementsByType(Task.class).iterator().next();
        System.out.println("Task to remove: " + task.getName());

        process.getFlowElements().remove(task);
        //modelInstance.findProcess().getFlowElements().remove(flowElement);
        tasks =  (List) modelInstance.getModelElementsByType(Task.class);

        System.out.println("Tasks: ");
        for (Task task2: tasks) {
            System.out.println(task2.getName());
        }

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
