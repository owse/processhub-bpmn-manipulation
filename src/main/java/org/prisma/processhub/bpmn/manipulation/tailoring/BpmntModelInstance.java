package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.exception.FlowElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.exception.FlowNodeNotFoundException;

//import java.util.List;

// Interface that extends the Camunda API with tailoring operations
public interface BpmntModelInstance extends BpmnModelInstance {

    // Low-level operations
    void suppress (FlowElement targetElement);

    void suppress (String targetElementId) throws FlowElementNotFoundException;

    // Due to Camunda API's fluent builder API, makes no sense to implement it
    //void contribute (FlowElement targetElement);

    // Not in use
    //void extend (BpmnModelInstance modelInstance);
    //void modify (FlowElement targetElement, List<String> properties);

    // High-level operations
    void rename(String targetElementId, String newName) throws FlowElementNotFoundException;

    void delete(FlowNode targetNode);
    void delete(String targetNodeId) throws FlowNodeNotFoundException;

    void delete(FlowNode startingNode, FlowNode endingNode);
    void delete(String startingNodeId, String endingNodeId) throws FlowNodeNotFoundException;

    void replace(FlowNode targetNode, FlowNode replacingNode);
    void replace(String targetNodeId, FlowNode FlowNode) throws FlowNodeNotFoundException;

    void replace(FlowNode targetNode, BpmnModelInstance replacingFragment);
    void replace(String targetNodeId, BpmnModelInstance replacingFragment) throws FlowNodeNotFoundException;

    void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode);
    void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) throws FlowNodeNotFoundException;

    void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment);
    void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) throws FlowNodeNotFoundException;



    void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode);
    void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode);
    void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode);

    void split(Task targetTask, BpmnModelInstance newSubProcessModel);

    void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop);
    void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop);
    void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop);

//    void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
//    void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
//    void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
}
