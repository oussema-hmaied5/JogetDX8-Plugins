package org.joget.plugin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilter;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.apps.form.model.FormAjaxOptionsBinder;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class TypeAheadMultiSelectSearch extends DataListFilterTypeDefault  {
    private final static String MESSAGE_PATH = "messages/TypeAheadMultiSelectSearch";
    private FormBinder optionBinder = null;
    private DataListFilter control = null;

    public String getName() {
        return "TypeAhead Multi Select Search";
    }

    public String getVersion() {
        return "7.0.3";
    }
    
    public String getClassName() {
        return getClass().getName();
    }
    
    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.TypeAheadMultiSelectSearch.pluginLabel", getClassName(), MESSAGE_PATH);
    }
    
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.TypeAheadMultiSelectSearch.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/typeAheadMultiSelectSearch.json", null, true, MESSAGE_PATH);
    }
    
    public String getTemplate(DataList datalist, String name, String label) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map dataModel = new HashMap();
        dataModel.put("element", this);
        dataModel.put("name", datalist.getDataListEncodedParamName(DataList.PARAMETER_FILTER_PREFIX+name));
        dataModel.put("label", label);
        dataModel.put("contextPath", WorkflowUtil.getHttpServletRequest().getContextPath());
        
        String[] values = null;
        values = getValues(datalist, name, getPropertyString("defaultValue"));

        if (values == null) {
            values = new String[]{""};
        }
        dataModel.put("values", values);
        dataModel.put("options", getOptionMap(datalist));
        
        dynamicOptions(datalist);
                
        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), "/templates/typeAheadMultiSelectSearch.ftl", null);
    }
    
    protected void dynamicOptions(DataList datalist) {
        DataListFilter filter = getControlField(datalist);
        if (filter != null) {
            setProperty("controlFieldName", datalist.getDataListEncodedParamName(DataList.PARAMETER_FILTER_PREFIX+filter.getName()));

            if (isAjaxOptionsSupported(datalist, getOptionBinder())) {
                String s = null;
                try {
                    JSONObject jo = new JSONObject();
                    jo.accumulate(FormUtil.PROPERTY_CLASS_NAME, getOptionBinder().getClassName());
                    jo.accumulate(FormUtil.PROPERTY_PROPERTIES, FormUtil.generatePropertyJsonObject(getOptionBinder().getProperties()));

                    s = jo.toString();
                } catch (Exception e) {}
                if (s != null) {
                    AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                    setProperty("appId", appDef.getAppId());
                    setProperty("appVersion", appDef.getVersion());

                    String nonce = SecurityUtil.generateNonce(new String[]{"AjaxOptions", appDef.getAppId(), s.substring(s.length() - 20)}, 1);
                    setProperty("nonce", nonce);

                    try {
                        //secure the data
                        s = SecurityUtil.encrypt(s);
                        s = URLEncoder.encode(s, "UTF-8");
                    } catch (Exception e) {}
                    setProperty("binderData", s);
                }
            }
        }
    }
    
    protected FormBinder getOptionBinder() {
        if (optionBinder == null) {
            Map optionsBinderProperties = (Map) getProperty("optionsBinder");
            if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty()) {
                PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString());
                if (optionBinder != null) {
                    optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));
                }
            }
        }
        return optionBinder;
    } 
    
    protected Collection<Map> getOptionMap(DataList datalist) {
        Collection<Map> optionsMap = new ArrayList<Map>();
        
        if (isAjaxOptionsSupported(datalist, getOptionBinder())) {
            DataListFilter filter = getControlField(datalist);
            String[] controlValues = getValues(datalist, filter.getName());
            
            FormAjaxOptionsBinder binder = (FormAjaxOptionsBinder) getOptionBinder();
            FormRowSet rowSet = binder.loadAjaxOptions(controlValues);
            
            if (rowSet != null) {
                optionsMap = new ArrayList<Map>();
                for (Map row : rowSet) {
                    optionsMap.add(row);
                }
            }
        } else {
            // load from "options" property
            Object[] options = (Object[]) getProperty(FormUtil.PROPERTY_OPTIONS);
            for (Object o : options) {
                Map option = (HashMap) o;
                optionsMap.add(option);
            }

            // load from binder if available
            if (optionBinder != null) {
                FormRowSet rowSet = ((FormLoadBinder) optionBinder).load(null, null, null);
                if (rowSet != null) {
                    optionsMap = new ArrayList<Map>();
                    for (Map row : rowSet) {
                        optionsMap.add(row);
                    }
                }
            }
        }
        
        return optionsMap;
    }
    
    public DataListFilter getControlField(DataList datalist) {
        if (control == null) {
            String name = getPropertyString("controlField");
            if (!name.isEmpty()) {
                DataListFilter[] filterList = datalist.getFilters();

                if (filterList != null) {
                    for (int i = 0; i < filterList.length; i++) {
                        DataListFilter filter = filterList[i];
                        if (name.equals(filter.getName())) {
                            control = filter;
                            return control;
                        }
                    }
                }
            }
        }
        return control;
    }
    
    public boolean isAjaxOptionsSupported(DataList datalist, FormBinder optionBinder) {
        boolean supported = true;
        
        //only support ajax when data encryption and nonce generator are available
        if (!(SecurityUtil.getDataEncryption() != null && SecurityUtil.getNonceGenerator() != null)) {
            supported = false;
        }
        
        //if control field not exist
        if (getControlField(datalist) == null) {
            supported = false;
        }
        
        //if option binder not support ajax
        if (!(optionBinder != null && optionBinder instanceof FormAjaxOptionsBinder && ((FormAjaxOptionsBinder) optionBinder).useAjax())) {
            supported = false;
        }
        return supported;
    }
     
    public DataListFilterQueryObject getQueryObject(DataList datalist, String name) {
        DataListFilterQueryObject queryObject = new DataListFilterQueryObject();
        String[] values = getValues(datalist, name, getPropertyString("defaultValue"));
        if (datalist != null && datalist.getBinder() != null && values != null && (values.length > 0 && !"".equals(values[0]))) {
               
            if (values != null && values.length > 0) {
                String query = "";
                String columnName = datalist.getBinder().getColumnName(name);
                List<String> queries = new ArrayList<String>();
                List<String> valuesList = new ArrayList<String>();
                
                for (String value : values) {
                    
                    if(value.isEmpty()){
                        continue;
                    }
                    //support aggregate function
                    if (columnName.toLowerCase().contains("count(")
                            || columnName.toLowerCase().contains("sum(")
                            || columnName.toLowerCase().contains("avg(")
                            || columnName.toLowerCase().contains("min(")
                            || columnName.toLowerCase().contains("max(")) {
                        queries.add(columnName + " = ?");
                        valuesList.add(value);
                    } else {
                        queries.add("("+columnName+" = ? or "+columnName+" like ? or "+columnName+" like ? or "+columnName+" like ?)");
                        valuesList.add(value);
                        valuesList.add(value + ";%");
                        valuesList.add("%;" + value + ";%");
                        valuesList.add("%;" + value);
                    }
                }
                
                if (values.length > 1) {
                    query = "(" + StringUtils.join(queries, " or ") + ")";
                } else {
                    query = queries.get(0);
                }

                queryObject.setQuery(query);
                queryObject.setValues(valuesList.toArray(new String[0]));

                return queryObject;
            }
        }
        return null;
    }
        
    public Set<String> getOfflineStaticResources() {
        Set<String> urls = new HashSet<String>();
        String contextPath = AppUtil.getRequestContextPath();
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/bootstrap.min.css");
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/bootstrap-multiselect.css");
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/prettify.css\" type=\"text/css");
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/bootstrap-multiselect.js");
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/prettify.js");
        urls.add(contextPath + "/plugin/org.joget.marketplace.TypeAheadMultiSelectSearch/lib/jquerysctipttop.css\" rel=\"stylesheet");
        
        return urls;
    }
}
