package org.prisma.processhub.bpmn.manipulation.impl.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.operation.*;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TailorableBpmnModelInstanceImpl extends BpmnModelInstanceImpl implements TailorableBpmnModelInstance {

    // Constructor
    public TailorableBpmnModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    // Create a BPMNt model from a tailorable model
    public BpmntModelInstance extend() {

        // Copy the tailorable model to a BPMNt model
        InputStream stream = new ByteArrayInputStream(TailorableBpmn.convertToString(this).getBytes(StandardCharsets.UTF_8));
        BpmntModelInstance bpmntModelInstance = Bpmnt.readModelFromStream(stream);

        // Get firs process to set a new id for it, different than the base process id
        Process process = BpmnElementSearcher.findFirstProcess(bpmntModelInstance);

        // Instance and populate the list of operations with the extend operator
        Map<Integer, BpmnOperation> bpmntLog = new LinkedHashMap<Integer, BpmnOperation>();
        Extend ext = new Extend(process.getId());
        bpmntLog.put(1, ext);
        bpmntModelInstance.setBpmntLog(bpmntLog);
        process.setId(ext.getNewProcessId());

        return bpmntModelInstance;
    }

    // Useful operations that extend BpmnModelInstance features
    public boolean contains (FlowElement element) {
        return BpmnElementHandler.contains(this, element);
    }

    public String setUniqueId(FlowElement element) {
        BpmnHelper.checkElementPresent(contains(element), "Argument element is not part of this BpmnModelInstance");
        return BpmnElementHandler.setUniqueId(element);
    }

    public void generateUniqueIds() {
        BpmnElementHandler.generateUniqueIds(this);
    }

    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Contribute
    public <T extends FlowElement, E extends ModelElementInstance> void contribute(E parentElement, T element) {
        ContributeToParent operation = new ContributeToParent(parentElement, element);
        operation.execute(this);
    }

    public <T extends FlowElement> void contribute(T element) {
        Contribute operation = new Contribute(element);
        operation.execute(this);
    }

    // Suppress
    public void suppress (String elementId) {
        Suppress operation = new Suppress(elementId);
        operation.execute(this);
    }

    public <T extends FlowElement> void suppress(T element) {
        String elementId = (element == null) ? null : element.getId();
        suppress(elementId);
    }

    public <T extends FlowElement> void suppress (Collection<T> elements) {
        for (FlowElement element : elements) {
            suppress(element.getId());
        }
    }

    // Modify
    public void modify(String elementId, String property, String value) {
        Modify operation = new Modify(elementId, property, value);
        operation.execute(this);
    }

    public <T extends FlowElement> void modify(T element, String property, String value) {
        modify(element.getId(), property, value);
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename
    public void rename(String elementId, String newName) {
        Rename operation = new Rename(elementId, newName);
        operation.execute(this);
    }

    public void rename(FlowElement element, String newName) {
        String elementId = (element == null) ? null : element.getId();
        rename(elementId, newName);
    }

    // Delete
    public void delete(String nodeId) {
        DeleteNode operation = new DeleteNode(nodeId);
        operation.execute(this);
    }

    public void delete(FlowNode node) {
        String nodeId = (node == null) ? null : node.getId();
        delete(nodeId);
    }

    public void delete(String startingNodeId, String endingNodeId) {
        DeleteFragment operation = new DeleteFragment(startingNodeId, endingNodeId);
        operation.execute(this);
    }

    public void delete(FlowNode startingNode, FlowNode endingNode) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        delete(startingNodeId, endingNodeId);
    }

    // Replace
    public void replace(String targetNodeId, FlowNode flowNode) {
        ReplaceNodeWithNode operation = new ReplaceNodeWithNode(targetNodeId, flowNode);
        operation.execute(this);
    }

    public void replace(FlowNode targetNode, FlowNode replacingNode) {
        String targetNodeId = (targetNode == null) ? null : targetNode.getId();
        replace(targetNodeId, replacingNode);
    }

    public void replace(String targetNodeId, BpmnModelInstance replacingFragment) {
        ReplaceNodeWithFragment operation = new ReplaceNodeWithFragment(targetNodeId, replacingFragment);
        operation.execute(this);
    }

    public void replace(FlowNode targetNode, BpmnModelInstance replacingFragment) {
        String targetNodeId = (targetNode == null) ? null : targetNode.getId();
        replace(targetNodeId, replacingFragment);
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        ReplaceFragmentWithNode operation = new ReplaceFragmentWithNode(startingNodeId, endingNodeId, replacingNode);
        operation.execute(this);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        replace(startingNodeId, endingNodeId, replacingNode);
    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        ReplaceFragmentWithFragment operation = new ReplaceFragmentWithFragment(startingNodeId, endingNodeId, replacingFragment);
        operation.execute(this);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        replace(startingNodeId, endingNodeId, replacingFragment);
    }

    // Move
    public void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        MoveNode operation = new MoveNode(targetNodeId, newPositionAfterOfId, newPositionBeforeOfId);
        operation.execute(this);
    }

    public void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        String targetNodeId = (targetNode == null) ? null : targetNode.getId();
        String newPositionAfterOfId = (newPositionAfterOf == null) ? null : newPositionAfterOf.getId();
        String newPositionBeforeOfId = (newPositionBeforeOf == null) ? null : newPositionBeforeOf.getId();
        move(targetNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public void move(String targetStartingNodeId, String targetEndingNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        MoveFragment operation = new MoveFragment(targetStartingNodeId, targetEndingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
        operation.execute(this);
    }

    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        String targetStartingNodeId = (targetStartingNode == null) ? null : targetStartingNode.getId();
        String targetEndingNodeId = (targetEndingNode == null) ? null : targetEndingNode.getId();
        String newPositionAfterOfId = (newPositionAfterOf == null) ? null : newPositionAfterOf.getId();
        String newPositionBeforeOfId = (newPositionBeforeOf == null) ? null : newPositionBeforeOf.getId();
        move(targetStartingNodeId, targetEndingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    // Parallelize
    public void parallelize(String targetStartingNodeId, String targetEndingNodeId) {
        Parallelize operation = new Parallelize(targetStartingNodeId, targetEndingNodeId);
        operation.execute(this);
    }

    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) {
        String targetStartingNodeId = (targetStartingNode == null) ? null : targetStartingNode.getId();
        parallelize(targetStartingNodeId, targetEndingNode.getId());
    }

    // Split
    public void split(Task targetTask, BpmnModelInstance newSubProcessModel) {
        String taskId = (targetTask == null) ? null : targetTask.getId();
        Split operation = new Split(taskId, newSubProcessModel);
        operation.execute(this);
    }


    // Insert
    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        String afterOfId = (afterOf == null) ? null : afterOf.getId();
        String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
        InsertNode operation = new InsertNode(afterOfId, beforeOfId, flowNodeToInsert);
        operation.execute(this);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        String afterOfId = (afterOf == null) ? null : afterOf.getId();
        String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
        InsertFragment operation = new InsertFragment(afterOfId, beforeOfId, fragmentToInsert);
        operation.execute(this);
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
         insert(afterOf, beforeOf, (BpmnModelInstance) fragmentToInsert.getModelInstance());
    }

    // Conditional Insert
    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        String afterOfId = (afterOf == null) ? null : afterOf.getId();
        String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
        ConditionalInsertNode operation = new ConditionalInsertNode(afterOfId, beforeOfId, flowNodeToInsert, condition, inLoop);
        operation.execute(this);
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        String afterOfId = (afterOf == null) ? null : afterOf.getId();
        String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
        ConditionalInsertFragment operation = new ConditionalInsertFragment(afterOfId, beforeOfId, fragmentToInsert, condition, inLoop);
        operation.execute(this);
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(afterOf, beforeOf, (BpmnModelInstance) fragmentToInsert.getModelInstance(), condition, inLoop);
    }
}