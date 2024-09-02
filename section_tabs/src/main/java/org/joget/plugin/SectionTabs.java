package org.joget.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormContainer;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.util.WorkflowUtil;

public class SectionTabs extends Element implements FormBuilderPaletteElement, FormContainer {
    private boolean init = false;
    @Override
    public String getName() {
        return "Section Tabs";
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getDescription() {
        return "Display Section as Tabs";
    }
    
    @Override
    public void setParent(Element parent) {
        super.setParent(parent);
        
        boolean firstChild = true;
        
        //move configured sections under this element
        if (!isFormBuilderActive() && !init && !"true".equals(getPropertyString("__DISABLED__"))) {
            init = true;
            Form form = FormUtil.findRootForm(parent);
            if (form != null) {
                String[] ids = getPropertyString("sections").split(",");
                
                Collection<Element> childs = new ArrayList<Element>();

                for (String id : ids) {
                    Element s = getSection(form, id);
                    if (s != null) {
                        Element sc = new SectionTabsChild();
                        sc.setParent(this);
                        sc.setChildren(s.getChildren());
                        sc.setProperties(s.getProperties());
                        sc.setCustomParameterName(s.getCustomParameterName());
                        sc.setLoadBinder(s.getLoadBinder());
                        sc.setStoreBinder(s.getStoreBinder());
                        if (getPropertyString("load_all").equals("true")) {
                            sc.setProperty("load", "true");
                        } else {
                            if (firstChild) {
                                sc.setProperty("load", "true");
                                firstChild = false;
                            }
                        }
                        form.getChildren().remove(s);
                        childs.add(sc);
                    }
                }

                setChildren(childs);
            }
        }
    }
    
    @Override
    public Collection<Element> getChildren(FormData formData) {
        Collection<Element> newChild = new ArrayList<Element>();
        for (Element e : getChildren()) {
            newChild.add(e);
        }
        return newChild;
    }
    
    protected Element getSection(Form form, String id) {
        for (Element s : form.getChildren()) {
            if (id.equals(s.getPropertyString(FormUtil.PROPERTY_ID))) {
                return s;
            }
        }
        return null;
    }
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "sectionTabs.ftl";

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<span class='form-floating-label'>SECTION TABS</span>";
    }

    @Override
    public String getLabel() {
        return "Section Tabs";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/sectionTabs.json", null, true, "message/SectionTabs");
    }

    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_CUSTOM;
    }

    @Override
    public int getFormBuilderPosition() {
        return 1200;
    }

    @Override
    public String getFormBuilderIcon() {
        return "/plugin/org.joget.apps.form.lib.TextArea/images/textArea_icon.gif";
    }
    
    protected boolean isFormBuilderActive() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        return FormUtil.isFormBuilderActive() 
                || (request != null 
                    && request.getHeader("referer") != null 
                    && request.getHeader("referer").contains("/form/builder/") 
                    && !(request.getRequestURL().toString().contains("/preview/")
                    && request.getRequestURL().toString().contains("/web/fbuilder/app/")));
    }
}
