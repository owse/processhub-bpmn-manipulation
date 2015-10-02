package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.BpmntOperation;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.Extend;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BpmntModelInstance extends BpmnModelInstance {
    // Useful operations that extend BpmnModelInstance features
    boolean contains (FlowElement element);

    String setUniqueId(FlowElement element);
    void generateUniqueIds();

    // BPMNt log operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    List<BpmntOperation> getBpmntLog();
    void setBpmntLog(List<BpmntOperation> bpmntLog);
    int getNumberOperations();
    void init(Extend extend);
    void execute(BpmntOperation operation);
    void execute(List<BpmntOperation> operations);
    void executeOwnBpmnt();

    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Contribute
    <T extends FlowElement, E extends ModelElementInstance> void contribute(E parentElement, T element);
    <T extends FlowElement> void contribute(T element);

    // Suppress
    <T extends FlowElement> void suppress (T element);
    <T extends FlowElement> void suppress (Collection<T> elements);
    void suppress (String elementId);

    // Modify
    <T extends FlowElement> void modify(T element, String property, String value);
    void modify(String elementId, String property, String value);


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename
    void rename(String elementId, String newName);
    void rename(FlowElement element, String newName);

    // Delete
    void delete(FlowNode node);
    void delete(String nodeId);

    void delete(FlowNode startingNode, FlowNode endingNode);
    void delete(String startingNodeId, String endingNodeId);

    // Replace
    void replace(FlowNode targetNode, FlowNode replacingNode);
    void replace(String targetNodeId, FlowNode flowNode);

    void replace(FlowNode targetNode, BpmnModelInstance replacingFragment);
    void replace(String targetNodeId, BpmnModelInstance replacingFragment);

    void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode);
    void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode);

    void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment);
    void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment);

    // Move
    void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf);
    void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId);

    void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf);
    void move(String targetStartingNodeId, String targetEndingNodeId, String newPositionAfterOfId, String newPositionBeforeOfId);

    // Parallelize
    void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode);
    void parallelize(String targetStartingNodeId, String targetEndingNodeId);

    // Split
    void split(Task targetTask, BpmnModelInstance newSubProcessModel);

    // Insert
    void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, org.camunda.bpm.model.bpmn.instance.Process fragmentToInsert);
    void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert);

    // Conditional Insert
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop);
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop);
    void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop);

	BpmnModelInstance getBpmntModel();
}
