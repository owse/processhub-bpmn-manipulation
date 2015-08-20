package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.impl.instance.*;
import org.camunda.bpm.model.bpmn.impl.instance.ExtensionImpl;
import org.camunda.bpm.model.bpmn.impl.instance.ProcessImpl;
import org.camunda.bpm.model.bpmn.impl.instance.bpmndi.*;
import org.camunda.bpm.model.bpmn.impl.instance.camunda.*;
import org.camunda.bpm.model.bpmn.impl.instance.dc.BoundsImpl;
import org.camunda.bpm.model.bpmn.impl.instance.dc.FontImpl;
import org.camunda.bpm.model.bpmn.impl.instance.dc.PointImpl;
import org.camunda.bpm.model.bpmn.impl.instance.di.*;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.xml.Model;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.camunda.bpm.model.xml.impl.util.IoUtil;
import org.camunda.bpm.model.xml.impl.util.ModelUtil;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementCreator;

import java.io.*;
import java.util.List;


public class Bpmnt {
    public static Bpmnt INSTANCE = new Bpmnt();
    private BpmntParser bpmntParser = new BpmntParser();
    private final ModelBuilder bpmntModelBuilder = ModelBuilder.createInstance("Tailorable BPMN Model");
    private Model BpmntModel;


    // BPMNt extension attributes
    //private static final String OPERATION = "operation";
    private static final String ORDER = "order";
    private static final String AFTER_OF_ID = "afterOfId";
    private static final String BEFORE_OF_ID = "beforeOfId";
    //final BpmnModelInstance fragmentToInsert;
    private static final String CONDITION = "condition";
    private static final String IN_LOOP = "inLoop";
    //private FlowNode flowNodeToInsert;
    // FlowElement newFlowElement
    private static final String PARENT_ELEMENT_ID = "parentId" ;
    // FlowElement newFlowElement
    private static final String STARTING_NODE_ID = "startingNodeId";
    private static final String ENDING_NODE_ID = "endingNodeId";
    private static final String NODE_ID = "nodeId";
    private static final String BASE_PROCESS_ID = "baseProcessId";
    private static final String NEW_PROCESS_ID = "newProcessId";
    private static final String MODIFIED_ELEMENT_ID = "modifiedId";
    private static final String PROPERTY = "property";
    private static final String VALUE = "value";
    private static final String NEW_POSITION_AFTER_OF_ID = AFTER_OF_ID;
    private static final String NEW_POSITION_BEFORE_OF_ID = BEFORE_OF_ID;
    private static final String ELEMENT_ID = "elementId";
    private static final String NEW_NAME = "newName";
    // private BpmnModelInstance replacingFragment;
    // private FlowNode replacingNode;
    private static final String REPLACED_NODE_ID = "replacedNodeId";
    private static final String TASK_ID = "taskId";
    //final BpmnModelInstance newSubProcessModel;
    private static final String SUPPRESSED_ELEMENT_ID = ELEMENT_ID;
    private static final String SUPPRESSED_ELEMENTS_IDS = "elementId";


    public static BpmnModelInstance convertBpmntFromListToModel (List<BpmntOperation> bpmntList) {

        Extend extend = null;

        for (BpmntOperation op: bpmntList) {
            if (op instanceof Extend) {
                extend = (Extend) op;
                break;
            }
        }

        if (extend == null) {
            return null;
        }

        BpmnModelInstance modelInstance = Bpmn.createProcess(extend.getNewProcessId())
                                            .name("Tailored_" + extend.getBaseProcessId()).done();

        Process process = modelInstance.getModelElementsByType(Process.class).iterator().next();

        process.setExtensionElements(modelInstance.newInstance(ExtensionElements.class));

        ModelElementInstance processExtension = process
                                                    .getExtensionElements()
                                                    .addExtensionElement("http://www.processhub.net", extend.getName());

        // Add the extension for the Extend operation
        //processExtension.setAttributeValue(OPERATION, extend.getName());
        processExtension.setAttributeValue(ORDER, Integer.toString(extend.getExecutionOrder()));
        processExtension.setAttributeValue(BASE_PROCESS_ID, extend.getBaseProcessId());
        processExtension.setAttributeValue(NEW_PROCESS_ID, extend.getNewProcessId());

        // Add extensions
        for (BpmntOperation op: bpmntList) {
            // Skip Extend operation
            if (op instanceof Extend) {}

            // Operations that depend only on the tailored model
            else if (op instanceof Suppress || op instanceof Modify || op instanceof Rename ||
                op instanceof DeleteNode || op instanceof DeleteFragment || op instanceof MoveNode ||
                op instanceof MoveFragment || op instanceof Parallelize)
            {
                ModelElementInstance currentExtension = process.getExtensionElements().addExtensionElement("http://www.processhub.net", op.getName());
                currentExtension.setAttributeValue(ORDER, Integer.toString(op.getExecutionOrder()));

                if (op instanceof Suppress) {
                    currentExtension.setAttributeValue(SUPPRESSED_ELEMENT_ID, ((Suppress) op).getSuppressedElementId());
                }
                else if (op instanceof Modify) {
                    currentExtension.setAttributeValue(MODIFIED_ELEMENT_ID, ((Modify) op).getModifiedElementId());
                    currentExtension.setAttributeValue(PROPERTY, ((Modify) op).getProperty());
                    currentExtension.setAttributeValue(VALUE, ((Modify) op).getValue());
                }
                else if (op instanceof Rename) {
                    currentExtension.setAttributeValue(ELEMENT_ID, ((Rename) op).getElementId());
                    currentExtension.setAttributeValue(NEW_NAME, ((Rename) op).getNewName());
                }
                else if (op instanceof DeleteNode) {
                    currentExtension.setAttributeValue(NODE_ID, ((DeleteNode) op).getNodeId());
                }
                else if (op instanceof DeleteFragment) {
                    currentExtension.setAttributeValue(STARTING_NODE_ID, ((DeleteFragment) op).getStartingNodeId());
                    currentExtension.setAttributeValue(ENDING_NODE_ID, ((DeleteFragment) op).getEndingNodeId());
                }
                else if (op instanceof MoveNode) {
                    currentExtension.setAttributeValue(NODE_ID, ((MoveNode) op).getNodeId());
                    currentExtension.setAttributeValue(NEW_POSITION_AFTER_OF_ID, ((MoveNode) op).getNewPositionAfterOfId());
                    currentExtension.setAttributeValue(NEW_POSITION_BEFORE_OF_ID, ((MoveNode) op).getNewPositionBeforeOfId());
                }
                else if (op instanceof MoveFragment) {
                    currentExtension.setAttributeValue(STARTING_NODE_ID, ((MoveFragment) op).getStartingNodeId());
                    currentExtension.setAttributeValue(ENDING_NODE_ID, ((MoveFragment) op).getEndingNodeId());
                    currentExtension.setAttributeValue(NEW_POSITION_AFTER_OF_ID, ((MoveFragment) op).getNewPositionAfterOfId());
                    currentExtension.setAttributeValue(NEW_POSITION_BEFORE_OF_ID, ((MoveFragment) op).getNewPositionBeforeOfId());
                }
                else {
                    currentExtension.setAttributeValue(STARTING_NODE_ID, ((Parallelize) op).getStartingNodeId());
                    currentExtension.setAttributeValue(ENDING_NODE_ID, ((Parallelize) op).getEndingNodeId());
                }
            }

            // Operations that need to save parts of other models
            else {
                SubProcess subProcess = modelInstance.newInstance(SubProcess.class);
                subProcess.setId(op.getName() + "_" + op.getExecutionOrder());
                subProcess.setName(op.getName() + " " + op.getExecutionOrder());
                process.addChildElement(subProcess);

                ModelElementInstance subProcessExt = subProcess
                                                        .getExtensionElements()
                                                        .addExtensionElement("http://www.processhub.net", op.getName());

                subProcessExt.setAttributeValue(ORDER, Integer.toString(op.getExecutionOrder()));

                if (op instanceof Contribute) {
                    BpmnElementCreator.add(subProcess, ((Contribute) op).getNewElement());
                    if (op instanceof ContributeCustomParent) {
                        subProcessExt.setAttributeValue(PARENT_ELEMENT_ID, ((ContributeCustomParent) op).getParentElementId());
                    }
                }

                else if (op instanceof ReplaceNodeWithNode) {
                    subProcessExt.setAttributeValue(REPLACED_NODE_ID, ((ReplaceNodeWithNode) op).getReplacedNodeId());
                    BpmnElementCreator.add(subProcess, ((ReplaceNodeWithNode) op).getReplacingNode());
                }

                // TODO: add fragment
                else if (op instanceof ReplaceNodeWithFragment) {
                    subProcessExt.setAttributeValue(REPLACED_NODE_ID, ((ReplaceNodeWithFragment) op).getReplacedNodeId());
                }

                else if (op instanceof ReplaceFragmentWithNode) {
                    subProcessExt.setAttributeValue(STARTING_NODE_ID, ((ReplaceFragmentWithNode) op).getStartingNodeId());
                    subProcessExt.setAttributeValue(ENDING_NODE_ID, ((ReplaceFragmentWithNode) op).getEndingNodeId());
                    BpmnElementCreator.add(subProcess, ((ReplaceFragmentWithNode) op).getReplacingNode());
                }

                // TODO: add fragment
                else if (op instanceof ReplaceFragmentWithFragment) {
                    subProcessExt.setAttributeValue(STARTING_NODE_ID, ((ReplaceFragmentWithFragment) op).getStartingNodeId());
                    subProcessExt.setAttributeValue(ENDING_NODE_ID, ((ReplaceFragmentWithFragment) op).getEndingNodeId());
                }

                // TODO: add fragment
                else if (op instanceof Split) {
                    subProcessExt.setAttributeValue(TASK_ID, ((Split) op).getTaskId());
                }

                else if (op instanceof InsertNode) {
                    subProcessExt.setAttributeValue(AFTER_OF_ID, ((InsertNode) op).getAfterOfId());
                    subProcessExt.setAttributeValue(BEFORE_OF_ID, ((InsertNode) op).getBeforeOfId());
                    BpmnElementCreator.add(subProcess, ((InsertNode) op).getFlowNodeToInsert());
                }

                // TODO: add fragment
                else if (op instanceof InsertFragment) {
                    subProcessExt.setAttributeValue(AFTER_OF_ID, ((InsertFragment) op).getAfterOfId());
                    subProcessExt.setAttributeValue(BEFORE_OF_ID, ((InsertFragment) op).getBeforeOfId());
                }

                else if (op instanceof ConditionalInsertNode) {
                    subProcessExt.setAttributeValue(AFTER_OF_ID, ((ConditionalInsertNode) op).getAfterOfId());
                    subProcessExt.setAttributeValue(BEFORE_OF_ID, ((ConditionalInsertNode) op).getBeforeOfId());
                    subProcessExt.setAttributeValue(CONDITION, ((ConditionalInsertNode) op).getCondition());
                    subProcessExt.setAttributeValue(IN_LOOP, Boolean.toString(((ConditionalInsertNode) op).isInLoop()));
                    BpmnElementCreator.add(subProcess, ((ConditionalInsertNode) op).getFlowNodeToInsert());
                }

                // TODO: add fragment
                else if (op instanceof ConditionalInsertFragment) {
                    subProcessExt.setAttributeValue(AFTER_OF_ID, ((ConditionalInsertFragment) op).getAfterOfId());
                    subProcessExt.setAttributeValue(BEFORE_OF_ID, ((ConditionalInsertFragment) op).getBeforeOfId());
                    subProcessExt.setAttributeValue(CONDITION, ((ConditionalInsertFragment) op).getCondition());
                    subProcessExt.setAttributeValue(IN_LOOP, Boolean.toString(((ConditionalInsertFragment) op).isInLoop()));

                }
            }
        }


        return modelInstance;
    }


    public static BpmntModelInstance readModelFromFile(File file) {
        return INSTANCE.doReadModelFromFile(file);
    }

    public static BpmntModelInstance readModelFromStream(InputStream stream) {
        return INSTANCE.doReadModelFromInputStream(stream);
    }

    public static void writeModelToFile(File file, BpmntModelInstance modelInstance) {
        INSTANCE.doWriteModelToFile(file, modelInstance);
    }

    public static void writeModelToStream(OutputStream stream, BpmntModelInstance modelInstance) {
        INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
    }

    public static String convertToString(BpmntModelInstance modelInstance) {
        return INSTANCE.doConvertToString(modelInstance);
    }


    public static void validateModel(BpmntModelInstance modelInstance) {
        INSTANCE.doValidateModel(modelInstance);
    }

    public static BpmntModelInstance createEmptyModel() {
        return INSTANCE.doCreateEmptyModel();
    }

    public static ProcessBuilder createProcess() {
        BpmntModelInstance modelInstance = INSTANCE.doCreateEmptyModel();
        Definitions definitions = (Definitions)modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
        definitions.getDomElement().registerNamespace("camunda", "http://activiti.org/bpmn");
        modelInstance.setDefinitions(definitions);
        Process process = (Process)modelInstance.newInstance(Process.class);
        String processId = ModelUtil.getUniqueIdentifier(process.getElementType());
        process.setId(processId);
        definitions.addChildElement(process);
        return process.builder();
    }

    public static ProcessBuilder createProcess(String processId) {
        return (ProcessBuilder)createProcess().id(processId);
    }

    public static ProcessBuilder createExecutableProcess() {
        return (ProcessBuilder)createProcess().executable();
    }

    public static ProcessBuilder createExecutableProcess(String processId) {
        return (ProcessBuilder)createProcess(processId).executable();
    }

    protected Bpmnt() {
        this.doRegisterTypes(this.bpmntModelBuilder);
        this.BpmntModel = this.bpmntModelBuilder.build();
    }

    protected BpmntModelInstance doReadModelFromFile(File file) {
        FileInputStream is = null;

        BpmntModelInstance e;
        try {
            is = new FileInputStream(file);
            e = this.doReadModelFromInputStream(is);
        } catch (FileNotFoundException var7) {
            throw new BpmnModelException("Cannot read model from file " + file + ": file does not exist.");
        } finally {
            IoUtil.closeSilently(is);
        }

        return e;
    }

    protected BpmntModelInstance doReadModelFromInputStream(InputStream is) {
        return this.bpmntParser.parseModelFromStream(is);
    }

    protected void doWriteModelToFile(File file, BpmntModelInstance modelInstance) {
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(file);
            this.doWriteModelToOutputStream(os, modelInstance);
        } catch (FileNotFoundException var8) {
            throw new BpmnModelException("Cannot write model to file " + file + ": file does not exist.");
        } finally {
            IoUtil.closeSilently(os);
        }

    }

    protected void doWriteModelToOutputStream(OutputStream os, BpmntModelInstance modelInstance) {
        this.doValidateModel(modelInstance);
        IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
    }

    protected String doConvertToString(BpmntModelInstance modelInstance) {
        this.doValidateModel(modelInstance);
        return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
    }

    protected void doValidateModel(BpmntModelInstance modelInstance) {
        this.bpmntParser.validateModel(modelInstance.getDocument());
    }

    protected BpmntModelInstance doCreateEmptyModel() {
        return this.bpmntParser.getEmptyModel();
    }

    protected void doRegisterTypes(ModelBuilder bpmnModelBuilder) {
        ActivationConditionImpl.registerType(bpmnModelBuilder);
        ActivityImpl.registerType(bpmnModelBuilder);
        ArtifactImpl.registerType(bpmnModelBuilder);
        AssignmentImpl.registerType(bpmnModelBuilder);
        AssociationImpl.registerType(bpmnModelBuilder);
        AuditingImpl.registerType(bpmnModelBuilder);
        BaseElementImpl.registerType(bpmnModelBuilder);
        BoundaryEventImpl.registerType(bpmnModelBuilder);
        BusinessRuleTaskImpl.registerType(bpmnModelBuilder);
        CallableElementImpl.registerType(bpmnModelBuilder);
        CallActivityImpl.registerType(bpmnModelBuilder);
        CallConversationImpl.registerType(bpmnModelBuilder);
        CancelEventDefinitionImpl.registerType(bpmnModelBuilder);
        CatchEventImpl.registerType(bpmnModelBuilder);
        CategoryValueImpl.registerType(bpmnModelBuilder);
        CategoryValueRef.registerType(bpmnModelBuilder);
        ChildLaneSet.registerType(bpmnModelBuilder);
        CollaborationImpl.registerType(bpmnModelBuilder);
        CompensateEventDefinitionImpl.registerType(bpmnModelBuilder);
        ConditionImpl.registerType(bpmnModelBuilder);
        ConditionalEventDefinitionImpl.registerType(bpmnModelBuilder);
        CompletionConditionImpl.registerType(bpmnModelBuilder);
        ComplexBehaviorDefinitionImpl.registerType(bpmnModelBuilder);
        ComplexGatewayImpl.registerType(bpmnModelBuilder);
        ConditionExpressionImpl.registerType(bpmnModelBuilder);
        ConversationAssociationImpl.registerType(bpmnModelBuilder);
        ConversationImpl.registerType(bpmnModelBuilder);
        ConversationLinkImpl.registerType(bpmnModelBuilder);
        ConversationNodeImpl.registerType(bpmnModelBuilder);
        CorrelationKeyImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyBindingImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyRef.registerType(bpmnModelBuilder);
        CorrelationPropertyRetrievalExpressionImpl.registerType(bpmnModelBuilder);
        CorrelationSubscriptionImpl.registerType(bpmnModelBuilder);
        DataAssociationImpl.registerType(bpmnModelBuilder);
        DataInputAssociationImpl.registerType(bpmnModelBuilder);
        DataInputImpl.registerType(bpmnModelBuilder);
        DataInputRefs.registerType(bpmnModelBuilder);
        DataOutputAssociationImpl.registerType(bpmnModelBuilder);
        DataOutputImpl.registerType(bpmnModelBuilder);
        DataOutputRefs.registerType(bpmnModelBuilder);
        DataPath.registerType(bpmnModelBuilder);
        DataStateImpl.registerType(bpmnModelBuilder);
        DataObjectImpl.registerType(bpmnModelBuilder);
        DataObjectReferenceImpl.registerType(bpmnModelBuilder);
        DefinitionsImpl.registerType(bpmnModelBuilder);
        DocumentationImpl.registerType(bpmnModelBuilder);
        EndEventImpl.registerType(bpmnModelBuilder);
        EndPointImpl.registerType(bpmnModelBuilder);
        EndPointRef.registerType(bpmnModelBuilder);
        ErrorEventDefinitionImpl.registerType(bpmnModelBuilder);
        ErrorImpl.registerType(bpmnModelBuilder);
        ErrorRef.registerType(bpmnModelBuilder);
        EscalationImpl.registerType(bpmnModelBuilder);
        EscalationEventDefinitionImpl.registerType(bpmnModelBuilder);
        EventBasedGatewayImpl.registerType(bpmnModelBuilder);
        EventDefinitionImpl.registerType(bpmnModelBuilder);
        EventDefinitionRef.registerType(bpmnModelBuilder);
        EventImpl.registerType(bpmnModelBuilder);
        ExclusiveGatewayImpl.registerType(bpmnModelBuilder);
        ExpressionImpl.registerType(bpmnModelBuilder);
        ExtensionElementsImpl.registerType(bpmnModelBuilder);
        ExtensionImpl.registerType(bpmnModelBuilder);
        FlowElementImpl.registerType(bpmnModelBuilder);
        FlowNodeImpl.registerType(bpmnModelBuilder);
        FlowNodeRef.registerType(bpmnModelBuilder);
        FormalExpressionImpl.registerType(bpmnModelBuilder);
        From.registerType(bpmnModelBuilder);
        GatewayImpl.registerType(bpmnModelBuilder);
        GlobalConversationImpl.registerType(bpmnModelBuilder);
        HumanPerformerImpl.registerType(bpmnModelBuilder);
        ImportImpl.registerType(bpmnModelBuilder);
        InclusiveGatewayImpl.registerType(bpmnModelBuilder);
        Incoming.registerType(bpmnModelBuilder);
        InMessageRef.registerType(bpmnModelBuilder);
        InnerParticipantRef.registerType(bpmnModelBuilder);
        InputDataItemImpl.registerType(bpmnModelBuilder);
        InputSetImpl.registerType(bpmnModelBuilder);
        InputSetRefs.registerType(bpmnModelBuilder);
        InteractionNodeImpl.registerType(bpmnModelBuilder);
        InterfaceImpl.registerType(bpmnModelBuilder);
        InterfaceRef.registerType(bpmnModelBuilder);
        IntermediateCatchEventImpl.registerType(bpmnModelBuilder);
        IntermediateThrowEventImpl.registerType(bpmnModelBuilder);
        IoBindingImpl.registerType(bpmnModelBuilder);
        IoSpecificationImpl.registerType(bpmnModelBuilder);
        ItemAwareElementImpl.registerType(bpmnModelBuilder);
        ItemDefinitionImpl.registerType(bpmnModelBuilder);
        LaneImpl.registerType(bpmnModelBuilder);
        LaneSetImpl.registerType(bpmnModelBuilder);
        LinkEventDefinitionImpl.registerType(bpmnModelBuilder);
        LoopCardinalityImpl.registerType(bpmnModelBuilder);
        LoopCharacteristicsImpl.registerType(bpmnModelBuilder);
        LoopDataInputRef.registerType(bpmnModelBuilder);
        LoopDataOutputRef.registerType(bpmnModelBuilder);
        ManualTaskImpl.registerType(bpmnModelBuilder);
        MessageEventDefinitionImpl.registerType(bpmnModelBuilder);
        MessageFlowAssociationImpl.registerType(bpmnModelBuilder);
        MessageFlowImpl.registerType(bpmnModelBuilder);
        MessageFlowRef.registerType(bpmnModelBuilder);
        MessageImpl.registerType(bpmnModelBuilder);
        MessagePath.registerType(bpmnModelBuilder);
        ModelElementInstanceImpl.registerType(bpmnModelBuilder);
        MonitoringImpl.registerType(bpmnModelBuilder);
        MultiInstanceLoopCharacteristicsImpl.registerType(bpmnModelBuilder);
        OperationImpl.registerType(bpmnModelBuilder);
        OperationRef.registerType(bpmnModelBuilder);
        OptionalInputRefs.registerType(bpmnModelBuilder);
        OptionalOutputRefs.registerType(bpmnModelBuilder);
        OuterParticipantRef.registerType(bpmnModelBuilder);
        OutMessageRef.registerType(bpmnModelBuilder);
        Outgoing.registerType(bpmnModelBuilder);
        OutputDataItemImpl.registerType(bpmnModelBuilder);
        OutputSetImpl.registerType(bpmnModelBuilder);
        OutputSetRefs.registerType(bpmnModelBuilder);
        ParallelGatewayImpl.registerType(bpmnModelBuilder);
        ParticipantAssociationImpl.registerType(bpmnModelBuilder);
        ParticipantImpl.registerType(bpmnModelBuilder);
        ParticipantMultiplicityImpl.registerType(bpmnModelBuilder);
        ParticipantRef.registerType(bpmnModelBuilder);
        PartitionElement.registerType(bpmnModelBuilder);
        PerformerImpl.registerType(bpmnModelBuilder);
        PotentialOwnerImpl.registerType(bpmnModelBuilder);
        ProcessImpl.registerType(bpmnModelBuilder);
        PropertyImpl.registerType(bpmnModelBuilder);
        ReceiveTaskImpl.registerType(bpmnModelBuilder);
        RelationshipImpl.registerType(bpmnModelBuilder);
        RenderingImpl.registerType(bpmnModelBuilder);
        ResourceAssignmentExpressionImpl.registerType(bpmnModelBuilder);
        ResourceImpl.registerType(bpmnModelBuilder);
        ResourceParameterBindingImpl.registerType(bpmnModelBuilder);
        ResourceParameterImpl.registerType(bpmnModelBuilder);
        ResourceRef.registerType(bpmnModelBuilder);
        ResourceRoleImpl.registerType(bpmnModelBuilder);
        RootElementImpl.registerType(bpmnModelBuilder);
        ScriptImpl.registerType(bpmnModelBuilder);
        ScriptTaskImpl.registerType(bpmnModelBuilder);
        SendTaskImpl.registerType(bpmnModelBuilder);
        SequenceFlowImpl.registerType(bpmnModelBuilder);
        ServiceTaskImpl.registerType(bpmnModelBuilder);
        SignalEventDefinitionImpl.registerType(bpmnModelBuilder);
        SignalImpl.registerType(bpmnModelBuilder);
        Source.registerType(bpmnModelBuilder);
        SourceRef.registerType(bpmnModelBuilder);
        StartEventImpl.registerType(bpmnModelBuilder);
        SubConversationImpl.registerType(bpmnModelBuilder);
        SubProcessImpl.registerType(bpmnModelBuilder);
        SupportedInterfaceRef.registerType(bpmnModelBuilder);
        Supports.registerType(bpmnModelBuilder);
        Target.registerType(bpmnModelBuilder);
        TargetRef.registerType(bpmnModelBuilder);
        TaskImpl.registerType(bpmnModelBuilder);
        TerminateEventDefinitionImpl.registerType(bpmnModelBuilder);
        TextImpl.registerType(bpmnModelBuilder);
        TextAnnotationImpl.registerType(bpmnModelBuilder);
        ThrowEventImpl.registerType(bpmnModelBuilder);
        TimeCycleImpl.registerType(bpmnModelBuilder);
        TimeDateImpl.registerType(bpmnModelBuilder);
        TimeDurationImpl.registerType(bpmnModelBuilder);
        TimerEventDefinitionImpl.registerType(bpmnModelBuilder);
        To.registerType(bpmnModelBuilder);
        Transformation.registerType(bpmnModelBuilder);
        UserTaskImpl.registerType(bpmnModelBuilder);
        WhileExecutingInputRefs.registerType(bpmnModelBuilder);
        WhileExecutingOutputRefs.registerType(bpmnModelBuilder);
        FontImpl.registerType(bpmnModelBuilder);
        PointImpl.registerType(bpmnModelBuilder);
        BoundsImpl.registerType(bpmnModelBuilder);
        DiagramImpl.registerType(bpmnModelBuilder);
        DiagramElementImpl.registerType(bpmnModelBuilder);
        EdgeImpl.registerType(bpmnModelBuilder);
        org.camunda.bpm.model.bpmn.impl.instance.di.ExtensionImpl.registerType(bpmnModelBuilder);
        LabelImpl.registerType(bpmnModelBuilder);
        LabeledEdgeImpl.registerType(bpmnModelBuilder);
        LabeledShapeImpl.registerType(bpmnModelBuilder);
        NodeImpl.registerType(bpmnModelBuilder);
        PlaneImpl.registerType(bpmnModelBuilder);
        ShapeImpl.registerType(bpmnModelBuilder);
        StyleImpl.registerType(bpmnModelBuilder);
        WaypointImpl.registerType(bpmnModelBuilder);
        BpmnDiagramImpl.registerType(bpmnModelBuilder);
        BpmnEdgeImpl.registerType(bpmnModelBuilder);
        BpmnLabelImpl.registerType(bpmnModelBuilder);
        BpmnLabelStyleImpl.registerType(bpmnModelBuilder);
        BpmnPlaneImpl.registerType(bpmnModelBuilder);
        BpmnShapeImpl.registerType(bpmnModelBuilder);
        CamundaConnectorImpl.registerType(bpmnModelBuilder);
        CamundaConnectorIdImpl.registerType(bpmnModelBuilder);
        CamundaConstraintImpl.registerType(bpmnModelBuilder);
        CamundaEntryImpl.registerType(bpmnModelBuilder);
        CamundaExecutionListenerImpl.registerType(bpmnModelBuilder);
        CamundaExpressionImpl.registerType(bpmnModelBuilder);
        CamundaFailedJobRetryTimeCycleImpl.registerType(bpmnModelBuilder);
        CamundaFieldImpl.registerType(bpmnModelBuilder);
        CamundaFormDataImpl.registerType(bpmnModelBuilder);
        CamundaFormFieldImpl.registerType(bpmnModelBuilder);
        CamundaFormPropertyImpl.registerType(bpmnModelBuilder);
        CamundaInImpl.registerType(bpmnModelBuilder);
        CamundaInputOutputImpl.registerType(bpmnModelBuilder);
        CamundaInputParameterImpl.registerType(bpmnModelBuilder);
        CamundaListImpl.registerType(bpmnModelBuilder);
        CamundaMapImpl.registerType(bpmnModelBuilder);
        CamundaOutputParameterImpl.registerType(bpmnModelBuilder);
        CamundaOutImpl.registerType(bpmnModelBuilder);
        CamundaPotentialStarterImpl.registerType(bpmnModelBuilder);
        CamundaPropertiesImpl.registerType(bpmnModelBuilder);
        CamundaPropertyImpl.registerType(bpmnModelBuilder);
        CamundaScriptImpl.registerType(bpmnModelBuilder);
        CamundaStringImpl.registerType(bpmnModelBuilder);
        CamundaTaskListenerImpl.registerType(bpmnModelBuilder);
        CamundaValidationImpl.registerType(bpmnModelBuilder);
        CamundaValueImpl.registerType(bpmnModelBuilder);
    }

    public Model getBpmntModel() {
        return this.BpmntModel;
    }

    public ModelBuilder getBpmntModelBuilder() {
        return this.bpmntModelBuilder;
    }

    public void setBpmntModel(Model bpmntModel) {
        this.BpmntModel = bpmntModel;
    }
}
