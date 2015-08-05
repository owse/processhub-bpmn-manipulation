package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;

//import java.util.List;

// Interface that extends the Camunda API with tailoring operations
public interface BpmntModelInstance extends BpmnModelInstance {

    // Low-level operations
    void suppress (FlowElement targetElement);

    void suppress (String targetElementId);

    // Due to Camunda API's fluent builder API, makes no sense to implement it
    //void contribute (FlowElement targetElement);

    // Not in use
    //void extend (BpmnModelInstance modelInstance);
    //void modify (FlowElement targetElement, List<String> properties);

    // High-level operations
    void rename(String targetElementId, String newName);

    void delete(FlowNode targetNode);
    void delete(String targetNodeId);

    void delete(FlowNode startingNode, FlowNode endingNode);
    void delete(String startingNodeId, String endingNodeId);

    void replace(FlowNode targetNode, FlowNode replacingNode);
    void replace(String targetNodeId, FlowNode FlowNode);

    void replace(FlowNode targetNode, BpmnModelInstance replacingFragment);
    void replace(String targetNodeId, BpmnModelInstance replacingFragment);

    void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode);
    void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode);

    void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment);
    void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment);

    void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf);
    void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId);

    void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf);
    void move(String targetStartingNodeId, String targetEndingNodeId, String newPositionAfterOfId, String newPositionBeforeOfId);

    void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) throws Exception;
    void parallelize(String targetStartingNodeId, String targetEndingNodeId) throws Exception;

    void split(Task targetTask, BpmnModelInstance newSubProcessModel);

    void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert);

    // TODO: find out what is the purpose of "inLoop"
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop);
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop);
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop);

//    void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
//    void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
//    void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
}
