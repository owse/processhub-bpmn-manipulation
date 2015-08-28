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
    public void convertBpmntFromListToModel_ValidBpmntList_BpmntListConvertedToModel() {
        BpmnModelInstance modelInstance1 = simpleModel;
        BpmnModelInstance modelInstance2 = parallelModel;

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
        bpmntOperations.add(new ReplaceFragmentWithFragment(task1.getId(), task2.getId(), modelInstance2));
        bpmntOperations.add(new MoveNode(task1.getId(), task2.getId(), endEvent.getId()));
        bpmntOperations.add(new Parallelize(task1.getId(), task2.getId()));

        System.out.println(Bpmn.convertToString(Bpmnt.convertBpmntFromListToModel(bpmntOperations)));
    }

}
