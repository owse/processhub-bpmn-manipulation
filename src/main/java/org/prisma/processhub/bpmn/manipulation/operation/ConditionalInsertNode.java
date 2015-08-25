package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class ConditionalInsertNode extends BpmnOperation {
    private String afterOfId;
    private String beforeOfId;
    private FlowNode flowNodeToInsert;
    String condition;
    boolean inLoop;

    public ConditionalInsertNode(String afterOfId, String beforeOfId,
                                 FlowNode flowNodeToInsert, String condition, boolean inLoop) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.flowNodeToInsert = flowNodeToInsert;
        this.condition = condition;
        this.inLoop = inLoop;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.conditionalInsert(modelInstance, afterOfId, beforeOfId, flowNodeToInsert, condition, inLoop);
    }

    public String getAfterOfId() { return afterOfId; }
    public String getBeforeOfId() { return beforeOfId; }
    public FlowNode getFlowNodeToInsert() {
        return flowNodeToInsert;
    }
    public String getCondition() {
        return condition;
    }
    public boolean isInLoop() {
        return inLoop;
    }
}