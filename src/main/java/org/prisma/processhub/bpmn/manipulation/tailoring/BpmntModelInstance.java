package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;

//import java.util.List;

// Interface that extends the Camunda API with tailoring operations
public interface BpmntModelInstance extends BpmnModelInstance {

    // Low-level operations
    void suppress (FlowElement targetElement);

    // Due to Camunda API's fluent builder API, makes no sense to implement it
    //void contribute (FlowElement targetElement);

    // Not in use
    //void extend (BpmnModelInstance modelInstance);
    //void modify (FlowElement targetElement, List<String> properties);


    // High-level operations
    void rename(FlowElement targetElement);
    void rename(String id, String newName);

    void delete(FlowNode targetNode);
    void delete(FlowNode startingNode, FlowNode endingNode);
    void replace(FlowNode targetNode, org.camunda.bpm.model.bpmn.instance.Process replacingFragment);
    void replace(FlowNode targetStartingNode, FlowNode targetEndingNode, Process replacingFragment);
    void move(FlowNode targetNode, FlowNode beforeNode, FlowNode afterNode);
    void move(FlowNode targetStartingNode, FlowNode targetEndingNode, FlowNode beforeNode, FlowNode afterNode);
    void parallelize(FlowNode targetStartingNode, FlowNode targetEndingNode);
    void split(Task targetTask, Process newSubProcess);
    void insertInSeries(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
    void insertWithCondition(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
    void insertInParallel(FlowNode beforeNode, FlowNode afterNode, Process fragmentToInsert);
}
