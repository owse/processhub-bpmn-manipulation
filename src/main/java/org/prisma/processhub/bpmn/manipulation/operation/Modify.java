package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class Modify extends BpmnOperation {
    String modifiedElementId;
    String property;
    String value;

    public String getModifiedElementId() {
        return modifiedElementId;
    }
    public String getProperty() {
        return property;
    }
    public String getValue() {
        return value;
    }

    public Modify(String modifiedElementId, String property, String value) {
        this.modifiedElementId = modifiedElementId;
        this.property = property;
        this.value = value;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.modify(modelInstance, modifiedElementId, property, value);
    }


}