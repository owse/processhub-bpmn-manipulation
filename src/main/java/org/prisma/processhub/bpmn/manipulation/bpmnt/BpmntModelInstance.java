package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.BpmntOperation;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;

import java.util.List;

public interface BpmntModelInstance extends TailorableBpmnModelInstance {
	
	public BpmnModelInstance getBpmntModel(); 
	
    List<BpmntOperation> getBpmntLog();

    void setBpmntLog(List<BpmntOperation> bpmntLog);

    int getNumberOperations();
}
