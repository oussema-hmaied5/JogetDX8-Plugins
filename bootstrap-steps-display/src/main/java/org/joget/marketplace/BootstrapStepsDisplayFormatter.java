package org.joget.marketplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.datalist.service.DataListService;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import javax.servlet.http.HttpServletRequest;

public class BootstrapStepsDisplayFormatter extends DataListColumnFormatDefault {
    
    @Override
    public String getLabel() {
        return "Bootstrap Steps Display Formatter";
    }
    
    @Override
    public String getName() {
        return "Bootstrap Steps Display Formatter";
    }

    @Override
    public String getVersion() {
        return "7.0.2";
    }

    @Override
    public String getDescription() {
        return "Bootstrap Steps Display Formatter";
    }
        
    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        //AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        
        //String recordId = (String) DataListService.evaluateColumnValueFromRow(row, "id");
        //String datalistId = dataList.getId();
        
        String theme = (String)getProperty("theme");
        String template = "";
        
        String content = "";
        
        if(theme.equalsIgnoreCase("1")){
            template = "/templates/BootstrapStepsFormatter.ftl";
        }else if(theme.equalsIgnoreCase("2")){
            template = "/templates/BootstrapStepsFormatterIcon.ftl";
        }
        
        // set options
        Object[] options = (Object[]) getProperty("options");
        Collection<Map> optionMap = new ArrayList<>();
        boolean valueFound = false;

        boolean lastStepReached = false;
        for (Object o : options) {
            Map mapping = (HashMap) o;
            if (value != null && !value.equals("") ){
                if (((String) value).equalsIgnoreCase((String) mapping.get("value"))) {
                    valueFound = true;
                }
            }
        }

        for (Object o : options) {
            Map mapping = (HashMap) o;
            if (valueFound){
                if (!lastStepReached) {
                    if (theme.equalsIgnoreCase("1")) {
                        mapping.put("class", "step-success");
                    } else if (theme.equalsIgnoreCase("2")) {
                        mapping.put("class", "completed");
                    }
                } else {
                    mapping.put("class", "");
                }
                if (((String) value).equalsIgnoreCase((String) mapping.get("value"))) {
                    lastStepReached = true;
                    if (theme.equalsIgnoreCase("1")) {
                        mapping.put("class", "step-active");
                    } else if (theme.equalsIgnoreCase("2")) {
                        mapping.put("class", "completed");
                    }
                }
            } else {
                mapping.put("class", "");
            }
            optionMap.add(mapping);
        }
        
        Map model = new HashMap();
        model.put("options", optionMap);
        //model.put("value", value);
        model.put("columnName", column.getName());
        //model.put("column", column);
        model.put("element", this);
        
        /* Add required stylesheet */
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && request.getAttribute(getClassName()) == null) {
            
            content += pluginManager.getPluginFreeMarkerTemplate(model, getClass().getName(), "/templates/BootstrapStepsFormatterHeader.ftl", null);
            request.setAttribute(getClassName(), true);
        }
        
        content += pluginManager.getPluginFreeMarkerTemplate(model, getClass().getName(), template, null);
        
        return content;
        
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/BootstrapStepsFormatter.json", null, true, "messages/form/BootstrapStepsDisplayField");
    }

    
}
