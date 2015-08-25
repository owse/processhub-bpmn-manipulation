package org.prisma.processhub.bpmn.manipulation.impl.bpmnt;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.camunda.CamundaListImpl;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaList;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.impl.tailoring.TailorableBpmnModelInstanceImpl;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
import org.prisma.processhub.bpmn.manipulation.util.BpmnHelper;

import java.util.ArrayList;
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

    private boolean isBpmntLogInitialized() {
        return bpmntLog != null;
    }

    private void addOperation(BpmntOperation operation) {
        if (bpmntLog == null) {
            if (operation instanceof Extend) {
                init((Extend) operation);
            }
        }
        else {
            operation.setExecutionOrder(bpmntLog.size() + 1);
            bpmntLog.add(operation);
        }
    }

    public void init(Extend extend) {
        if (bpmntLog != null) {
            return;
        }
        bpmntLog = new ArrayList<BpmntOperation>();
        extend.setExecutionOrder(1);
        bpmntLog.add(extend);
    }


    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Extend
    // Create a BPMNt model
    public BpmntModelInstance extend() {
        BpmnHelper.checkInvalidArgument(isBpmntLogInitialized(), "Unable to call Extend() again on this object");

        BpmntModelInstanceImpl bpmntModelInstance = (BpmntModelInstanceImpl) BpmnElementCreator.copyModelInstance(this);

        Process process = BpmnElementSearcher.findFirstProcess(bpmntModelInstance);

        // Initialize the BPMNt log
        Extend ext = new Extend(process.getId());
        bpmntModelInstance.init(ext);

        process.setId(ext.getNewProcessId());
        return bpmntModelInstance;
    }
    // Contribute
    // Add a new single process element to the given parent element
    public <T extends FlowElement, E extends ModelElementInstance> T contribute(E parentElement, T element) {
        T newElement = super.contribute(parentElement, element);
        T copiedElement = BpmnElementCreator.copyElement(element);
        addOperation(new ContributeCustomParent(parentElement.getAttributeValue("id"), copiedElement));
        return newElement;
    }

    // Add a new single element to the first process in this model as parent
    public <T extends FlowElement> T contribute(T element) {
        T newElement = super.contribute(element);
        T copiedElement = BpmnElementCreator.copyElement(element);
        addOperation(new Contribute(copiedElement));
        return newElement;
    }

    // Suppress
    // Remove flow element leaving the rest of the model untouched
    public <T extends FlowElement> void suppress(T element) {
        super.suppress(element);
        addOperation(new Suppress(element.getId()));
    }

    // Remove every element in collection
    public <T extends FlowElement> void suppress(Collection<T> elements) {
        super.suppress(elements);
        for (T element : elements) {
            addOperation(new Suppress(element.getId()));
        }
    }

    // Remove flow element by id
    public void suppress(String elementId) {
        FlowElement targetElement = getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        suppress(targetElement);
    }

    // Modify
    // Modify a property of a flow element
    public <T extends FlowElement> void modify(T element, String property, String value) {
        super.modify(element, property, value);
        addOperation(new Modify(element.getId(), property, value));
    }

    // Modify a property of a flow element with given id
    public void modify(String elementId, String property, String value) {
        FlowElement targetElement = getModelElementById(elementId);
        // If element not found throw exception
        BpmnHelper.checkElementPresent(targetElement != null, "Flow Element with id \'" + elementId +  "\' not found");
        modify(targetElement, property, value);

    }


    // High-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Rename
    public void rename(FlowElement element, String newName) {
        super.rename(element, newName);
        addOperation(new Rename(element.getId(), newName));
    }

    public void rename(String elementId, String newName) {
        FlowElement element = getModelElementById(elementId);
        BpmnHelper.checkElementPresent(element != null, "Flow Element with id \'" + elementId + "\' not found");
        rename(element, newName);
    }

    // Delete
    // Delete a node, all sequence flows connected to it and also obsolete gateways
    public void delete(FlowNode node){
        super.delete(node);
        addOperation(new DeleteNode(node.getId()));
    }

    // Delete element by id
    public void delete(String nodeId) {
        FlowNode node = getModelElementById(nodeId);
        BpmnHelper.checkElementPresent(node != null, "Flow Node with id \'" + nodeId + "\' not found");
        delete(node);
    }

    // Delete range of elements from startingNode to endingNode
    public void delete(FlowNode startingNode, FlowNode endingNode) {
        super.delete(startingNode, endingNode);
        addOperation(new DeleteFragment(startingNode.getId(), endingNode.getId()));
    }

    public void delete(String startingNodeId, String endingNodeId) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");

        delete(startingNode, endingNode);
    }

    // Replace
    public void replace(FlowNode existingNode, FlowNode replacingNode) {
        super.replace(existingNode, replacingNode);
        FlowNode copiedNode = BpmnElementCreator.copyElement(replacingNode);
        addOperation(new ReplaceNodeWithNode(existingNode.getId(), copiedNode));
    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingNode);    }

    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        super.replace(existingNode, replacingFragment);
        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);
        addOperation(new ReplaceNodeWithFragment(existingNode.getId(), replacingFragment));
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingFragment);    }

    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        super.replace(startingNode, endingNode, replacingNode);
        FlowNode copiedNode = BpmnElementCreator.copyElement(replacingNode);
        addOperation(new ReplaceFragmentWithNode(startingNode.getId(), endingNode.getId(), copiedNode));
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(startingNode, endingNode, replacingNode);
    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        super.replace(startingNode, endingNode, replacingFragment);
        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);
        addOperation(new ReplaceFragmentWithFragment(startingNode.getId(), endingNode.getId(), replacingFragment));
    }

    public void replace(String startingNodeId, String endingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(startingNode, endingNode, replacingFragment);
    }

    // Move
    public void move(FlowNode targetNode, FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        super.move(targetNode, newPositionAfterOf, newPositionBeforeOf);
        if (newPositionAfterOf == null) {
            addOperation(new MoveNode(targetNode.getId(), null, newPositionBeforeOf.getId()));
        }
        else if (newPositionBeforeOf == null) {
            addOperation(new MoveNode(targetNode.getId(), newPositionAfterOf.getId(), null));
        }
        else {
            addOperation(new MoveNode(targetNode.getId(), newPositionAfterOf.getId(), newPositionBeforeOf.getId()));
        }

    }

    public void move(String targetNodeId, String newPositionAfterOfId, String newPositionBeforeOfId) {
        FlowNode targetNode = getModelElementById(targetNodeId);

        BpmnHelper.checkNotNull(targetNode, "targetNode not found");

        FlowNode newPositionAfterOf = getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = getModelElementById(newPositionBeforeOfId);

        BpmnHelper.checkInvalidArgument(newPositionAfterOf == newPositionBeforeOf, "New position not set");

        move(targetNode, newPositionAfterOf, newPositionBeforeOf);
    }

    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode,
                     FlowNode newPositionAfterOf, FlowNode newPositionBeforeOf) {
        super.move(targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);
        if (newPositionAfterOf == null) {
            addOperation(new MoveFragment(targetStartingNode.getId(), targetEndingNode.getId(),
                                            null, newPositionBeforeOf.getId()));
        }
        else if (newPositionBeforeOf == null) {
            addOperation(new MoveFragment(targetStartingNode.getId(), targetEndingNode.getId(),
                                        newPositionAfterOf.getId(), null));
        }
        else {
            addOperation(new MoveFragment(targetStartingNode.getId(), targetEndingNode.getId(),
                                            newPositionAfterOf.getId(), newPositionBeforeOf.getId()));
        }
    }

    public void move(String targetStartingNodeId, String targetEndingNodeId,
                     String newPositionAfterOfId, String newPositionBeforeOfId) {

        FlowNode targetStartingNode = getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = getModelElementById(targetEndingNodeId);
        FlowNode newPositionAfterOf = getModelElementById(newPositionAfterOfId);
        FlowNode newPositionBeforeOf = getModelElementById(newPositionBeforeOfId);

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        move(targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);    }

    // Parallelize
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode) throws Exception {
        super.parallelize(targetStartingNode, targetEndingNode);
        addOperation(new Parallelize(targetStartingNode.getId(), targetEndingNode.getId()));
    }

    public void parallelize(String targetStartingNodeId, String targetEndingNodeId) throws Exception {
        FlowNode targetStartingNode = getModelElementById(targetStartingNodeId);
        FlowNode targetEndingNode = getModelElementById(targetEndingNodeId);

        // Check null arguments
        BpmnHelper.checkNotNull(targetStartingNode, "Argument targetStartingNode must not be null");
        BpmnHelper.checkNotNull(targetEndingNode, "Argument targetEndingNode must not be null");

        parallelize(targetStartingNode, targetEndingNode);
    }

    // Split
    public void split(Task targetTask, BpmnModelInstance newSubProcessModel){
        super.split(targetTask, newSubProcessModel);
        newSubProcessModel = BpmnElementCreator.copyModelInstance(newSubProcessModel);
        addOperation(new Split(targetTask.getId(), newSubProcessModel));
    }

    // Insert
    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        super.insert(afterOf, beforeOf, flowNodeToInsert);
        FlowNode copiedNode = BpmnElementCreator.copyElement(flowNodeToInsert);
        if (afterOf == null) {
            addOperation(new InsertNode(null, beforeOf.getId(), copiedNode));
        }
        else if (beforeOf == null) {
            addOperation(new InsertNode(afterOf.getId(), null, copiedNode));
        }
        else {
            addOperation(new InsertNode(afterOf.getId(), beforeOf.getId(), copiedNode));
        }
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        super.insert(afterOf, beforeOf, fragmentToInsert);
        BpmnModelInstance fragmentToInsertModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsertModel = BpmnElementCreator.copyModelInstance(fragmentToInsertModel);
        if (afterOf == null) {
            addOperation(new InsertFragment(null, beforeOf.getId(), fragmentToInsertModel));
        }
        else if (beforeOf == null) {
            addOperation(new InsertFragment(afterOf.getId(), null, fragmentToInsertModel));
        }
        else {
            addOperation(new InsertFragment(afterOf.getId(), beforeOf.getId(), fragmentToInsertModel));
        }
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
    }

    // Conditional Insert
    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert,
                                  String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, flowNodeToInsert, condition, inLoop);
        FlowNode copiedNode = BpmnElementCreator.copyElement(flowNodeToInsert);
        if (afterOf == null) {
            addOperation(new ConditionalInsertNode(null, beforeOf.getId(), copiedNode, condition, inLoop));
        }
        else if (beforeOf == null) {
            addOperation(new ConditionalInsertNode(afterOf.getId(), null, copiedNode, condition, inLoop));
        }
        else {
            addOperation(new ConditionalInsertNode(afterOf.getId(), beforeOf.getId(), copiedNode, condition, inLoop));
        }
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,
                                  String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, fragmentToInsert, condition, inLoop);
        BpmnModelInstance fragmentToInsertModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsertModel = BpmnElementCreator.copyModelInstance(fragmentToInsertModel);

        if (afterOf == null) {
            addOperation(new ConditionalInsertFragment(null, beforeOf.getId(), fragmentToInsertModel,
                                                        condition, inLoop));

        }
        else if (beforeOf == null) {
            addOperation(new ConditionalInsertFragment(afterOf.getId(), null, fragmentToInsertModel,
                                                        condition, inLoop));
        }
        else {
            addOperation(new ConditionalInsertFragment(afterOf.getId(), beforeOf.getId(), fragmentToInsertModel,
                                                        condition, inLoop));
        }
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert,  String condition, boolean inLoop) {
        conditionalInsert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert),
                            condition, inLoop);
    }
}
