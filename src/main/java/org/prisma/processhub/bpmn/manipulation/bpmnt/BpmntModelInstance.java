package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.BpmntOperation;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;

import java.util.List;

public interface BpmntModelInstance extends TailorableBpmnModelInstance {
    List<BpmntOperation> getBpmntLog();
}
