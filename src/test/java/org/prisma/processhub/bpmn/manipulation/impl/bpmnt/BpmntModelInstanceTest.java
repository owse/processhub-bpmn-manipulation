package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.ArrayList;
import java.util.List;

/**
* Created by renan on 8/19/15.
*/

public class BpmntModelInstanceTest {

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
        simpleModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        simpleModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        parallelModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        parallelModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
        subprocessModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
        fragmentModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
    }

// Tests naming convention: methodName_StateUnderTest_ExpectedBehavior

    @Test
    public void myTest() {
        // Create model from scratch
        TailorableBpmnModelInstance modelInstance = TailorableBpmn.createEmptyModel();
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://camunda.org/examples");
        modelInstance.setDefinitions(definitions);

        // Create process
        Process process = modelInstance.newInstance(Process.class);
        process.setId("process");
        definitions.addChildElement(process);

        // Create flow node
        FlowNode flowNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(simpleModel);
        System.out.println(flowNode);
        modelInstance.contribute(flowNode);

        System.out.println(TailorableBpmn.convertToString(modelInstance));

        flowNode = modelInstance.getModelElementById(flowNode.getId());
        flowNode.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));
        ExtensionElements extensionElements = flowNode.getExtensionElements();
        ModelElementInstance extension = extensionElements.addExtensionElement("domain", "bpmnt");
        extension.setTextContent("Some text");
        extension.setAttributeValue("some_attr", "Some Attribute");
        extensionElements.addExtensionElement("extension", "my_operation");

        // Check if model is valid
        TailorableBpmn.validateModel(modelInstance);
    }

    @Test
    public void testBpmntListToModelConversion() {
        List<BpmntOperation> bpmntOperations = new ArrayList<BpmntOperation>();

        Process baseProcess = BpmnElementSearcher.findFirstProcess(simpleModel);
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(simpleModel);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(simpleModel);
        FlowNode task1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(simpleModel);
        FlowNode task2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(simpleModel);

        BpmntOperation operation;
        bpmntOperations.add(new Extend(baseProcess.getId()));
        operation = new DeleteNode(startEvent.getId());
        operation.setExecutionOrder(2);
        bpmntOperations.add(operation);
        operation = new DeleteFragment(task1.getId(), task2.getId());
        operation.setExecutionOrder(3);
        bpmntOperations.add(operation);
        operation = new ReplaceFragmentWithNode(task1.getId(), task2.getId(), task1);
        operation.setExecutionOrder(4);
        bpmntOperations.add(operation);
        operation = new ReplaceFragmentWithFragment(task1.getId(), task2.getId(), parallelModel);
        operation.setExecutionOrder(5);
        bpmntOperations.add(operation);
        operation = new MoveNode(task1.getId(), task2.getId(), endEvent.getId());
        operation.setExecutionOrder(6);
        bpmntOperations.add(operation);
        operation = new Parallelize(task1.getId(), task2.getId());
        operation.setExecutionOrder(7);
        bpmntOperations.add(operation);


        System.out.println(Bpmn.convertToString(Bpmnt.convertBpmntFromListToModel(bpmntOperations)));
    }
}
