package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class InsertFragment extends BpmntOperation {
    private String afterOfId;
    private String BeforeOfId;
    private BpmnModelInstance fragmentToInsert;

    public InsertFragment(int executionOrder, String afterOfId, String BeforeOfId, BpmnModelInstance fragmentToInsert) {
        this.executionOrder = executionOrder;
        this.afterOfId = afterOfId;
        this.BeforeOfId = BeforeOfId;
        this.fragmentToInsert = fragmentToInsert;
        this.name = "InsertFragment";
    }

    public String getAfterOfId() {
        return afterOfId;
    }

    public String getBeforeOfId() {
        return BeforeOfId;
    }

    public BpmnModelInstance getFragmentToInsert() {
        return fragmentToInsert;
    }
}