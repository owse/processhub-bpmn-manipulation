package org.prisma.processhub.bpmn.manipulation.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementHandler;

public class InsertFragment extends BpmnOperation {
    private String afterOfId;
    private String beforeOfId;
    private BpmnModelInstance fragmentToInsert;

    public InsertFragment(String afterOfId, String beforeOfId, BpmnModelInstance fragmentToInsert) {
        this.afterOfId = afterOfId;
        this.beforeOfId = beforeOfId;
        this.fragmentToInsert = fragmentToInsert;
    }

    public void execute(BpmnModelInstance modelInstance) {
        BpmnElementHandler.insert(modelInstance, afterOfId, beforeOfId, fragmentToInsert);
    }

    public String getAfterOfId() { return afterOfId; }
    public String getBeforeOfId() { return beforeOfId; }
    public BpmnModelInstance getFragmentToInsert() {
        return fragmentToInsert;
    }
}