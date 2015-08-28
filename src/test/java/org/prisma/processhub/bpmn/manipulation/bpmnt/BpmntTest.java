package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.ArrayList;
import java.util.List;

/**
* Created by renan on 8/19/15.
*/

public class BpmntTest {

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
        simpleModel = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        simpleModel2 = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        parallelModel = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
        parallelModel2 = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
        subprocessModel = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
        fragmentModel = Bpmnt.readModelFromStream(BpmntTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
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
    public void testFragmentValidation() {
        System.out.println("Testing fragment validation");

        BpmntModelInstance modelInstance = Bpmnt.readModelFromStream(getClass().getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));

        StartEvent afterOf = BpmnElementSearcher.findStartEvent(modelInstance);
        FlowNode beforeOf = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance);
        FlowNode targetStartingNode = beforeOf.getSucceedingNodes().singleResult().getSucceedingNodes().singleResult();
        FlowNode targetEndingNode = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance).getPreviousNodes().singleResult();

        // Moving the parallel fragment to the start of the process
        modelInstance.move(targetStartingNode.getId(), targetEndingNode.getId(), afterOf.getId(), beforeOf.getId());
    }

    @Test
    public void testBpmntListToModelConversion2() {
        TailorableBpmnModelInstance modelInstance1 = TailorableBpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));

        FlowNode newNode = modelInstance3.getModelElementsByType(Task.class).iterator().next();

        BpmntModelInstance resultModel = modelInstance1.extend();

        Process baseProcess = BpmnElementSearcher.findFirstProcess(resultModel);
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(resultModel);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(resultModel);
        FlowNode task1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(resultModel);
        FlowNode task2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(resultModel);


        resultModel.delete(task1);
        resultModel.insert(startEvent, task2, newNode);

        for (BpmntOperation op: resultModel.getBpmntLog()) {
            System.out.println(op.getName());
        }

        System.out.println(Bpmn.convertToString(Bpmnt.convertBpmntFromListToModel(resultModel.getBpmntLog())));
    }


    @Test
    public void testBpmntListToModelConversion() {
        BpmnModelInstance modelInstance1 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
        BpmnModelInstance modelInstance2 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
        BpmnModelInstance modelInstance3 = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));

        List<BpmntOperation> bpmntOperations = new ArrayList<BpmntOperation>();

        Process baseProcess = BpmnElementSearcher.findFirstProcess(modelInstance1);
        StartEvent startEvent = BpmnElementSearcher.findStartEvent(modelInstance1);
        EndEvent endEvent = BpmnElementSearcher.findEndEvent(modelInstance1);
        FlowNode task1 = BpmnElementSearcher.findFlowNodeAfterStartEvent(modelInstance1);
        FlowNode task2 = BpmnElementSearcher.findFlowNodeBeforeEndEvent(modelInstance1);

        bpmntOperations.add(new Extend(baseProcess.getId()));
        bpmntOperations.add(new Extend(baseProcess.getId()));
        bpmntOperations.add(new DeleteNode(startEvent.getId()));
        bpmntOperations.add(new DeleteFragment(task1.getId(), task2.getId()));
        bpmntOperations.add(new ReplaceFragmentWithNode(task1.getId(), task2.getId(), task1));
        bpmntOperations.add(new ReplaceFragmentWithFragment(task1.getId(), task2.getId(), modelInstance3));
        bpmntOperations.add(new MoveNode(task1.getId(), task2.getId(), endEvent.getId()));
        bpmntOperations.add(new Parallelize(task1.getId(), task2.getId()));


        System.out.println(Bpmn.convertToString(Bpmnt.convertBpmntFromListToModel(bpmntOperations)));
    }

}
