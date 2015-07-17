package org.prisma.processhub.bpmn.manipulation.tailoring.impl;

import junit.framework.TestCase;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmnExtendedModelInstance;

import java.util.Iterator;

public class BpmnExtendedModelInstanceImplTest extends TestCase {

    public void testSuppress() throws Exception {
        BpmnModelInstance modelInstance = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnExtendedModelInstance extendedModelInstance = new BpmnExtendedModelInstanceImpl();
        extendedModelInstance.setModelInstance(modelInstance);

        Iterator<FlowElement> flowElementIterator = extendedModelInstance.getModelInstance().getModelElementsByType(FlowElement.class).iterator();
        FlowElement flowElementToRemove = flowElementIterator.next();

        System.out.println("Flow Elements:\n");
        System.out.println(flowElementToRemove);

        while (flowElementIterator.hasNext()) {
            System.out.println(flowElementIterator.next());
        }

        extendedModelInstance.suppress(flowElementToRemove);

        Iterator<FlowElement> flowElementIterator1 = extendedModelInstance.getModelInstance().getModelElementsByType(FlowElement.class).iterator();

        System.out.println("\n\nFlow elements with first element removed:\n");
        while (flowElementIterator1.hasNext()) {
            System.out.println(flowElementIterator1.next());
        }

    }

    // TODO: fix contribute()
    /*
    public void testContribute() throws Exception {

        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnExtendedModelInstance extendedModelInstance = new BpmnExtendedModelInstanceImpl();
        extendedModelInstance.setModelInstance(modelInstance1);

        Iterator<FlowElement> flowElementIterator = extendedModelInstance.getModelInstance().getModelElementsByType(FlowElement.class).iterator();
        FlowElement flowElementToRemove = flowElementIterator.next();

        System.out.println("Flow Elements:\n");
        while (flowElementIterator.hasNext()) {
            System.out.println(flowElementIterator.next());
        }

        extendedModelInstance.contribute(modelInstance2.getModelElementsByType(StartEvent.class).iterator().next());

        Iterator<FlowElement> flowElementIterator1 = extendedModelInstance.getModelInstance().getModelElementsByType(FlowElement.class).iterator();

        System.out.println("\n\nFlow elements with element added:\n");
        while (flowElementIterator1.hasNext()) {
            System.out.println(flowElementIterator1.next());
        }

    }
    */
}
