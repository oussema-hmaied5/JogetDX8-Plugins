package org.joget;

import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;

public class FormRepeaterStoreBinderWrapper extends FormBinder implements FormStoreBinder {
    private FormRepeater formRepeater;
    private FormStoreBinder storeBinder;
    
    public String getName() {
        return "FormRepeaterStoreBinderWrapper";
    }

    public String getVersion() {
        return "7.0.3";
    }

    public String getDescription() {
        return "";
    }

    public String getLabel() {
        return "Grid Inner Data Store Binder Wrapper";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return "";
    }
    
    public FormRepeaterStoreBinderWrapper (FormRepeater formRepeater, FormStoreBinder storeBinder) {
        this.formRepeater = formRepeater;
        this.storeBinder = storeBinder;
    }

    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        if (rows != null && !rows.isEmpty()) {
            //store inner form/grid data
            formRepeater.storeInnerData(rows);
        }
        
        if (rows != null) {    
            storeBinder.store(element, rows, formData);
        }
        
        if (rows != null && !rows.isEmpty()) {
            //execute addition form action for add mode equeal to show empty form on top or bottom
            formRepeater.executeFormActionForDefaultAddForm(formData);
        }
        
        return rows;
    }
}
