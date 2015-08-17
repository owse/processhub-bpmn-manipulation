package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.BpmntOperation;
import org.prisma.processhub.bpmn.manipulation.impl.tailoring.TailorableBpmnModelInstanceImpl;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;

import java.util.Collection;
import java.util.List;

public class BpmntModelInstanceImpl extends TailorableBpmnModelInstanceImpl implements BpmntModelInstance {

    private List<BpmntOperation> bpmntLog;

    // Constructor
    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    // BPMNt log operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    public List<BpmntOperation> getBpmntLog() {
        return bpmntLog;
    }

    public void setBpmntLog(List<BpmntOperation> bpmntLog) {
        this.bpmntLog = bpmntLog;
    }

    public int getNumberOperations() {
        return bpmntLog.size();
    }


    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Add a new single process element to the given parent element
    public <T extends FlowElement, E extends ModelElementInstance> T contribute(E parentElement, T element) {
        return super.contribute(parentElement, element);
    }

    // Add a new single element to the first process in this model as parent
    public <T extends FlowElement> T contribute(T element) {
       return super.contribute(element);
    }

    // Remove flow element leaving the rest of the model untouched
    public <T extends FlowElement> void suppress(T element) {
        super.suppress(element);
    }

    // Remove every element in collection
    public <T extends FlowElement> void suppress(Collection<T> elements) {
        super.suppress(elements);
    }

    // Remove flow element by id
    public void suppress(String elementId) {
        super.suppress(elementId);
    }

    // Modify a property of a flow element
    public <T extends FlowElement> void modify(T element, String property, String value) {
        super.modify(element, property, value);
    }

    // Modify a property of a flow element with given id
    public void modify(String elementId, String property, String value) {
        super.modify(elementId, property, value);
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void rename(FlowElement element, String newName) {
        super.rename(element, newName);
    }

    // Rename element by id
    public void rename(String elementId, String newName) {
        super.rename(elementId, newName);
    }


    // Delete a node, all sequence flows connected to it and also obsolete gateways
    public void delete(FlowNode node){
        super.delete(node);
    }

    public void fixGatewaysDelete(Collection<FlowNode> previousNodes, Collection<FlowNode> succeedingNodes) {
        super.fixGatewaysDelete(previousNodes, succeedingNodes);
    }


    // Delete element by id
    public void delete(String nodeId) {
        super.delete(nodeId);
    }

    // Delete range of elements from startingNode to endingNode
    public void delete(FlowNode startingNode, FlowNode endingNode) {
        super.delete(startingNode, endingNode);
    }

    public void delete(String startingNodeId, String endingNodeId) {
        super.delete(startingNodeId, endingNodeId);
    }

    public void replace(FlowNode existingNode, FlowNode replacingNode) {
        super.replace(existingNode, replacingNode);
    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        super.replace(existingNodeId, replacingNode);
    }

    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        super.replace(existingNode, replacingFragment);
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        super.replace(existingNodeId, replacingFragment);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        super.replace(startingNode, endingNode, replacingNode);
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        super.replace(startingNodeId, endingNodeId, replacingNode);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        super.replace(startingNode, endingNode, replacingFragment);
    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        super.replace(startingNodeId, endingNodeId, replacingFragment);
    }

    public void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        super.move(targetNode, newPositionAfterOf, newPositionBeforeOf);
    }

    public void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        super.move(targetNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode,
                     FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        super.move(targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);
    }

    public void move(String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {

        super.move(targetStartingNodeId, targetEndingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) throws Exception {
        super.parallelize(targetStartingNode, targetEndingNode);
    }

    public void parallelize(String targetStartingNodeId, String targetEndingNodeId) throws Exception {
        super.parallelize(targetStartingNodeId, targetEndingNodeId);
    }

    public void split(Task targetTask, BpmnModelInstance newSubProcessModel){
        super.split(targetTask, newSubProcessModel);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        super.insert(afterOf, beforeOf, flowNodeToInsert);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        super.insert(afterOf, beforeOf, fragmentToInsert);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, flowNodeToInsert, condition, inLoop);
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, fragmentToInsert, condition, inLoop);
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(
                afterOf,
                beforeOf,
                BpmnElementSearcher.findFirstProcess(fragmentToInsert),
                condition,
                inLoop
        );
    }
}
