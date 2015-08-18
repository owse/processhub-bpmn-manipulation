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

    public boolean isBpmntLogInitialized() {
        return bpmntLog != null;
    }

    public void setBpmntLog(List<BpmntOperation> bpmntLog) {
        this.bpmntLog = bpmntLog;
    }

    public int getNumberOperations() {
        return bpmntLog.size();
    }


    // Low-level operations
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // Extend
    // Create a BPMNt model
    public BpmntModelInstance extend() {
        BpmnHelper.checkInvalidArgument(isBpmntLogInitialized(), "Unable to call Extend() again on this object");

        BpmntModelInstance bpmntModelInstance = BpmnElementCreator.copyModelInstance(this);

        Process process = BpmnElementSearcher.findFirstProcess(bpmntModelInstance);

        // Instance and populate the list of operations
        List<BpmntOperation> bpmntLog = new ArrayList<BpmntOperation>();
        Extend ext = new Extend(process.getId());
        bpmntLog.add(ext);
        bpmntModelInstance.setBpmntLog(bpmntLog);

        process.setId(ext.getNewProcessId());

        return bpmntModelInstance;
    }

    // Contribute
    // TODO: copy element before adding the operation to the BPMNt log
    // Add a new single process element to the given parent element
    public <T extends FlowElement, E extends ModelElementInstance> T contribute(E parentElement, T element) {
        T newElement = super.contribute(parentElement, element);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new ContributeCustomParent(getNumberOperations() + 1, parentElement, element));
        }
        return newElement;
    }

    // TODO: copy element before adding the operation to the BPMNt log
    // Add a new single element to the first process in this model as parent
    public <T extends FlowElement> T contribute(T element) {
        T newElement = super.contribute(element);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Contribute(getNumberOperations() + 1, element));
        }
        return newElement;
    }

    // Suppress
    // Remove flow element leaving the rest of the model untouched
    public <T extends FlowElement> void suppress(T element) {
        super.suppress(element);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Suppress(getNumberOperations() + 1, element.getId()));
        }
    }

    // Remove every element in collection
    public <T extends FlowElement> void suppress(Collection<T> elements) {
        super.suppress(elements);
        if (isBpmntLogInitialized()) {
            Collection<String> elementsIds = new ArrayList<String>();
            for (T element : elements) {
                elementsIds.add(element.getId());
            }
            bpmntLog.add(new SuppressAll(getNumberOperations() + 1, elementsIds));
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Modify(getNumberOperations() + 1, element.getId(), property, value));
        }
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Rename(getNumberOperations() + 1, element.getId(), newName));
        }
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new DeleteNode(getNumberOperations() + 1, node.getId()));
        }
    }

    // Delete element by id
    public void delete(String nodeId) {
        FlowNode node = getModelElementById(nodeId);
        BpmnHelper.checkElementPresent(node != null, "Flow Node with id \'" + nodeId +  "\' not found");
        delete(node);
    }

    // Delete range of elements from startingNode to endingNode
    public void delete(FlowNode startingNode, FlowNode endingNode) {
        super.delete(startingNode, endingNode);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new DeleteFragment(getNumberOperations() + 1, startingNode.getId(), endingNode.getId()));
        }
    }

    public void delete(String startingNodeId, String endingNodeId) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");

        delete(startingNode, endingNode);
    }

    // Replace
    // TODO: copy element before adding the operation to the BPMNt log
    public void replace(FlowNode existingNode, FlowNode replacingNode) {
        super.replace(existingNode, replacingNode);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new ReplaceNodeWithNode(getNumberOperations() + 1, existingNode.getId(), replacingNode));
        }
    }

    public void replace(String existingNodeId, FlowNode replacingNode) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingNode);    }

    public void replace(FlowNode existingNode, BpmnModelInstance replacingFragment) {
        super.replace(existingNode, replacingFragment);
        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new ReplaceNodeWithFragment(getNumberOperations() + 1, existingNode.getId(), replacingFragment));
        }
    }

    public void replace(String existingNodeId, BpmnModelInstance replacingFragment) {
        FlowNode existingNode = getModelElementById(existingNodeId);
        BpmnHelper.checkElementPresent(existingNode != null, "Flow Node with id \'" + existingNodeId + "\' not found");
        replace(existingNode, replacingFragment);    }

    // TODO: copy element before adding the operation to the BPMNt log
    public void replace(FlowNode startingNode, FlowNode endingNode, FlowNode replacingNode) {
        super.replace(startingNode, endingNode, replacingNode);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new ReplaceFragmentWithNode(
                            getNumberOperations() + 1,
                            startingNode.getId(),
                            endingNode.getId(),
                            replacingNode
                    )
            );
        }
    }

    public void replace(String startingNodeId, String endingNodeId, FlowNode replacingNode) {
        FlowNode startingNode = getModelElementById(startingNodeId);
        FlowNode endingNode = getModelElementById(endingNodeId);

        BpmnHelper.checkElementPresent(startingNode != null, "Flow Node with id \'" + startingNodeId + "\' not found");
        BpmnHelper.checkElementPresent(endingNode != null, "Flow Node with id \'" + endingNodeId + "\' not found");

        replace(startingNode, endingNode, replacingNode);    }

    public void replace(FlowNode startingNode, FlowNode endingNode, BpmnModelInstance replacingFragment) {
        super.replace(startingNode, endingNode, replacingFragment);
        replacingFragment = BpmnElementCreator.copyModelInstance(replacingFragment);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new ReplaceFragmentWithFragment(
                            getNumberOperations() + 1,
                            startingNode.getId(),
                            endingNode.getId(),
                            replacingFragment
                    )
            );
        }
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new MoveNode(
                            getNumberOperations() + 1,
                            targetNode.getId(),
                            newPositionAfterOf.getId(),
                            newPositionBeforeOf.getId()
                    )
            );
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new MoveFragment(
                            getNumberOperations() + 1,
                            targetStartingNode.getId(),
                            targetEndingNode.getId(),
                            newPositionAfterOf.getId(),
                            newPositionBeforeOf.getId()
                    )
            );
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Parallelize(getNumberOperations() + 1, targetStartingNode.getId(), targetEndingNode.getId()));
        }
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
        if (isBpmntLogInitialized()) {
            bpmntLog.add(new Split(getNumberOperations() + 1, targetTask.getId(), newSubProcessModel));
        }
    }

    // Insert
    // TODO: copy element before adding the operation to the BPMNt log
    public void insert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert) {
        super.insert(afterOf, beforeOf, flowNodeToInsert);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new InsertNode(
                            getNumberOperations() + 1,
                            afterOf.getId(),
                            beforeOf.getId(),
                            flowNodeToInsert
                    )
            );
        }
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert) {
        super.insert(afterOf, beforeOf, fragmentToInsert);
        BpmnModelInstance fragmentToInsertModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsertModel = BpmnElementCreator.copyModelInstance(fragmentToInsertModel);

        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new InsertFragment(
                            getNumberOperations() + 1,
                            afterOf.getId(),
                            beforeOf.getId(),
                            fragmentToInsertModel
                    )
            );
        }
    }

    public void insert(FlowNode afterOf, FlowNode beforeOf, BpmnModelInstance fragmentToInsert) {
        insert(afterOf, beforeOf, BpmnElementSearcher.findFirstProcess(fragmentToInsert));
    }

    // Conditional Insert
    // TODO: copy element before adding the operation to the BPMNt log
    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, flowNodeToInsert, condition, inLoop);
        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new ConditionalInsertNode(
                            getNumberOperations() + 1,
                            afterOf.getId(),
                            beforeOf.getId(),
                            flowNodeToInsert,
                            condition,
                            inLoop
                    )
            );
        }
    }

    public void conditionalInsert(FlowNode afterOf, FlowNode beforeOf, Process fragmentToInsert,  String condition, boolean inLoop) {
        super.conditionalInsert(afterOf, beforeOf, fragmentToInsert, condition, inLoop);
        BpmnModelInstance fragmentToInsertModel = (BpmnModelInstance) fragmentToInsert.getModelInstance();
        fragmentToInsertModel = BpmnElementCreator.copyModelInstance(fragmentToInsertModel);

        if (isBpmntLogInitialized()) {
            bpmntLog.add(
                    new ConditionalInsertFragment(
                            getNumberOperations() + 1,
                            afterOf.getId(),
                            beforeOf.getId(),
                            fragmentToInsertModel,
                            condition,
                            inLoop
                    )
            );
        }
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
