package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;

public abstract class BpmntOperation {

    protected int executionOrder;

    public String getName() { return this.getClass().getSimpleName(); }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }

    public abstract void execute(BpmnModelInstance modelInstance);

    public abstract void generateExtensionElement(Process process);

    protected ModelElementInstance initExtensionElement(BaseElement element) {
        // Add operation extensions with its attributes
        ModelElementInstance currentExtension = element.getExtensionElements()
                            .addExtensionElement(BpmntExtensionAttributes.DOMAIN, getName());
        currentExtension.setAttributeValue(BpmntExtensionAttributes.ORDER, Integer.toString(executionOrder));
        return currentExtension;
    }
}
