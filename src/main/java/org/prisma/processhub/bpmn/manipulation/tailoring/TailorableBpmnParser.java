package org.prisma.processhub.bpmn.manipulation.tailoring;

import org.camunda.bpm.model.xml.ModelValidationException;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.impl.parser.AbstractModelParser;
import org.camunda.bpm.model.xml.impl.util.ReflectUtil;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.impl.tailoring.TailorableBpmnModelInstanceImpl;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;

import java.io.InputStream;
import java.net.URL;


public class TailorableBpmnParser extends AbstractModelParser {
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    public TailorableBpmnParser() {
        this.schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        URL bpmnSchema = ReflectUtil.getResource("BPMN20.xsd", TailorableBpmnParser.class.getClassLoader());

        try {
            this.schema = this.schemaFactory.newSchema(bpmnSchema);
        } catch (SAXException var3) {
            throw new ModelValidationException("Unable to parse schema:" + bpmnSchema);
        }
    }

    protected void configureFactory(DocumentBuilderFactory dbf) {
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", ReflectUtil.getResource("BPMN20.xsd", TailorableBpmnParser.class.getClassLoader()).toString());
        super.configureFactory(dbf);
    }

    protected TailorableBpmnModelInstanceImpl createModelInstance(DomDocument document) {
        return new TailorableBpmnModelInstanceImpl((ModelImpl) TailorableBpmn.INSTANCE.getTailorableBpmnModel(), TailorableBpmn.INSTANCE.getTailorableBpmnModelBuilder(), document);
    }

    public TailorableBpmnModelInstanceImpl parseModelFromStream(InputStream inputStream) {
        return (TailorableBpmnModelInstanceImpl)super.parseModelFromStream(inputStream);
    }

    public TailorableBpmnModelInstanceImpl getEmptyModel() {
        return (TailorableBpmnModelInstanceImpl)super.getEmptyModel();
    }
}
