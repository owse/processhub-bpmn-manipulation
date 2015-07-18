package org.prisma.processhub.bpmn.manipulation.tailoring.impl;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmntModelInstance;

import java.util.Collection;

public class BpmntModelInstanceImpl extends BpmnModelInstanceImpl implements BpmntModelInstance {
    //protected BpmnModelInstance modelInstance;

    public BpmntModelInstanceImpl(ModelImpl model, ModelBuilder modelBuilder, DomDocument document) {
        super(model, modelBuilder, document);
    }

    public void suppress (FlowElement targetElement) {

        Collection<FlowElement> flowElements = getModelElementsByType(Process.class).iterator().next().getFlowElements();
        FlowElement targetElementInModel = getModelElementById(targetElement.getId());
        if (targetElementInModel != null) {
            flowElements.remove(targetElementInModel);
        }
        return;
    }

    public void rename(FlowElement targetElement) {
        FlowElement flowElementToRename = getModelElementById(targetElement.getId());
        flowElementToRename.setName(targetElement.getName());
        return;
    }

    public void rename(String id, String newName) {
        FlowElement flowElementToRename = getModelElementById(id);
        if (flowElementToRename != null) {
            flowElementToRename.setName(newName);
        }
        return;
    }

    public void delete(FlowNode targetNode){}
    public void delete(FlowNode startingNode, FlowNode endingNode){}
    public void replace(FlowNode targetNode, Process replacingFragment){}
    public void replace(FlowNode targetStartingNode, FlowNode targetEndingNode, Process replacingFragment){}
    public void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode){}
    public void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode){}
    public void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode){}
    public void split(Task targetTask, Process newSubProcess){}
    public void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}
    public void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert){}

    // TODO: insert appendTo code from BpmnModelComposer here
    /*
    public void contribute (FlowElement targetElement) {
        Collection<FlowElement> flowElements = modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements();
        Iterator<FlowElement> flowElementIterator = flowElements.iterator();
        while (flowElementIterator.hasNext()) {
            FlowElement currentElement = flowElementIterator.next();
            if (currentElement.getId().equals(targetElement.getId())) {
                return;
            }
        }
        flowElements.add(targetElement);
        return;
    }
    */

    //public void extend (BpmnModelInstance modelInstance);
    //public void modify (FlowElement targetElement, List<String> properties);

}
