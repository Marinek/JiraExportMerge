package de.materna.jira.migration.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

@ShellComponent
@Log4j2
public class JiraXMLShell {

    private Document newDocument = null;

    @ShellMethod(value = "Generate XML export from JIRA instance xml to stack custom fields.")
    public void exportXML(String sourceXML, String targetXML)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Document sourceDoc = getEntitiesDocumentFromZip(sourceXML);
        //Document targetDoc = getEntitiesDocumentFromZip(targetXML);
        Node newRootNode = getNewXMLDocument(sourceDoc);

        final int maxCustomFieldID = 40000; //getMaxID("CustomField", "id", targetDoc);
        final int maxCustomFieldOption = 40000; //getMaxID("CustomFieldOption", "id", targetDoc);
        final int maxFieldConfigScheme = 40000;//getMaxID("FieldConfigScheme", "id", targetDoc);
        final int maxFieldConfiguration = 40000; //getMaxID("FieldConfiguration", "id", targetDoc);
        final int maxFieldConfigSchemeIssueType = 400000; //getMaxID("FieldConfigSchemeIssueType", "id", targetDoc);
        final int maxCustomFieldValue = 5000000; //getMaxID("FieldConfigSchemeIssueType", "id", targetDoc);

        Map<String, Node> fieldConfigSchemeMap = new HashMap<>();
        Map<String, Node> fieldConfigurationMap = new HashMap<>();
        Map<String, Node> fieldConfigSchemeIssueTypeMap = new HashMap<>();

        {
            NodeList customFields = sourceDoc.getElementsByTagName("CustomField");
    
            // CustomFields lesen und hochshiften
            for (int i = 0; i < customFields.getLength(); i++) {
                Node customField = customFields.item(i);
                System.out.println(customField);
                Node idAttribute = customField.getAttributes().getNamedItem("id");
                idAttribute.setNodeValue(String.valueOf(Integer.parseInt(idAttribute.getNodeValue()) + maxCustomFieldID));
                appendNode(newRootNode, customField);
            }
            
        }
        {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            NodeList customFieldValues = sourceDoc.getElementsByTagName("CustomFieldValue");
    
            // CustomFields lesen und hochshiften
            for (int i = 0; i < customFieldValues.getLength(); i++) {
                Node customFieldValue = customFieldValues.item(i);
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {
                        setAttr("id", String.valueOf(Integer.parseInt(getAttrValue("id", customFieldValue)) + maxCustomFieldValue), customFieldValue);
                        setAttr("customfield", String.valueOf(Integer.parseInt(getAttrValue("customfield", customFieldValue)) + maxCustomFieldID), customFieldValue);
                    }

                });

            }
        }
        {
            NodeList fieldConfigSchemes = sourceDoc.getElementsByTagName("FieldConfigScheme");
    
            // CustomFields lesen und hochshiften
            for (int i = 0; i < fieldConfigSchemes.getLength(); i++) {
                Node fieldConfigScheme = fieldConfigSchemes.item(i);
                fieldConfigSchemeMap.put(getAttrValue("id", fieldConfigScheme), fieldConfigScheme);
                this.setAttr("id", String.valueOf(Integer.parseInt(getAttrValue("id", fieldConfigScheme)) + maxFieldConfigScheme), fieldConfigScheme);
                
            }
            
        }

        {
            NodeList fieldConfigSchemeIssueTypes = sourceDoc.getElementsByTagName("FieldConfigSchemeIssueType");
    
            // CustomFields lesen und hochshiften
            for (int i = 0; i < fieldConfigSchemeIssueTypes.getLength(); i++) {
                Node fieldConfigSchemeIssueType = fieldConfigSchemeIssueTypes.item(i);
                this.setAttr("id", String.valueOf(Integer.parseInt(getAttrValue("id", fieldConfigSchemeIssueType)) + maxFieldConfigSchemeIssueType), fieldConfigSchemeIssueType);
                this.setAttr("fieldconfigscheme", String.valueOf(Integer.parseInt(getAttrValue("fieldconfigscheme", fieldConfigSchemeIssueType)) + maxFieldConfigScheme), fieldConfigSchemeIssueType);
                this.setAttr("fieldconfiguration", String.valueOf(Integer.parseInt(getAttrValue("fieldconfiguration", fieldConfigSchemeIssueType)) + maxFieldConfiguration), fieldConfigSchemeIssueType);
                fieldConfigSchemeIssueTypeMap.put(getAttrValue("fieldconfigscheme", fieldConfigSchemeIssueType), fieldConfigSchemeIssueType);
            }
        }
        {
            NodeList fieldConfigurations = sourceDoc.getElementsByTagName("FieldConfiguration");
    
            // CustomFields lesen und hochshiften
            for (int i = 0; i < fieldConfigurations.getLength(); i++) {
                Node fieldConfiguration = fieldConfigurations.item(i);
                this.setAttr("id", String.valueOf(Integer.parseInt(getAttrValue("id", fieldConfiguration)) + maxFieldConfiguration), fieldConfiguration);
                fieldConfigurationMap.put(getAttrValue("id", fieldConfiguration), fieldConfiguration);
            }
        }
        
        {
            NodeList customFieldOptions = sourceDoc.getElementsByTagName("CustomFieldOption");
            Set<Node> copyNodes = new HashSet<>();
            
            for (int i = 0; i < customFieldOptions.getLength(); i++) {
                Node customFieldOption = customFieldOptions.item(i);
                String customFieldOptionOrigId = this.getAttrValue("id", customFieldOption);
                String customfieldIdOrig = this.getAttrValue("customfield", customFieldOption);
                String customfieldconfigIdOrig = this.getAttrValue("customfieldconfig", customFieldOption);
              
                Node fieldConfigScheme = fieldConfigSchemeMap.get(customfieldconfigIdOrig);

                this.setAttr("id", String.valueOf(Integer.parseInt(customFieldOptionOrigId) + maxCustomFieldOption), customFieldOption);
                
                this.setAttr("customfield", String.valueOf(Integer.parseInt(customfieldIdOrig) + maxCustomFieldID), customFieldOption);
                
                this.setAttr("customfieldconfig", this.getAttrValue("id", fieldConfigScheme), customFieldOption);

                appendNode(newRootNode, customFieldOption);

                // Jetzt hier noch FieldConfigScheme mitnehmen, weil wir es hier referenzieren

                this.setAttr("fieldid","customfield_" + getAttrValue("customfield", customFieldOption), fieldConfigScheme);

                copyNodes.add(fieldConfigScheme);

                Node fieldConfigSchemeIssueType = fieldConfigSchemeIssueTypeMap.get(getAttrValue("id", fieldConfigScheme));
                copyNodes.add(fieldConfigSchemeIssueType);

                copyNodes.add(fieldConfigurationMap.get(getAttrValue("fieldconfiguration", fieldConfigSchemeIssueType)));
            }

            for(Node node : copyNodes) {
                appendNode(newRootNode, node);
            }
        }

        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(newDocument);
            File file = new File(sourceXML + "__" + targetXML + ".xml");
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

        }
        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(sourceDoc);
            File file = new File( "entities_shifted.xml");
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

        }
    }

    private boolean hasAttr(String attrName, Node node) {
        Node attribute = node.getAttributes().getNamedItem(attrName);
        return attribute != null;
    }

    private void appendNode(Node newRootNode, Node customField) {
        customField = newDocument.importNode(customField, false);
        newRootNode.appendChild(customField);
    }

    private void setAttr(String attrName, String newValue, Node node) {
        Node attribute = node.getAttributes().getNamedItem(attrName);
        attribute.setNodeValue(newValue);
    }

    private String getAttrValue(String attrName, Node node) {
        Node attribute = node.getAttributes().getNamedItem(attrName);
        return attribute.getNodeValue();
    }

    private Node getNewXMLDocument(Document targetDoc) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Node item = targetDoc.getDocumentElement();

        newDocument = builder.newDocument();

        Node customFieldCopy = newDocument.importNode(item, false);
        newDocument.setStrictErrorChecking(false);

        Node appendChild = newDocument.appendChild(customFieldCopy);

        return appendChild;
    }

    private int getMaxID(String tagName, String attrName, Document document) {
        NodeList customFields = document.getElementsByTagName(tagName);
        int maxValue = 0;

        for (int i = 0; i < customFields.getLength(); i++) {
            Node customField = customFields.item(i);
            String nodeValue = customField.getAttributes().getNamedItem(attrName).getNodeValue();
            int currentValue = Integer.parseInt(nodeValue);

            if (maxValue < currentValue) {
                maxValue = currentValue;
            }
        }

        return maxValue + 1;
    }

    private Document getEntitiesDocumentFromZip(String zipFileName)
            throws IOException, ParserConfigurationException, SAXException {
        ZipFile zipFile = new ZipFile(zipFileName);

        ZipEntry entry = zipFile.getEntry("entities.xml");

        InputStream inputStream = zipFile.getInputStream(entry);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputStream);

        inputStream.close();
        zipFile.close();

        return document;

    }

}
