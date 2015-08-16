package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.bpmn.BpmnModelException;
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
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.Model;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.camunda.bpm.model.xml.impl.util.IoUtil;
import org.camunda.bpm.model.xml.impl.util.ModelUtil;

import java.io.*;


public class TailorableBpmn {
    public static TailorableBpmn INSTANCE = new TailorableBpmn();
    private TailorableBpmnParser tailorableBpmnParser = new TailorableBpmnParser();
    private final ModelBuilder tailorableBpmnModelBuilder = ModelBuilder.createInstance("Tailorable BPMN Model");
    private Model TailorableBpmnModel;

    public static TailorableBpmnModelInstance readModelFromFile(File file) {
        return INSTANCE.doReadModelFromFile(file);
    }

    public static TailorableBpmnModelInstance readModelFromStream(InputStream stream) {
        return INSTANCE.doReadModelFromInputStream(stream);
    }

    public static void writeModelToFile(File file, TailorableBpmnModelInstance modelInstance) {
        INSTANCE.doWriteModelToFile(file, modelInstance);
    }

    public static void writeModelToStream(OutputStream stream, TailorableBpmnModelInstance modelInstance) {
        INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
    }

    public static String convertToString(TailorableBpmnModelInstance modelInstance) {
        return INSTANCE.doConvertToString(modelInstance);
    }


    public static void validateModel(TailorableBpmnModelInstance modelInstance) {
        INSTANCE.doValidateModel(modelInstance);
    }

    public static TailorableBpmnModelInstance createEmptyModel() {
        return INSTANCE.doCreateEmptyModel();
    }

    public static org.camunda.bpm.model.bpmn.builder.ProcessBuilder createProcess() {
        TailorableBpmnModelInstance modelInstance = INSTANCE.doCreateEmptyModel();
        Definitions definitions = (Definitions)modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
        definitions.getDomElement().registerNamespace("camunda", "http://activiti.org/bpmn");
        modelInstance.setDefinitions(definitions);
        org.camunda.bpm.model.bpmn.instance.Process process = (Process)modelInstance.newInstance(Process.class);
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

    protected TailorableBpmn() {
        this.doRegisterTypes(this.tailorableBpmnModelBuilder);
        this.TailorableBpmnModel = this.tailorableBpmnModelBuilder.build();
    }

    protected TailorableBpmnModelInstance doReadModelFromFile(File file) {
        FileInputStream is = null;

        TailorableBpmnModelInstance e;
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

    protected TailorableBpmnModelInstance doReadModelFromInputStream(InputStream is) {
        return this.tailorableBpmnParser.parseModelFromStream(is);
    }

    protected void doWriteModelToFile(File file, TailorableBpmnModelInstance modelInstance) {
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

    protected void doWriteModelToOutputStream(OutputStream os, TailorableBpmnModelInstance modelInstance) {
        this.doValidateModel(modelInstance);
        IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
    }

    protected String doConvertToString(TailorableBpmnModelInstance modelInstance) {
        this.doValidateModel(modelInstance);
        return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
    }

    protected void doValidateModel(TailorableBpmnModelInstance modelInstance) {
        this.tailorableBpmnParser.validateModel(modelInstance.getDocument());
    }

    protected TailorableBpmnModelInstance doCreateEmptyModel() {
        return this.tailorableBpmnParser.getEmptyModel();
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

    public Model getTailorableBpmnModel() {
        return this.TailorableBpmnModel;
    }

    public ModelBuilder getTailorableBpmnModelBuilder() {
        return this.tailorableBpmnModelBuilder;
    }

    public void setTailorableBpmnModel(Model tailorableBpmnModel) {
        this.TailorableBpmnModel = tailorableBpmnModel;
    }
}
