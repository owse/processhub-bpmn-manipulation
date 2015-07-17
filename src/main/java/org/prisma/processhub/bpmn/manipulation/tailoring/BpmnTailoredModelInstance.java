package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.prisma.processhub.bpmn.manipulation.tailoring.BpmnExtendedModelInstance;

// Interface that extends the Camunda API with low-level tailoring operations
public interface BpmnTailoredModelInstance extends BpmnExtendedModelInstance {
    void rename(FlowElement targetElement);
    void delete(FlowNode targetNode);
    void delete(FlowNode startingNode, FlowNode endingNode);
    void replace(FlowNode targetNode, Process replacingFragment);
    void replace(FlowNode targetStartingNode, FlowNode targetEndingNode, Process replacingFragment);
    void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode);
    void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode);
    void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode);
    void split(Task targetTask, Process newSubProcess);
    void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
    void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
    void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
}
