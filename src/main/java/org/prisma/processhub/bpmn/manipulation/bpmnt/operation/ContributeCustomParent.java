package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

public class ContributeCustomParent extends Contribute {
    private String parentElementId;

    public ContributeCustomParent(int executionOrder, String parentElementId, FlowElement newElement) {
        super(executionOrder, newElement);
        this.parentElementId = parentElementId;
        name = "ContributeCustomParent";
    }

    public String getParentElementId() {
        return parentElementId;
    }
}
