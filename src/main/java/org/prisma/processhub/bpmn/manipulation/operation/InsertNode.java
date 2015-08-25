package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class InsertNode extends BpmnOperation {
    private String afterOfId;
    private String beforeOfId;
    private FlowNode flowNodeToInsert;

    public InsertNode(String afterOfId, String beforeOfId, FlowNode flowNodeToInsert) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.insert(modelInstance, afterOfId, beforeOfId, flowNodeToInsert);
    }

    public String getAfterOfId() {
        return afterOfId;
    }
    public String getBeforeOfId() {
        return beforeOfId;
    }
    public FlowNode getFlowNodeToInsert() {
        return flowNodeToInsert;
    }
}