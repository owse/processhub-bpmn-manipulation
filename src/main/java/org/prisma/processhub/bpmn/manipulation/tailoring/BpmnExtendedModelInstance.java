package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;

//import java.util.List;

// Interface that extends the Camunda API with low-level tailoring operations
public interface BpmnExtendedModelInstance {
    BpmnModelInstance getModelInstance();
    void setModelInstance(BpmnModelInstance modelInstance);
    void suppress (FlowElement targetElement);

    // TODO: Fix it
    //void contribute (FlowElement targetElement);

    // Not in user
    //void extend (BpmnModelInstance modelInstance);
    //void modify (FlowElement targetElement, List<String> properties);
}
