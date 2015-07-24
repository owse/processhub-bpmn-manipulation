package org.prisma.processhub.bpmn.manipulation.bpmnt;

import org.camunda.bpm.model.xml.ModelValidationException;
import org.camunda.bpm.model.xml.impl.ModelImpl;
import org.camunda.bpm.model.xml.impl.parser.AbstractModelParser;
import org.camunda.bpm.model.xml.impl.util.ReflectUtil;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.prisma.processhub.bpmn.manipulation.impl.tailoring.BpmntModelInstanceImpl;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;

import java.io.InputStream;
import java.net.URL;


public class BpmntParser extends AbstractModelParser {
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    public BpmntParser() {
        this.schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        URL bpmnSchema = ReflectUtil.getResource("BPMN20.xsd", BpmntParser.class.getClassLoader());

        try {
            this.schema = this.schemaFactory.newSchema(bpmnSchema);
        } catch (SAXException var3) {
            throw new ModelValidationException("Unable to parse schema:" + bpmnSchema);
        }
    }

    protected void configureFactory(DocumentBuilderFactory dbf) {
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", ReflectUtil.getResource("BPMN20.xsd", BpmntParser.class.getClassLoader()).toString());
        super.configureFactory(dbf);
    }

    protected BpmntModelInstanceImpl createModelInstance(DomDocument document) {
        return new BpmntModelInstanceImpl((ModelImpl)Bpmnt.INSTANCE.getBpmntModel(), Bpmnt.INSTANCE.getBpmntModelBuilder(), document);
    }

    public BpmntModelInstanceImpl parseModelFromStream(InputStream inputStream) {
        return (BpmntModelInstanceImpl)super.parseModelFromStream(inputStream);
    }

    public BpmntModelInstanceImpl getEmptyModel() {
        return (BpmntModelInstanceImpl)super.getEmptyModel();
    }
}
