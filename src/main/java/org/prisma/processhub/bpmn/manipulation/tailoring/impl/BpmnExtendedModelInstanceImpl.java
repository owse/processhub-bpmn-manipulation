package org.prisma.processhub.bpmn.manipulation.tailoring.impl;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmnExtendedModelInstance;

import java.util.Collection;
import java.util.Iterator;

public class BpmnExtendedModelInstanceImpl implements BpmnExtendedModelInstance {
    protected BpmnModelInstance modelInstance;

    public void BpmnExtendedModelInstanceImpl (BpmnModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

    public BpmnModelInstance getModelInstance () {
        return modelInstance;
    }

    public void setModelInstance (BpmnModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

    public void suppress (FlowElement targetElement) {
        Collection<FlowElement> flowElements = modelInstance.getModelElementsByType(Process.class).iterator().next().getFlowElements();
        FlowElement targetElementInModel = modelInstance.getModelElementById(targetElement.getId());
        if (targetElementInModel != null) {
            flowElements.remove(targetElementInModel);
        }
        return;
    }

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
