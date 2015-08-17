package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

public class ContributeCustomParent extends Contribute {
    private ModelElementInstance parentElement;

    public ContributeCustomParent(int executionOrder, ModelElementInstance parentElement, FlowElement newElement) {
        super(executionOrder, newElement);
        this.parentElement = parentElement;
        name = "ContributeCustomParent";
    }

    public ModelElementInstance getParentElement() {
        return parentElement;
    }
}
