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
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.Model;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.camunda.bpm.model.xml.impl.util.IoUtil;
import org.camunda.bpm.model.xml.impl.util.ModelUtil;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.*;
import org.prisma.processhub.bpmn.manipulation.bpmnt.operation.constant.BpmntExtensionAttributes;
import org.prisma.processhub.bpmn.manipulation.util.BpmnElementSearcher;
import org.prisma.processhub.bpmn.manipulation.util.BpmnHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Bpmnt {
    public static Bpmnt INSTANCE = new Bpmnt();
    private BpmntParser bpmntParser = new BpmntParser();
    private final ModelBuilder bpmntModelBuilder = ModelBuilder.createInstance("Tailorable BPMN Model");
    private Model BpmntModel;

//    public static BpmnModelInstance executeBpmnt (BpmnModelInstance modelInstance, List<BpmntOperation> bpmntList) {
//
//        Extend extend = null;
//
//        for (BpmntOperation op: bpmntList) {
//            if (op instanceof Extend) {
//                extend = (Extend) op;
//                break;
//            }
//        }
//
//        if (extend == null) {
//            return null;
//        }
//
//
//        return modelInstance;
//    }

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

        BpmnModelInstance modelInstance =
                Bpmn
                        .createProcess(extend.getNewProcessId())
                        .name(BpmntExtensionAttributes.MODIFIED_PROCESS_ID_PREFIX + extend.getBaseProcessId())
                        .done();

        Process process = BpmnElementSearcher.findFirstProcess(modelInstance);

        // Add extensions
        for (BpmntOperation op: bpmntList) {
            op.generateExtensionElement(process);
        }

        return modelInstance;
    }

    public static List<BpmntOperation> convertBpmntFromModelToList (BpmnModelInstance bpmntModel) {
        Process process = BpmnElementSearcher.findFirstProcess(bpmntModel);

        //Collection<ModelElementInstance> extentionElements = process.getExtensionElements().getElements();
        Collection<ExtensionElements> extensionElementsCollection = bpmntModel.getModelElementsByType(ExtensionElements.class);
        //Collection<ModelElementInstance> extentionElements = bpmntModel.getModelElementsByType(ExtensionElements.class);
        List<BpmntOperation> operations = new ArrayList<BpmntOperation>();

        for (ExtensionElements extensionElements: extensionElementsCollection) {
            ModelElementInstance parent = extensionElements.getParentElement();

            if (parent instanceof Process) {
                Collection<ModelElementInstance> extensions = ((Process) parent).getExtensionElements().getElements();

                for (ModelElementInstance extension: extensions) {
                    String operationName = extension.getElementType().getTypeName();

                    if (operationName.equals(Extend.class.getSimpleName())) {
                        Extend extend = new Extend(extension.getAttributeValue(BpmntExtensionAttributes.BASE_PROCESS_ID));
                        extend.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(extend);
                    }
                    else if (operationName.equals(Suppress.class.getSimpleName())) {
                        Suppress suppress = new Suppress(extension.getAttributeValue(BpmntExtensionAttributes.SUPPRESSED_ELEMENT_ID));
                        suppress.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(suppress);
                    }
                    else if (operationName.equals(Modify.class.getSimpleName())) {
                        Modify modify = new Modify(
                                extension.getAttributeValue(BpmntExtensionAttributes.MODIFIED_ELEMENT_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.PROPERTY),
                                extension.getAttributeValue(BpmntExtensionAttributes.VALUE)
                        );
                        modify.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(modify);
                    }
                    else if (operationName.equals(Rename.class.getSimpleName())) {
                        Rename rename = new Rename(
                                extension.getAttributeValue(BpmntExtensionAttributes.ELEMENT_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.NEW_NAME)
                        );
                        rename.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(rename);
                    }
                    else if (operationName.equals(DeleteNode.class.getSimpleName())) {
                        DeleteNode deleteNode = new DeleteNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.NODE_ID)
                        );
                        deleteNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(deleteNode);
                    }
                    else if (operationName.equals(DeleteFragment.class.getSimpleName())) {
                        DeleteFragment deleteFragment = new DeleteFragment(
                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID)
                        );
                        deleteFragment.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(deleteFragment);
                    }
                    else if (operationName.equals(MoveNode.class.getSimpleName())) {
                        MoveNode moveNode = new MoveNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID)
                        );
                        moveNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(moveNode);
                    }
                    else if (operationName.equals(MoveFragment.class.getSimpleName())) {
                        MoveFragment moveFragment = new MoveFragment(
                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.NEW_POSITION_AFTER_OF_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.NEW_POSITION_BEFORE_OF_ID)
                        );
                        moveFragment.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(moveFragment);
                    }
                    else if (operationName.equals(Parallelize.class.getSimpleName())) {
                        Parallelize parallelize = new Parallelize(
                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID)
                        );
                        parallelize.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(parallelize);
                    }
                }
            }
            else if (parent instanceof SubProcess) {
                if (extensionElements.getElementsQuery().count() == 1) {
                    ModelElementInstance extension = extensionElements.getElementsQuery().singleResult();

                    if (extension.getElementType().getTypeName().equals(Contribute.class.getSimpleName())) {
                        Contribute contribute = new Contribute(((SubProcess) parent).getFlowElements().iterator().next());
                        contribute.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(contribute);
                    }
                    else if (extension.getElementType().getTypeName().equals(ContributeToParent.class.getSimpleName())) {
                        ContributeToParent contributeToParent = new ContributeToParent(
                                extension.getAttributeValue(BpmntExtensionAttributes.PARENT_ELEMENT_ID),
                                ((SubProcess) parent).getFlowElements().iterator().next()
                        );
                        contributeToParent.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(contributeToParent);
                    }
                    else if (extension.getElementType().getTypeName().equals(InsertNode.class.getSimpleName())) {
                        InsertNode insertNode = new InsertNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID),
                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
                        );
                        insertNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(insertNode);
                    }
//                    else if (extension.getElementType().getTypeName().equals(InsertFragment.class.getSimpleName())) {
//                        InsertFragment insertFragment = new InsertFragment(
//                                extension.getAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID),
//                                extension.getAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID),
//                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
//                        );
//                        insertNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
//                        operations.add(insertNode);
//                    }
                    else if (extension.getElementType().getTypeName().equals(ConditionalInsertNode.class.getSimpleName())) {
                        ConditionalInsertNode conditionalInsertNode = new ConditionalInsertNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID),
                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next(),
                                extension.getAttributeValue(BpmntExtensionAttributes.CONDITION),
                                Boolean.parseBoolean(extension.getAttributeValue(BpmntExtensionAttributes.IN_LOOP))
                        );
                        conditionalInsertNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(conditionalInsertNode);
                    }
//                    else if (extension.getElementType().getTypeName().equals(ConditionalInsertNode.class.getSimpleName())) {
//                        ConditionalInsertNode conditionalInsertNode = new ConditionalInsertNode(
//                                extension.getAttributeValue(BpmntExtensionAttributes.AFTER_OF_ID),
//                                extension.getAttributeValue(BpmntExtensionAttributes.BEFORE_OF_ID),
//                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next(),
//                                extension.getAttributeValue(BpmntExtensionAttributes.CONDITION),
//                                Boolean.parseBoolean(extension.getAttributeValue(BpmntExtensionAttributes.IN_LOOP))
//                        );
//                        conditionalInsertNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
//                        operations.add(conditionalInsertNode);
//                    }
                    else if (extension.getElementType().getTypeName().equals(ReplaceNodeWithNode.class.getSimpleName())) {
                        ReplaceNodeWithNode replaceNodeWithNode = new ReplaceNodeWithNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID),
                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
                        );
                        replaceNodeWithNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(replaceNodeWithNode);
                    }
//                    else if (extension.getElementType().getTypeName().equals(ReplaceNodeWithNode.class.getSimpleName())) {
//                        ReplaceNodeWithNode replaceNodeWithNode = new ReplaceNodeWithNode(
//                                extension.getAttributeValue(BpmntExtensionAttributes.REPLACED_NODE_ID),
//                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
//                        );
//                        replaceNodeWithNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
//                        operations.add(replaceNodeWithNode);
//                    }
                    else if (extension.getElementType().getTypeName().equals(ReplaceFragmentWithNode.class.getSimpleName())) {
                        ReplaceFragmentWithNode replaceFragmentWithNode = new ReplaceFragmentWithNode(
                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID),
                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
                        );
                        replaceFragmentWithNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
                        operations.add(replaceFragmentWithNode);
                    }
//                    else if (extension.getElementType().getTypeName().equals(ReplaceFragmentWithNode.class.getSimpleName())) {
//                        ReplaceFragmentWithNode replaceFragmentWithNode = new ReplaceFragmentWithNode(
//                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
//                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID),
//                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
//                        );
//                        replaceFragmentWithNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
//                        operations.add(replaceFragmentWithNode);
//                    }

                    // Split
//                    else if (extension.getElementType().getTypeName().equals(ReplaceFragmentWithNode.class.getSimpleName())) {
//                        ReplaceFragmentWithNode replaceFragmentWithNode = new ReplaceFragmentWithNode(
//                                extension.getAttributeValue(BpmntExtensionAttributes.STARTING_NODE_ID),
//                                extension.getAttributeValue(BpmntExtensionAttributes.ENDING_NODE_ID),
//                                (FlowNode) ((SubProcess) parent).getFlowElements().iterator().next()
//                        );
//                        replaceFragmentWithNode.setExecutionOrder(Integer.parseInt(extension.getAttributeValue(BpmntExtensionAttributes.ORDER)));
//                        operations.add(replaceFragmentWithNode);
//
                }
            }


//            else if (operationName.equals("Extend")) {
//
//            }
        }

        return operations;
    }

    public static BpmntModelInstance readModelAndBpmntFromStrings(String modelXml, String bpmntXml) {
        BpmntModelInstance model = readModelFromStream(new ByteArrayInputStream(modelXml.getBytes(StandardCharsets.UTF_8)));
        BpmnModelInstance bpmntModel = readModelFromStream(new ByteArrayInputStream(bpmntXml.getBytes(StandardCharsets.UTF_8)));

        model.setBpmntLog(convertBpmntFromModelToList(bpmntModel));

        return model;
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