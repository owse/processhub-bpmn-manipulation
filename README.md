# processhub-bpmn-manipulation
## Introduction
Processhub API to manipulate BPMN process. This API provides custom operators for tailoring and process composition by extending the camunda-bpmn-model functions to offer the required operators.

For every operator, the BPMN models will be represented as an instance of the class BpmnModelInstance from Camunda's API.

## Composition Operators

The composition operators are functions that take two or more processes as input and generate a new one. The resulting process represents the composition of the processes provided. There are different strategies to perform a processes composition. Each available operator is described below.

### 1. Serial Composition
This operator takes a list of two or more processes and perform a serial composition. It "glues" the end of a process to the start of the next one. As it relies on BPMN start and end events to identify where to connect the processes, the operator requires that every process have exactly only one start event and one end event.

A simple example  is shown in Figure 01.

![alt text](https://github.com/owse/processhub-bpmn-manipulation/blob/master/docs/images/SerialComposition-1.png)
*Figure 01 -Simple serial composition scenario*

The above scenario could be achieved by the execution of the following code:

```java
BpmnModelInstance modelInstance1 = Bpmn.readModelFromFile(new File("simple_diagram1.bpmn"));
BpmnModelInstance modelInstance2 = Bpmn.readModelFromFile(new File("simple_diagram2.bpmn"));
List<BpmnModelInstance> modelsToJoin = new ArrayList<BpmnModelInstance>();

modelsToJoin.add(modelInstance1);
modelsToJoin.add(modelInstance2);

BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();
BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInSeries(modelsToJoin);
```

### 2. Parallel Composition
This operator takes a list of two or more processes and perform a parallel composition. It adds (if necessary) parallel gateways after the start event and before the end event of the first process and connects the other processes ends to these gateways. Possible scenarios are showed in Figures 02, 03 and 04.

![alt text](https://github.com/owse/processhub-bpmn-manipulation/blob/master/docs/images/ParallelComposition-1.png)
*Figure 02 - Simple parallel composition scenario*


![alt text](https://github.com/owse/processhub-bpmn-manipulation/blob/master/docs/images/ParallelComposition-2.png)
*Figure 03 - Parallel composition scenario with gateways already  present in the process*


![alt text](https://github.com/owse/processhub-bpmn-manipulation/blob/master/docs/images/ParallelComposition-3.png)
*Figure 04 - Parallel composition scenario with only one gateway already  present in the process*


The above scenarios can be achieved by the following code:

```java
BpmnModelInstance modelInstance1 = Bpmn.readModelFromFile(new File("simple_diagram1.bpmn"));
BpmnModelInstance modelInstance2 = Bpmn.readModelFromFile(new File("simple_diagram2.bpmn"));
List<BpmnModelInstance> modelsToJoin = new ArrayList<BpmnModelInstance>();

modelsToJoin.add(modelInstance1);
modelsToJoin.add(modelInstance2);

BpmnModelComposer bpmnModelComposer = new BpmnModelComposer();
BpmnModelInstance resultModel = bpmnModelComposer.joinModelsInParallel(modelsToJoin);
```

### Restrictions
Currently, the operations rely on the assumption that process to be joined has exactly one start event and one end events. It's also assumed that the processes are valid. The operators work only if both conditions are met.


