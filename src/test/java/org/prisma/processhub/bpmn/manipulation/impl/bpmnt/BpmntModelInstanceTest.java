//package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;
//
//import org.camunda.bpm.model.bpmn.instance.*;
//import org.camunda.bpm.model.bpmn.instance.Process;
//import org.camunda.bpm.model.xml.instance.ModelElementInstance;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.ExpectedException;
//import org.prisma.processhub.bpmn.manipulation.tailoring.Bpmnt;
//import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;
//import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
//
///**
// * Created by renan on 8/19/15.
// */
//
//public class BpmntModelInstanceTest {
//
//    @Rule
//    public final ExpectedException exception = ExpectedException.none();
//
//    private BpmntModelInstance simpleModel;
//    private BpmntModelInstance simpleModel2;
//    private BpmntModelInstance parallelModel;
//    private BpmntModelInstance parallelModel2;
//    private BpmntModelInstance subprocessModel;
//    private BpmntModelInstance fragmentModel;
//
//    // Load diagrams before each test
//    @Before
//    public void loadDiagrams() {
//        simpleModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("simple_diagram.bpmn"));
//        simpleModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("simple_diagram2.bpmn"));
//        parallelModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("parallel_diagram.bpmn"));
//        parallelModel2 = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("parallel_diagram2.bpmn"));
//        subprocessModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("subprocess_diagram.bpmn"));
//        fragmentModel = Bpmnt.readModelFromStream(BpmntModelInstanceTest.class.getClassLoader().getResourceAsStream("test_fragment_validation_diagram.bpmn"));
//    }
//
//// Tests naming convention: methodName_StateUnderTest_ExpectedBehavior
//
//    @Test
//    public void myTest() {
//        // Create model from scratch
//        BpmntModelInstance modelInstance = Bpmnt.createEmptyModel();
//        Definitions definitions = modelInstance.newInstance(Definitions.class);
//        definitions.setTargetNamespace("http://camunda.org/examples");
//        modelInstance.setDefinitions(definitions);
//
//        // Create process
//        org.camunda.bpm.model.bpmn.instance.Process process = modelInstance.newInstance(Process.class);
//        process.setId("process");
//        definitions.addChildElement(process);
//
//        // Create flow node
//        FlowNode flowNode = BpmnElementSearcher.findFlowNodeAfterStartEvent(simpleModel);
//        modelInstance.contribute(process, flowNode);
//        flowNode = modelInstance.getModelElementById(flowNode.getId());
//        flowNode.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));
//        ExtensionElements extensionElements = flowNode.getExtensionElements();
//        ModelElementInstance extension = extensionElements.addExtensionElement("domain", "bpmnt");
//        extension.setTextContent("Some text");
//        extension.setAttributeValue("some_attr", "Some Attribute");
//        extensionElements.addExtensionElement("extension", "my operation");
//
//        // Check if model is valid
//        Bpmnt.validateModel(modelInstance);
//
//    }
//}
