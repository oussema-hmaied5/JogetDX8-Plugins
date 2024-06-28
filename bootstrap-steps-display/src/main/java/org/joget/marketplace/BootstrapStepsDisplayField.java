package org.joget.marketplace;

import java.util.Collection;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;

public class BootstrapStepsDisplayField extends Element implements FormBuilderPaletteElement {

    @Override
    public String getName() {
        return "Bootstrap Steps Display Field";
    }

    @Override
    public String getVersion() {
        return "7.0.2";
    }

    @Override
    public String getDescription() {
        return "Bootstrap Steps Display Field";
    }

    public Collection<Map> getOptionMap(FormData formData) {
        Collection<Map> optionMap = FormUtil.getElementPropertyOptionsMap(this, formData);
        return optionMap;
    }
    
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String theme = (String)getProperty("theme");
        String template = "";

        if(theme.equalsIgnoreCase("1")){
            template = "BootstrapStepsDisplayField.ftl";
        }else if(theme.equalsIgnoreCase("2")){
            template = "BootstrapStepsDisplayFieldIcon.ftl";
        }
        
        //set scale
        String scale = (String)getProperty("scale");
        if(scale == null){
            if(theme.equalsIgnoreCase("1")){
                scale = "1.5";
            }else if(theme.equalsIgnoreCase("2")){
                scale = "1";
            }
        }
        dataModel.put("scale", scale);
        
        // set options
        Collection<Map> optionMap = getOptionMap(formData);
        dataModel.put("options", optionMap);
        
        Form form = FormUtil.findRootForm(this);
        
        Element statusElement = FormUtil.findElement(getPropertyString("statusFieldId"), form, formData);
        if (statusElement != null) {
            String statusValue = FormUtil.getElementPropertyValue(statusElement, formData);
            dataModel.put("value", statusValue);
        }

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }
    
//    public FormRowSet formatData(FormData formData) {
//        FormRowSet rowSet = null;
//
//        // get value
//        String id = getPropertyString(FormUtil.PROPERTY_ID);
//        if (id != null) {
//            String value = FormUtil.getElementPropertyValue(this, formData);
//            if (value != null) {
//                // set value into Properties and FormRowSet object
//                FormRow result = new FormRow();
//                result.setProperty(id, value);
//                rowSet = new FormRowSet();
//                rowSet.add(result);
//            }
//        }
//
//        return rowSet;
//    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>Steps Display</label>";
    }

    @Override
    public String getLabel() {
        return "Steps Display";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/BootstrapStepsDisplayField.json", null, true, "messages/form/BootstrapStepsDisplayField");
    }

    @Override
    public String getFormBuilderCategory() {
        return "Marketplace";
    }

    @Override
    public int getFormBuilderPosition() {
        return 500;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-info\"></i>";
    }

}
