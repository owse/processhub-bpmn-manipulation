package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;
import org.prisma.processhub.bpmn.manipulation.util.BpmnHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BpmntModelInstanceImpl extends BpmnModelInstanceImpl implements BpmntModelInstance {

    // Constructor
    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    private List<BpmntOperation> bpmntLog;

    // BPMNt log operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    public List<BpmntOperation> getBpmntLog() {
        return bpmntLog;
    }

    private boolean isBpmntLogInitialized() {
        return bpmntLog != null;
    }

    public void setBpmntLog(List<BpmntOperation> bpmntLog) {
        this.bpmntLog = bpmntLog;
    }

    protected void addOperation(BpmntOperation operation) {
        if (bpmntLog == null) {
            if (operation instanceof Extend) {
                init((Extend) operation);
            }
        } else {
            operation.setExecutionOrder(bpmntLog.size() + 1);
            bpmntLog.add(operation);
        }
    }

    public void init(Extend extend) {
        if (bpmntLog == null) {
            bpmntLog = new ArrayList<BpmntOperation>();
            extend.setExecutionOrder(1);
            bpmntLog.add(extend);
        }
    }

    public int getNumberOperations() {
        if (isBpmntLogInitialized())
            return bpmntLog.size();
        return 0;
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
    // Add a new single process element to the given parent element
    public <T extends FlowElement, E extends ModelElementInstance> void contribute(E parentElement, T element) {
        if (isBpmntLogInitialized()) {
            String parentElementId = (parentElement == null) ? null : parentElement.getAttributeValue("id");
            element = BpmnElementHandler.copyElement(element);
            ContributeToParent operation = new ContributeToParent(parentElementId, element);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Add a new single element to the first process in this model as parent
    public <T extends FlowElement> void contribute(T element) {
        if (isBpmntLogInitialized()) {
            element = BpmnElementHandler.copyElement(element);
            Contribute operation = new Contribute(element);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Suppress
    // Remove flow element leaving the rest of the model untouched
    public <T extends FlowElement> void suppress(T element) {
        String elementId = (element == null) ? null : element.getId();
        suppress(elementId);
    }

    // Remove every element in collection
    public <T extends FlowElement> void suppress(Collection<T> elements) {
        for (FlowElement element : elements) {
            suppress(element.getId());
        }
    }

    // Remove flow element by id
    public void suppress(String elementId) {
        if (isBpmntLogInitialized()) {
            Suppress operation = new Suppress(elementId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Modify
    // Modify a property of a flow element
    public <T extends FlowElement> void modify(T element, String property, String value) {
        String elementId = (element == null) ? null : element.getId();
        modify(elementId, property, value);
    }

    // Modify a property of a flow element with given id
    public void modify(String elementId, String property, String value) {
        if (isBpmntLogInitialized()) {
            Modify operation = new Modify(elementId, property, value);
            operation.execute(this);
            addOperation(operation);
        }
    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename
    public void rename(FlowElement element, String newName) {
        String elementId = (element == null) ? null : element.getId();
        rename(elementId, newName);
    }

    public void rename(String elementId, String newName) {
        if (isBpmntLogInitialized()) {
            Rename operation = new Rename(elementId, newName);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Delete
    // Delete a node, all sequence flows connected to it and also obsolete gateways
    public void delete(FlowNode node){
        String nodeId = (node == null) ? null : node.getId();
        delete(nodeId);
    }

    // Delete element by id
    public void delete(String nodeId) {
        if (isBpmntLogInitialized()) {
            DeleteNode operation = new DeleteNode(nodeId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Delete range of elements from startingNode to endingNode
    public void delete(FlowNode startingNode, FlowNode endingNode) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        delete(startingNodeId, endingNodeId);
    }

    public void delete(String startingNodeId, String endingNodeId) {
        if (isBpmntLogInitialized()) {
            DeleteFragment operation = new DeleteFragment(startingNodeId, endingNodeId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Replace
    public void replace(FlowNode existingNode, FlowNode replacingNode) {
        String existingNodeId = (existingNode == null) ? null : existingNode.getId();
        replace(existingNodeId, replacingNode);
    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        if (isBpmntLogInitialized()) {
            replacingNode = BpmnElementHandler.copyElement(replacingNode);
            ReplaceNodeWithNode operation = new ReplaceNodeWithNode(existingNodeId, replacingNode);
            operation.execute(this);
            addOperation(operation);
        }
    }


    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        String existingNodeId = (existingNode == null) ? null : existingNode.getId();
        replace(existingNodeId, replacingFragment);
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        if (isBpmntLogInitialized()) {
            replacingFragment = BpmnElementHandler.copyModelInstance(replacingFragment);
            ReplaceNodeWithFragment operation = new ReplaceNodeWithFragment(existingNodeId, replacingFragment);
            operation.execute(this);
            addOperation(operation);
        }
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        replace(startingNodeId, endingNodeId, replacingNode);
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        if (isBpmntLogInitialized()) {
            replacingNode = BpmnElementHandler.copyElement(replacingNode);
            ReplaceFragmentWithNode operation = new ReplaceFragmentWithNode(startingNodeId, endingNodeId, replacingNode);
            operation.execute(this);
            addOperation(operation);
        }
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        String startingNodeId = (startingNode == null) ? null : startingNode.getId();
        String endingNodeId = (endingNode == null) ? null : endingNode.getId();
        replace(startingNodeId, endingNodeId, replacingFragment);
    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        if (isBpmntLogInitialized()) {
            replacingFragment = BpmnElementHandler.copyModelInstance(replacingFragment);
            ReplaceFragmentWithFragment operation = new ReplaceFragmentWithFragment(startingNodeId, endingNodeId, replacingFragment);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Move
    public void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        String targetNodeId = (targetNode == null) ? null : targetNode.getId();
        String newPositionAfterOfId = (newPositionAfterOf == null) ? null : newPositionAfterOf.getId();
        String newPositionBeforeOfId = (newPositionBeforeOf == null) ? null : newPositionBeforeOf.getId();
        move(targetNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        if (isBpmntLogInitialized()) {
            MoveNode operation = new MoveNode(targetNodeId, newPositionAfterOfId, newPositionBeforeOfId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode,
                     FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        String targetStartingNodeId = (targetStartingNode == null) ? null : targetStartingNode.getId();
        String targetEndingNodeId = (targetEndingNode == null) ? null : targetEndingNode.getId();
        String newPositionAfterOfId = (newPositionAfterOf == null) ? null : newPositionAfterOf.getId();
        String newPositionBeforeOfId = (newPositionBeforeOf == null) ? null : newPositionBeforeOf.getId();
        move(targetStartingNodeId, targetEndingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
    }

    public void move(String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {
        if (isBpmntLogInitialized()) {
            MoveFragment operation = new MoveFragment(targetStartingNodeId, targetEndingNodeId, newPositionAfterOfId, newPositionBeforeOfId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Parallelize
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) {
        String targetStartingNodeId = (targetStartingNode == null) ? null : targetStartingNode.getId();
        parallelize(targetStartingNodeId, targetEndingNode.getId());
    }

    public void parallelize(String targetStartingNodeId, String targetEndingNodeId) {
        if (isBpmntLogInitialized()) {
            Parallelize operation = new Parallelize(targetStartingNodeId, targetEndingNodeId);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Split
    public void split(Task targetTask, BpmnModelInstance newSubProcessModel){
        if (isBpmntLogInitialized()) {
            String taskId = (targetTask == null) ? null : targetTask.getId();
            newSubProcessModel = BpmnElementHandler.copyModelInstance(newSubProcessModel);
            Split operation = new Split(taskId, newSubProcessModel);
            operation.execute(this);
            addOperation(operation);
        }
    }

    // Insert
    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        if (isBpmntLogInitialized()) {
            flowNodeToInsert = BpmnElementHandler.copyElement(flowNodeToInsert);
            String afterOfId = (afterOf == null) ? null : afterOf.getId();
            String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
            InsertNode operation = new InsertNode(afterOfId, beforeOfId, flowNodeToInsert);
            operation.execute(this);
            addOperation(operation);
        }
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        insert(afterOf, beforeOf, (BpmnModelInstance) fragmentToInsert.getModelInstance());
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        if (isBpmntLogInitialized()) {
            fragmentToInsert = BpmnElementHandler.copyModelInstance(fragmentToInsert);
            String afterOfId = (afterOf == null) ? null : afterOf.getId();
            String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();

            InsertFragment operation = new InsertFragment(afterOfId, beforeOfId, fragmentToInsert);

            operation.execute(this);
            addOperation(operation);
        }
    }

    // Conditional Insert
    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        if (isBpmntLogInitialized()) {
            flowNodeToInsert = BpmnElementHandler.copyElement(flowNodeToInsert);
            String afterOfId = (afterOf == null) ? null : afterOf.getId();
            String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
            ConditionalInsertNode operation = new ConditionalInsertNode(afterOfId, beforeOfId, flowNodeToInsert, condition, inLoop);
            operation.execute(this);
            addOperation(operation);
        }
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(afterOf, beforeOf, (BpmnModelInstance) fragmentToInsert.getModelInstance(), condition, inLoop);
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        if (isBpmntLogInitialized()) {
            String afterOfId = (afterOf == null) ? null : afterOf.getId();
            String beforeOfId = (beforeOf == null) ? null : beforeOf.getId();
            fragmentToInsert = BpmnElementHandler.copyModelInstance(fragmentToInsert);
            ConditionalInsertFragment operation = new ConditionalInsertFragment(afterOfId, beforeOfId, fragmentToInsert, condition, inLoop);
            operation.execute(this);
            addOperation(operation);
        }
    }
}
