package org.alfresco.extension;

import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Namespace;
import org.alfresco.repo.dictionary.M2Property;
import org.alfresco.repo.dictionary.M2Type;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

public class CustomModelRegistrar extends AbstractModuleComponent implements InitializingBean {

    private NamespaceService namespaceService;

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @Override
    protected void executeInternal() throws Throwable {
        registerCustomModel();
    }

    private void registerCustomModel() {
        M2Model model = new M2Model();
        model.setName("custom:model");
        model.setAuthor("Your Name");
        model.setDescription("A custom model created programmatically");

        M2Namespace namespace = new M2Namespace("custom", "http://www.alfresco.org/model/custom/1.0");
        model.addNamespace(namespace);

        M2Type type = new M2Type();
        type.setName("custom:customDocumentType");
        type.setParentName("cm:content");
        type.setTitle("Custom Document Type");
        type.setDescription("A custom document type");

        M2Property property = new M2Property();
        property.setName("custom:customProperty");
        property.setType("d:text");
        property.setTitle("Custom Property");
        property.setDescription("A custom property");
        property.setMandatory(false);

        type.addProperty(property);
        model.addType(type);

        // Register the model
        namespaceService.getDictionaryService().putModel(QName.createQName("http://www.alfresco.org/model/custom/1.0", "model"), model);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Ensures the model is registered after properties are set
        registerCustomModel();
    }
}
