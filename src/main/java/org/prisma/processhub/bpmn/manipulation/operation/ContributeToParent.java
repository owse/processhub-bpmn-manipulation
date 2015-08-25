package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ContributeToParent extends BpmnOperation {
    private FlowElement newElement;
    private ModelElementInstance parentElement;

    public FlowElement getNewElement() {
        return newElement;
    }
    public ModelElementInstance getParentElement() {
        return parentElement;
    }

    public ContributeToParent(ModelElementInstance parentElement, FlowElement newElement) {
        this.newElement = newElement;
        this.parentElement = parentElement;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.contribute(modelInstance, parentElement, newElement);
    }
}
