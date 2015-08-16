# processhub-bpmn-manipulation

## Table of contents
- [Introduction](#introduction)
- [Composition Operators](#composition-operators)
	- [1. Serial Composition](#1-serial-composition)
	- [2. Parallel Composition](#2-parallel-composition)
- [Tailoring Operators](#tailoring-operators)
	- [1. Rename](#1-rename)
	- [2. Delete](#2-delete)
	- [3. Replace](#3-replace)
	- [4. Move](#4-move)
	- [5. Parallelize](#5-parallelize)
	- [6. Split](#6-split)
	- [7. Insert](#7-insert)
	- [8. Conditional Insert](#8-conditional-insert)


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

## Tailoring Operators

Tailoring operators are functions that modify an existing process elements in order to adapt it to a new context. The operators will be supported in an interface and implementation that extends BpmnModelInstance with tailoring operations. The new interface is called BpmntModelInstance and the following operations are supported:

### 1. Rename
Rename a flow element.

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Pass element id and new name
modelInstance.rename(flowElement.getId(), "New Name");
// Or pass the element already renamed as parameter
modelInstance.rename(renamedFlowElement);
```
### 2. Delete
Delete a flow node or a set of flow nodes from the model. Currently, the nodes before and after the set to be deleted are connected with a single sequence flow.
#### Restrictions:
* Start or end events cannot be deleted
* A gateway cannot be deleted singly
* Process fragments containing incomplete flow branches cannot be deleted

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Delete a single node
modelInstance.delete(flowNode);
// or delete a range of elements from startingNode to endingNode
modelInstance.delete(startingNode, endingNode);
```

### 3. Replace
Replace a flow node or a set of flow nodes from the model. The targets can be replaced by a flow node or a process.
#### Restrictions:
* Start or end events can only be placed by other start and end events, respectively
* Gateways cannot be replaced
* Process fragments containing incomplete flow branches cannot be replaced

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Replace a node with another node
modelInstance.replace(targetNode, replacingNode);

// Replace a node with a fragment
modelInstance.replace(targetNode, replacingFragment);

// Replace a fragment with a node
modelInstance.replace(startingNode, endingNode, replacingNode);

// Replace a fragment with another fragment
modelInstance.replace(startingNode, endingNode, replacingFragment);

```


### 4. Move
Move a flow node or a set of flow nodes to another part of the model, marked by the nodes newPositionAfterOf and newPositionBeforeOf.
#### Restrictions:
* If only the parameter newPositionAfterOf is provided, it cannot link to a diverging gateway or end event
* If only the parameter newPositionBeforeOf is provided, it cannot link to a converging gateway or start event
* If both parameters newPositionAfterOf and newPositionBeforeOf are set, they must be consecutive nodes
* Start or end events cannot be moved
* Gateways cannot be moved singly
* Process fragments containing incomplete flow branches cannot be moved


```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Move a node
modelInstance.move(targetNode, newPositionAfterOf, newPositionBeforeOf);
// Or
modelInstance.move(targetNode, newPositionAfterOf, null);
// Or
modelInstance.move(targetNode, null, newPositionBeforeOf);

// Move a set of nodes
modelInstance.move(targetStartingNode, targetEndingNode, newPositionAfterOf, newPositionBeforeOf);
// Or
modelInstance.move(targetStartingNode, targetEndingNode, newPositionAfterOf, null);
// Or
modelInstance.move(targetStartingNode, targetEndingNode, null, newPositionBeforeOf);
```

### 5. Parallelize
Parallelize the execution of elements from a process fragment that has been defined as a sequential flow
#### Restrictions:
* The target fragment cannot contain start or end events
* The target fragment cannot include gateways

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Parallelize nodes between targetStartingNode and targetEndingNode (both included)
modelInstance.move(targetStartingNode, targetEndingNode);
```

### 6. Split
Split a single task into a subprocess that details its procedure. Can be seen as task replaced by a subprocess
#### Restrictions:
* Only single tasks can be splitted
* This operation can be defined only by subprocesses

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Split the target task in a subprocess (newSubProcessModel is a process inside another BpmnModelInstance)
modelInstance.split(targetNode, newSubProcessModel);
```


### 7. Insert
Insert a flow node or a set of flow nodes between afterOf and beforeOf nodes. If afterOf and beforeOf are consecutive nodes, the new nodes are inserted in series. Else, the new nodes are inserted in parallel to the nodes between afterOf and beforeOf
#### Restrictions:
* If only the parameter afterOf is provided, it cannot link to a diverging gateway or end event
* If only the parameter beforeOf is provided, it cannot link to a converging gateway or start event
* Gateways cannot be inserted singly;
* The fragment between the parameters afterOf and beforeOf cannot contain incomplete flow branches
* Process fragments containing incomplete flow branches cannot be inserted.

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Insert a node
modelInstance.insert(afterOf, beforeOf, flowNodeToInsert);
// Or
modelInstance.insert(afterOf, null, flowNodeToInsert);
// Or
modelInstance.insert(null, beforeOf, flowNodeToInsert);

// Insert a set of nodes (fragmentToInsert can be a BpmnModelInstance or a Process)
modelInstance.insert(afterOf, beforeOf, fragmentToInsert);
// Or
modelInstance.insert(afterOf, null, fragmentToInsert);
// Or
modelInstance.insert(null, beforeOf, fragmentToInsert);
```

### 8. Conditional Insert
Insert a flow node or a set of flow nodes between afterOf and beforeOf nodes. The included nodes are only executed with the given condition
#### Restrictions:
* Gateways cannot be inserted singly;
* The fragment between the parameters afterOf and beforeOf cannot contain incomplete flow branches
* Process fragments containing incomplete flow branches cannot be inserted.

```java
BpmntModelInstance modelInstance = Bpmnt.readModelFromFile(new File("simple_diagram.bpmn"));
// Insert a node
void conditionalInsert(afterOf, beforeOf, flowNodeToInsert, condition, inLoop);

// Insert a set of nodes (fragmentToInsert can be a BpmnModelInstance or a Process)
void conditionalInsert(afterOf, beforeOf, flowNodeToInsert, condition, inLoop);
```
