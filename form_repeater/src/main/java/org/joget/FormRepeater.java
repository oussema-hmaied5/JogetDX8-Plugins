package org.joget;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.Grid;
import org.joget.apps.form.lib.HiddenField;
import org.joget.apps.form.model.AbstractSubForm;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.joget.apps.form.service.FileUtil;

public class FormRepeater extends Grid implements PluginWebSupport {
	// Base path for internationalized messages
	private final static String MESSAGE_PATH = "message/form/FormRepeater";

	// Map to store cached form rows
	private final Map<FormData, FormRowSet> cachedRowSet = new HashMap<>();

	// Option binder data binding
	private final Map<String, Map<String, FormRowSet>> optionBinderData = new HashMap<>();

	// Map to store FormData by key
	private final Map<String, FormData> formDatas = new HashMap<>();

	// Map to store FormRow by key
	private final Map<String, FormRow> existing = new HashMap<>();

	// Map to store Form by key
	private final Map<String, Form> forms = new HashMap<>();

	// Map to store FormDefinition by key
	private final Map<String, FormDefinition> formDefs = new HashMap<>();

	// Map for elements in error with their ID
	private final Map<String, String> erreurElemnt = new HashMap<>();

	private FormData formData;

	// Method to get the plugin name
	@Override
	public String getName() {
		return "Form Repeater";
	}

	// Method to get the plugin version
	@Override
	public String getVersion() {
		return "7.0.3";
	}

	// Method to get the plugin class name
	@Override
	public String getClassName() {
		return getClass().getName();
	}

	// Method to get the plugin label, with internationalization
	@Override
	public String getLabel() {
		return AppPluginUtil.getMessage("org.joget.FormRepeater.pluginLabel", getClassName(), MESSAGE_PATH);
	}

	// Method to get the plugin description, with internationalization
	@Override
	public String getDescription() {
		return AppPluginUtil.getMessage("org.joget.FormRepeater.pluginDesc", getClassName(), MESSAGE_PATH);
	}

	// Method to get plugin property options from a JSON file
	@Override
	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClass().getName(), "/properties/subformRepeater.json", null, true, MESSAGE_PATH);
	}

	// Method to get the form builder template
	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>" + getLabel() + "</label>";
	}

	// Method to set the form storeBinder
	@Override
	public void setStoreBinder(FormStoreBinder storeBinder) {
		if (storeBinder != null) {
			super.setStoreBinder(new FormRepeaterStoreBinderWrapper(this, storeBinder));
		} else {
			super.setStoreBinder(null);
		}
	}

	// Method to render the form template
	@Override
	public String renderTemplate(FormData formData, Map dataModel) {
		// Information log
		LogUtil.info(" in method renderTemplate()", "");

		// Default template
		String template = "subformRepeater.ftl";

		// Set current formData
		this.formData = formData;

		// Set validator decoration
		String decoration = FormUtil.getElementValidatorDecoration(this, formData);
		dataModel.put("decoration", decoration);
		dataModel.put("customDecorator", getDecorator());

		// Get form rows
		FormRowSet rows = getRows(formData);
		if (rows == null) {
			rows = new FormRowSet();
		}
		dataModel.put("rows", rows);

		// Get form definition
		AppDefinition appDef = AppUtil.getCurrentAppDefinition();
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext().getBean("formDefinitionDao");
		FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef);
		if (formDef != null) {
			// Get form definition details
			String formName = formDef.getName();
			String tableName = formDef.getTableName();
			String desc = formDef.getDescription();

			// Information log about plugin options
			LogUtil.info("Plugin Options - 1", formData.getPrimaryKeyValue());
			if (formData.getPrimaryKeyValue() != null) {
				// Information log about unique ID
				LogUtil.info("id util", FormUtil.getUniqueKey());
				FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

				// Iterate over cached rows to set master ID
				for (FormRow r : cachedRowSet.get(formData)) {
					r.setProperty("id_master", formData.getPrimaryKeyValue());
				}

				// Save or update form data
				formDataDao.saveOrUpdate(getPropertyString("formDefId"), formDef.getTableName(), cachedRowSet.get(formData));
			}
		}

		// Generate HTML from form template
		String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
		return html;
	}


	/**
	 * Retrieves and processes grid data for rendering.
	 *
	 * @param formData The FormData object containing form data.
	 * @return A FormRowSet containing the grid cell data.
	 */
	@Override
	protected FormRowSet getRows(FormData formData) {

		LogUtil.info("--->in method getRows () ", ""); // Log a message indicating entry into the method

		FormRowSet rowSetGlobal = new FormRowSet(); // Initialize a new FormRowSet to store global row data

		this.formData = formData; // Set the instance variable formData to the passed parameter

		if (!cachedRowSet.containsKey(formData)) { // Check if the cachedRowSet does not contain data for formData
			AppDefinition appDef = AppUtil.getCurrentAppDefinition(); // Get the current application definition
			FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext()
					.getBean("formDefinitionDao"); // Retrieve the FormDefinitionDao bean from the application context
			FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef); // Load form definition by ID

			if (formDef != null) { // If form definition exists
				String id = getPropertyString(FormUtil.PROPERTY_ID); // Get the ID property from FormUtil
				String param = FormUtil.getElementParameterName(this); // Get the parameter name for the current element
				FormRowSet rowSet = new FormRowSet(); // Initialize a new FormRowSet for storing row data
				rowSet.setMultiRow(true); // Set multiRow flag to true for rowSet

				if (!FormUtil.isReadonly(this, formData)) { // Check if the current form is not in readonly mode
					LogUtil.info("-----> in !FormUtil.isReadonly(this, formData) ", ""); // Log entry into readonly mode check

					String position = formData.getRequestParameter(param + "_position"); // Get position parameter from formData

					if (position != null) { // If position parameter is not null
						String[] uniqueValues = position.split(";"); // Split position string into unique values

						getBinderData(id, false); // Retrieve binder data for the specified ID

						if (this.formData.getPrimaryKeyValue() == null) { // If primary key value is not set
							String primaryKeyValue = UuidGenerator.getInstance().getUuid(); // Generate a UUID
							this.formData.setPrimaryKeyValue(primaryKeyValue); // Set primary key value in formData
						}

						for (String uv : uniqueValues) { // Iterate through unique values
							if (!uv.isEmpty()) { // If unique value is not empty
								String paramPrefix = uv + "_" + param; // Construct parameter prefix
								String rId = formData.getRequestParameter(paramPrefix + "_" + FormUtil.PROPERTY_ID); // Get request parameter ID
								FormRow r = null; // Initialize FormRow variable r

								if ("disable".equals(getPropertyString("editMode")) && rId != null && !rId.isEmpty()
										&& existing.containsKey(rId)) { // Check edit mode and existing row
									r = existing.get(rId); // Retrieve existing row if conditions are met
								} else {
									FormRow data = null; // Initialize FormRow data variable
									if (rId != null && !rId.isEmpty() && existing.containsKey(rId)) {
										data = existing.get(rId); // Retrieve existing data row if conditions are met
									}
									r = getSubmittedData(uv, paramPrefix, param, data, formData); // Get submitted data for uv
								}
								r.setId(uv); // Set ID of FormRow r to uv
								if (this.formData.getPrimaryKeyValue() != null) { // If primary key value is set
									r.setProperty("id_master", this.formData.getPrimaryKeyValue()); // Set property id_master
								}
								rowSet.add(r); // Add FormRow r to rowSet
							}
						}

						if (this.formData.getPrimaryKeyValue() != null) { // If primary key value is set
							try {
								FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext()
										.getBean("formDataDao"); // Get FormDataDao bean from application context
								FormRowSet results = new FormRowSet(); // Initialize FormRowSet results
								results.setMultiRow(true); // Set multiRow flag to true for results
								String condition = null; // Initialize condition string
								Object[] conditionParams = new Object[1]; // Initialize condition parameters array
								conditionParams[0] = this.formData.getPrimaryKeyValue(); // Set condition parameter
								String labelColumn = (String) getProperty("labelColumn"); // Get labelColumn property
								condition = " WHERE c_id_master = ? "; // Set condition string
								results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(),
										condition, conditionParams, labelColumn, false, null, null); // Perform data retrieval

								List<String> to_delete = new ArrayList<String>(); // Initialize list for items to delete
								List dist_uv = Arrays.asList(uniqueValues); // Create list of unique values

								for (FormRow v : results) { // Iterate through results
									if (!dist_uv.contains(v.getId())) { // If results does not contain unique value
										to_delete.add(v.getId()); // Add ID to deletion list
									}
								}

								formDataDao.delete(getPropertyString("formDefId"), formDef.getTableName(),
										to_delete.toArray(new String[to_delete.size()])); // Delete items from database

								FileUtil.checkAndUpdateFileName(rowSet, formDef.getTableName(), null); // Check and update file names

								formDataDao.saveOrUpdate(getPropertyString("formDefId"), formDef.getTableName(), rowSet); // Save or update data in database

								FileUtil.storeFileFromFormRowSet(rowSet, formDef.getTableName(), null); // Store files associated with FormRowSet

								rowSetGlobal = rowSet; // Set global rowSet to current rowSet

							} catch (NullPointerException e) { // Catch NullPointerException
								LogUtil.info("IO", "Caught the NullPointerException"); // Log exception
							}
						}
					}
				} else {
					LogUtil.info("--->in FormUtil.isReadonly(this, formData) ", ""); // Log entry into readonly mode

					rowSet = getBinderData(id, true); // Retrieve binder data for readonly mode

					rowSetGlobal = rowSet; // Set global rowSet to current rowSet
				}

				FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao"); // Get FormDataDao bean
				FormRowSet results = new FormRowSet(); // Initialize FormRowSet results
				results.setMultiRow(true); // Set multiRow flag to true for results
				String condition = null; // Initialize condition string
				Object[] conditionParams = new Object[1]; // Initialize condition parameters array
				conditionParams[0] = this.formData.getPrimaryKeyValue(); // Set condition parameter
				String labelColumn = (String) getProperty("labelColumn"); // Get labelColumn property
				condition = " WHERE c_id_master = ? "; // Set condition string
				results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(), condition,
						conditionParams, labelColumn, false, null, null); // Perform data retrieval

				cachedRowSet.put(formData, results); // Put results into cachedRowSet
			}

			// Set TempFile
			for (FormRow rowG : rowSetGlobal) { // Iterate through global rowSet
				for (FormRow row : cachedRowSet.get(formData)) { // Iterate through cached rowSet
					if (rowG.getId().equals(row.getId())) { // If IDs match
						String path = FileUtil.getUploadPath("personne", row.getId()); // Get upload path
						row.setTempFilePathMap(rowG.getTempFilePathMap()); // Set temp file path map
						row.setDeleteFilePathMap(rowG.getDeleteFilePathMap()); // Set delete file path map
					}
				}
			}
		}

		return cachedRowSet.get(formData); // Return cached rowSet for formData
	}

	/**
	 * Retrieves submitted data for a given unique value and processes it.
	 *
	 * @param uv         The unique value identifier.
	 * @param prefix     The parameter prefix.
	 * @param paramName  The parameter name.
	 * @param rowData    The existing FormRow data.
	 * @param formData   The FormData object containing form data.
	 * @return A FormRow containing the processed data.
	 */
	protected FormRow getSubmittedData(String uv, String prefix, String paramName, FormRow rowData, FormData formData) {
		FormData rowFormData = new FormData(); // Initialize new FormData object
		LogUtil.info("sd id", formData.getPrimaryKeyValue()); // Log primary key value from formData

		for (String key : formData.getRequestParams().keySet()) { // Iterate through request parameters
			if (key.startsWith(uv) || key.contains("_" + uv + "_")) { // If key matches unique value
				rowFormData.addRequestParameterValues(key, formData.getRequestParameterValues(key)); // Add parameter values to rowFormData
			}
		}

		Form form = getEditableForm(uv); // Get editable form based on unique value
		setOptionBinderData(form.getPropertyString(FormUtil.PROPERTY_ID), form, form, rowFormData); // Set option binder data

		if (rowData != null) { // If existing row data is not null
			FormLoadBinder loadBinder = form.getLoadBinder(); // Get load binder for form
			FormRowSet rowSet = new FormRowSet(); // Initialize FormRowSet
			rowSet.add(rowData); // Add existing row data to rowSet
			rowFormData.setLoadBinderData(loadBinder, rowSet); // Set load binder data for rowFormData
		}

		FormUtil.executeElementFormatDataForValidation(form, rowFormData); // Execute element format data for validation
		FormUtil.executeElementFormatData(form, rowFormData); // Execute element format data

		formDatas.put(uv, rowFormData); // Put rowFormData into formDatas

		FormStoreBinder storeBinder = form.getStoreBinder(); // Get store binder for form
		FormRowSet submittedRows = rowFormData.getStoreBinderData(storeBinder); // Get store binder data

		for (FormRow r : submittedRows) { // Iterate through submitted rows

		}

		if (submittedRows != null && !submittedRows.isEmpty()) { // If submitted rows are not null and not empty
			return submittedRows.get(0); // Return first submitted row
		} else {
			return new FormRow(); // Return empty FormRow
		}
	}

	/**
	 * Retrieves binder data for a given ID and sorts it if required.
	 *
	 * @param id    The identifier.
	 * @param sort  True if sorting is enabled, false otherwise.
	 * @return A FormRowSet containing the binder data.
	 */
	protected FormRowSet getBinderData(String id, boolean sort) {
		FormRowSet rowSet = null; // Initialize FormRowSet rowSet

		String json = getPropertyString(FormUtil.PROPERTY_VALUE); // Get property value as JSON string

		try {
			rowSet = parseFormRowSetFromJson(json); // Parse JSON into FormRowSet
		} catch (Exception ex) {
			LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON"); // Log error if JSON parsing fails
		}

		FormRowSet binderRowSet = formData.getLoadBinderData(this); // Get load binder data from formData

		if (binderRowSet != null) { // If binderRowSet is not null
			if (!binderRowSet.isMultiRow()) { // If binderRowSet is not multi-row
				if (!binderRowSet.isEmpty()) { // If binderRowSet is not empty
					FormRow row = binderRowSet.get(0); // Get first row from binderRowSet
					String jsonValue = row.getProperty(id); // Get property value from row
					LogUtil.info("json", jsonValue); // Log JSON value
					try {
						rowSet = parseFormRowSetFromJson(jsonValue); // Parse JSON value into FormRowSet
					} catch (Exception ex) {
						LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON"); // Log error if JSON parsing fails
					}
				}
			} else {
				rowSet = binderRowSet; // Set rowSet to binderRowSet if it is multi-row
			}
		}

		if (sort && rowSet != null && getPropertyString("enableSorting") != null
				&& getPropertyString("enableSorting").equals("true") && getPropertyString("sortField") != null
				&& !getPropertyString("sortField").isEmpty()) { // If sorting is enabled and conditions are met
			final String sortField = getPropertyString("sortField"); // Get sort field property
			Collections.sort(rowSet, new Comparator<FormRow>() { // Sort rowSet using comparator
				public int compare(FormRow row1, FormRow row2) { // Compare function for sorting
					String number1 = row1.getProperty(sortField); // Get property value from row1
					String number2 = row2.getProperty(sortField); // Get property value from row2

					if (number1 != null && number2 != null) { // If both values are not null
						try {
							return Integer.parseInt(number1) - Integer.parseInt(number2); // Compare integers
						} catch (Exception e) {
							// ignore
						}
					}
					return 0; // Return 0 if comparison fails
				}
			});
		}

		for (FormRow r : rowSet) { // Iterate through rowSet
			existing.put(r.getId(), r); // Put row ID and row into existing map
		}

		AppDefinition appDef = AppUtil.getCurrentAppDefinition(); // Get current application definition
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext()
				.getBean("formDefinitionDao"); // Get FormDefinitionDao bean from application context
		FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef); // Load form definition by ID

		FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao"); // Get FormDataDao bean
		FormRowSet results = new FormRowSet(); // Initialize FormRowSet results
		results.setMultiRow(true); // Set multiRow flag to true for results
		String condition = null; // Initialize condition string
		Object[] conditionParams = new Object[1]; // Initialize condition parameters array
		conditionParams[0] = this.formData.getPrimaryKeyValue(); // Set condition parameter
		LogUtil.info("getSD id", this.formData.getPrimaryKeyValue()); // Log primary key value

		String labelColumn = (String) getProperty("labelColumn"); // Get labelColumn property
		condition = " WHERE c_id_master = ? "; // Set condition string
		results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(), condition, conditionParams,
				labelColumn, false, null, null); // Perform data retrieval

		return results; // Return results
	}

	/**
	 * Retrieves an editable form based on the unique value.
	 *
	 * @param uniqueValue The unique value identifier.
	 * @return The editable Form object.
	 */
	protected Form getEditableForm(String uniqueValue) {
		Form editableForm = forms.get(uniqueValue); // Get editable form from forms map using unique value

		if (editableForm == null) { // If editable form does not exist in forms map
			editableForm = createForm(uniqueValue, getPropertyString("formDefId"), false); // Create editable form
			forms.put(uniqueValue, editableForm); // Put editable form into forms map
		}

		return editableForm; // Return editable form
	}

	/**
	 * Retrieves a readonly form based on the unique value.
	 *
	 * @param uniqueValue The unique value identifier.
	 * @return The readonly Form object.
	 */
	protected Form getReadonlyForm(String uniqueValue) {
		Form readonlyForm = forms.get(uniqueValue); // Get readonly form from forms map using unique value

		if (readonlyForm == null) { // If readonly form does not exist in forms map
			String formDefId = getPropertyString("editFormDefId"); // Get editFormDefId property
			if (formDefId.isEmpty()) { // If editFormDefId is empty
				formDefId = getPropertyString("formDefId"); // Get formDefId property
			}
			readonlyForm = createForm(uniqueValue, formDefId, true); // Create readonly form
			forms.put(uniqueValue, readonlyForm); // Put readonly form into forms map
		}

		return readonlyForm; // Return readonly form
	}


	/**
	 * Creates a Form object based on the JSON definition retrieved from FormDefinition.
	 *
	 * @param uniqueValue The unique value identifier.
	 * @param formDefId   The form definition ID.
	 * @param readonly    Flag indicating if the form is readonly.
	 * @return The created Form object.
	 */
	protected Form createForm(String uniqueValue, String formDefId, boolean readonly) {
		Form form = null; // Initialize Form object as null
		AppDefinition appDef = AppUtil.getCurrentAppDefinition(); // Get current application definition
		FormService formService = (FormService) FormUtil.getApplicationContext().getBean("formService"); // Get FormService bean
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext().getBean("formDefinitionDao"); // Get FormDefinitionDao bean

		FormDefinition formDef = formDefs.get(formDefId); // Attempt to retrieve form definition from cache
		if (formDef == null) { // If form definition not found in cache
			formDef = formDefinitionDao.loadById(formDefId, appDef); // Load form definition from database
			formDefs.put(formDefId, formDef); // Cache loaded form definition
		}
		if (formDef != null) { // If form definition exists
			String json = formDef.getJson(); // Get JSON definition from form definition
			WorkflowAssignment wfAssignment = null; // Initialize WorkflowAssignment object

			// Check if formData and processId are available to get workflow assignment
			if (this.formData != null && this.formData.getProcessId() != null && !this.formData.getProcessId().isEmpty()) {
				formData.setProcessId(this.formData.getProcessId()); // Set processId in formData
				WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager"); // Get WorkflowManager bean
				wfAssignment = wm.getAssignmentByProcess(this.formData.getProcessId()); // Retrieve workflow assignment
			}

			// Process hash variables in JSON definition based on workflow assignment
			json = AppUtil.processHashVariable(json, wfAssignment, StringUtil.TYPE_JSON, null);

			// Create the form from JSON definition
			try {
				form = (Form) formService.createElementFromJson(json); // Parse JSON into Form object
				form.setParent(this); // Set current object as parent of form

				// Automatically add an ID hidden field if not already present
				Element idElement = FormUtil.findElement(FormUtil.PROPERTY_ID, form, formData); // Find ID element in form
				if (idElement == null) { // If ID element not found
					Collection<Element> subFormElements = form.getChildren(); // Get form's child elements
					idElement = new HiddenField(); // Create new HiddenField element
					idElement.setProperty(FormUtil.PROPERTY_ID, FormUtil.PROPERTY_ID); // Set ID property
					idElement.setParent(form); // Set form as parent of ID element
					subFormElements.add(idElement); // Add ID element to form's children
				}

				FormData tempFormData = new FormData(); // Create temporary FormData object
				loadOptionBinders(formDefId, form, form, tempFormData); // Load option binders for form elements

				// Recursively update parameter names for child elements
				String parentId = uniqueValue + "_" + FormUtil.getElementParameterName(this); // Construct parent ID
				updateElement(form, parentId, readonly); // Update elements recursively based on parent ID
			} catch (Exception e) {
				LogUtil.error(AbstractSubForm.class.getName(), e, null); // Log error if exception occurs
			}
		}

		return form; // Return created Form object
	}

	/**
	 * Recursively updates parameter names and properties for elements within a form.
	 *
	 * @param element  The element to update.
	 * @param prefix   The prefix to prepend to parameter names.
	 * @param readonly Flag indicating if the form is readonly.
	 */
	protected void updateElement(Element element, String prefix, boolean readonly) {
		if (prefix == null) { // If prefix is null, set it to empty string
			prefix = "";
		} else { // Otherwise, append element ID to prefix if it has a parent
			String paramName = prefix;
			if (element.getParent() != this) { // Check if element's parent is not current object
				paramName += "_" + element.getPropertyString(FormUtil.PROPERTY_ID); // Append element ID to parameter name
			}
			element.setCustomParameterName(paramName); // Set custom parameter name for element
		}

		// Append form ID to prefix if element is a Form or AbstractSubForm and has a parent
		if (element.getParent() != this && (element instanceof Form || element instanceof AbstractSubForm)) {
			String formId = element.getPropertyString(FormUtil.PROPERTY_ID); // Get form ID from element properties
			if (formId == null) { // If form ID is null, set it to empty string
				formId = "";
			}
			prefix += "_" + formId; // Append form ID to prefix
		}

		boolean readonlyLabel = Boolean.parseBoolean(getPropertyString(FormUtil.PROPERTY_READONLY_LABEL)); // Check if readonly label is enabled
		Collection<Element> children = element.getChildren(); // Get children elements of current element
		for (Element child : children) { // Iterate through children elements
			if (readonly) { // If form is readonly, set child element readonly property to true
				child.setProperty(FormUtil.PROPERTY_READONLY, "true");
			}
			if (readonlyLabel) { // If readonly label is enabled, set child element readonly label property to true
				child.setProperty(FormUtil.PROPERTY_READONLY_LABEL, "true");
			}
			updateElement(child, prefix, readonly); // Recursively update child element
		}
	}

	/**
	 * Sets option binder data for form elements based on form definition ID and FormData.
	 *
	 * @param formDefId The form definition ID.
	 * @param form      The Form object.
	 * @param element   The Element to set option binder data for.
	 * @param formData  The FormData containing form data.
	 */
	protected void setOptionBinderData(String formDefId, Form form, Element element, FormData formData) {
		Map<String, FormRowSet> dataSet = optionBinderData.get(formDefId); // Retrieve option binder data for form definition ID
		if (dataSet != null && !dataSet.isEmpty()) { // If option binder data set is not empty
			FormLoadBinder binder = (FormLoadBinder) element.getOptionsBinder(); // Get load binder for element
			if (binder != null && !FormUtil.isAjaxOptionsSupported(element, formData)) { // Check if AJAX options are supported
				String key = element.getPropertyString(FormUtil.PROPERTY_ID); // Get element's ID property
				if (element.getParent() != null) { // If element has a parent, prepend parent's ID to key
					key = element.getParent().getPropertyString(FormUtil.PROPERTY_ID) + "_" + key;
				}

				FormRowSet rows = dataSet.get(key); // Get rows from dataset based on key
				if (rows != null) { // If rows exist
					formData.setOptionsBinderData(binder, rows); // Set options binder data in formData
				}
			}
			Collection<Element> children = element.getChildren(formData); // Get children elements of current element
			if (children != null) { // If children elements exist
				for (Element child : children) { // Iterate through children elements
					setOptionBinderData(formDefId, form, child, formData); // Recursively set option binder data for child elements
				}
			}
		}
	}

	/**
	 * Loads option binders for form elements recursively based on form definition ID and FormData.
	 *
	 * @param formDefId The form definition ID.
	 * @param form      The Form object.
	 * @param element   The Element to load option binders for.
	 * @param formData  The FormData containing form data.
	 */
	public void loadOptionBinders(String formDefId, Form form, Element element, FormData formData) {
		FormLoadBinder binder = (FormLoadBinder) element.getOptionsBinder(); // Get load binder for element
		if (binder != null && !FormUtil.isAjaxOptionsSupported(element, formData)) { // Check if AJAX options are supported
			String primaryKeyValue = (formData != null) ? element.getPrimaryKeyValue(formData) : null; // Get primary key value from formData if available
			FormRowSet data = binder.load(element, primaryKeyValue, formData); // Load data using binder for element
			if (data != null) { // If data is loaded successfully
				Map<String, FormRowSet> dataSet = optionBinderData.get(formDefId); // Retrieve option binder data set for form definition ID
				if (dataSet == null) { // If option binder data set does not exist, create new HashMap
					dataSet = new HashMap<String, FormRowSet>();
					optionBinderData.put(formDefId, dataSet); // Put new dataset into optionBinderData
				}

				String key = element.getPropertyString(FormUtil.PROPERTY_ID); // Get element's ID property
				if (element.getParent() != null) { // If element has a parent, prepend parent's ID to key
					key = element.getParent().getPropertyString(FormUtil.PROPERTY_ID) + "_" + key;
				}

				dataSet.put(key, data); // Put loaded data into dataset with key
			}
		}
		Collection<Element> children = element.getChildren(formData); // Get children elements of current element
		if (children != null) { // If children elements exist
			for (Element child : children) { // Iterate through children elements
				loadOptionBinders(formDefId, form, child, formData); // Recursively load option binders for child elements
			}
		}
	}

	/**
	 * Generates HTML template for a table row based on provided row data, element parameter name, and mode.
	 *
	 * @param rowMap          The map containing row data.
	 * @param elementParamName The parameter name of the form element.
	 * @param mode            The mode indicating position in table (oneTop, oneBottom, etc.).
	 * @return The generated HTML template for the table row.
	 */
	public String getRowTemplate(Map rowMap, String elementParamName, String mode) {
		FormRow row = null; // Initialize FormRow object
		if (rowMap != null) { // If rowMap is not null, create FormRow from rowMap
			row = new FormRow();
			row.putAll(rowMap);
		}

		String uniqueValue = "uv" + Long.toString(System.currentTimeMillis()); // Generate unique value

		// Determine uniqueValue based on row and mode
		if (row != null && row.getProperty("id") != null) {
			uniqueValue = row.getProperty("id");
		} else if ("oneTop".equals(mode) || "oneBottom".equals(mode)) {
			uniqueValue = mode;
		}

		// Skip rendering form on top or bottom in table body
		if (("oneTop".equals(uniqueValue) && !"oneTop".equals(mode))
				|| ("oneBottom".equals(uniqueValue) && !"oneBottom".equals(mode))) {
			return "";
		}

		String rId = null;
		if (row != null && row.getId() != null) {
			rId = row.getId();
		}

		Form form = null; // Initialize Form object
		String readonlyCss = ""; // Initialize readonly CSS class

		// Determine form and readonly state based on conditions
		if (FormUtil.isReadonly(this, formData)
				|| ("disable".equals(getPropertyString("editMode")) && rId != null && !rId.isEmpty() && existing.containsKey(rId))) {
			form = getReadonlyForm(uniqueValue); // Get readonly form
			readonlyCss = " readonly"; // Set readonly CSS class
		} else {
			form = getEditableForm(uniqueValue); // Get editable form
		}

		if (form == null) { // If form is null, return empty string
			return "";
		}

		String cssClass = "grid-row " + mode; // CSS class for table row
		if (row == null || row.getId() == null) {
			cssClass += " new"; // Add 'new' class if row ID is null
		}
		String html = "<tr class=\"" + cssClass + "\">"; // Begin HTML for table row

		// Add sorting column if enabled and applicable
		if (!FormUtil.isReadonly(this, formData)
				&& getPropertyString("enableSorting") != null
				&& getPropertyString("enableSorting").equals("true") && getPropertyString("sortField") != null
				&& !getPropertyString("sortField").isEmpty()) {
			if (("oneTop".equals(getPropertyString("addMode")) || "oneBottom".equals(getPropertyString("addMode")))
					&& "disable".equals(getPropertyString("editMode"))) {
				// ignore sorting
			} else if (!("oneTop".equals(mode) || "oneBottom".equals(mode))) {
				html += "<td class=\"order\"><a title=\""
						+ AppPluginUtil.getMessage("form.subformRepeater.sort", getClassName(), MESSAGE_PATH)
						+ "\"><span ></span></a></td>"; // Add sorting column
			} else {
				html += "<td></td>"; // Add empty column
			}
		}

		html += "<td class=\"subform_wrapper\">"; // Begin subform wrapper

		// Add hidden input for unique value
		html += "<input type=\"hidden\" class=\"unique_value\" name=\"" + elementParamName + "_unique_value\" value=\""
				+ uniqueValue + "\" />";

		FormData rowFormData = formDatas.get(uniqueValue); // Get FormData for current unique value
		if (rowFormData == null) {
			rowFormData = new FormData(); // Create new FormData if not already present
			setOptionBinderData(form.getPropertyString(FormUtil.PROPERTY_ID), form, form, rowFormData); // Set option binder data

			if (row != null) {
				// Set load binder data if row is not null
				FormLoadBinder loadBinder = form.getLoadBinder();
				FormRowSet rowSet = new FormRowSet();
				rowSet.add(row);
				rowFormData.setLoadBinderData(loadBinder, rowSet);
			}
		}

		// Load data for child elements
		rowFormData.setPrimaryKeyValue(rId); // Set primary key value in FormData
		Collection<Element> children = form.getChildren(rowFormData); // Get child elements of form
		if (children != null) {
			for (Element child : children) {
				FormUtil.executeLoadBinders(child, rowFormData); // Execute load binders for child elements
			}
		}

		// Generate HTML for subform container
		html += "<div class=\"subform-container no-frame" + readonlyCss + "\">";

		// Render form HTML and adjust classes
		String formHtml = form.render(rowFormData, false);
		formHtml = formHtml.replaceAll("\"form-section", "\"subform-section");
		formHtml = formHtml.replaceAll("\"form-column", "\"subform-column");
		formHtml = formHtml.replaceAll("\"form-cell", "\"subform-cell");

		// Fix form Hash Variable if recordId is present
		if (rId != null && !rId.isEmpty() && formHtml.contains("{recordId}")) {
			formHtml = formHtml.replaceAll(StringUtil.escapeRegex("{recordId}"), rId);
			formHtml = AppUtil.processHashVariable(formHtml, null, null, null);
		}

		// Parse HTML into Document for manipulation
		Document doc = Jsoup.parse(formHtml);

		String errorMsg;

		// Validate and add error messages for multi-row elements
		for (org.jsoup.nodes.Element div : doc.select("div.subform-column").select("div.subform-cell")) {

			errorMsg = this.validateMultiRow(div.toString(), rowFormData.getPrimaryKeyValue());

			if (!errorMsg.isEmpty()) {
				// Append error message span if validation fails
				div.after("<span class=\"form-error-message\" style=\"display: flex; justify-content: center;\">" + errorMsg
						+ "</span>" + "</div></td>\n");
			}

		}
		formHtml = doc.html(); // Get modified HTML from Document

		html += formHtml; // Append form HTML to main HTML

		html += "</div>"; // Close subform container

		html += "</td>"; // Close subform wrapper

		// Add repeater actions (delete, add, collapse) if enabled
		if ("enable".equals(getPropertyString("deleteMode")) || "enable".equals(getPropertyString("addMode"))
				|| "true".equals(getPropertyString("collapsible"))) {
			html += "<td class=\"repeater-action\">"; // Begin repeater action column
			if (!("oneTop".equals(mode) || "oneBottom".equals(mode))) {
				if (!FormUtil.isReadonly(this, formData) && "enable".equals(getPropertyString("addMode"))) {
					html += "<a class=\"repeater-action-add add-row-before\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.add", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>"; // Add 'add row' action
				}
				if ("true".equals(getPropertyString("collapsible"))) {
					html += "<a class=\"repeater-collapsible\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.collapse", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>"; // Add 'collapse' action
				}
				if (readonlyCss.isEmpty() && "enable".equals(getPropertyString("deleteMode"))) {
					html += "<a class=\"repeater-action-delete\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.delete", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>"; // Add 'delete row' action
				}
			}
			html += "</td>"; // Close repeater action column
		}

		html += "</tr>"; // Close table row

		LogUtil.info("---->mode ", mode); // Log mode information

		return html; // Return generated HTML template
	}

	/**
	 * Validates multi-row elements based on given HTML and primary key value.
	 *
	 * @param divs The HTML content to validate.
	 * @param pk   The primary key value to match.
	 * @return The error message if validation fails, empty string otherwise.
	 */
	public String validateMultiRow(String divs, String pk) {
		LogUtil.info("--->pk ", pk); // Log primary key value

		String errorMsg = ""; // Initialize error message
		String uv;
		String field;

		// Iterate through error elements map to find matching errors
		for (Map.Entry<String, String> map : erreurElemnt.entrySet()) {
			LogUtil.info("-->key map ", map.getKey()); // Log map key
			LogUtil.info("--> value ", map.getValue()); // Log map value

			int pos = map.getKey().indexOf("_"); // Find position of underscore in key

			uv = (String) map.getKey().subSequence(0, pos); // Extract unique value from key
			field = map.getKey().substring(pos); // Extract field from key

			// Check if unique value matches pk and divs contains field
			if (uv.equals(pk) && divs.contains(field)) {
				return map.getValue(); // Return error message from map if match found
			}
		}

		return errorMsg; // Return empty string if no validation errors found
	}

	/**
	 * Validates the form data including all editable rows.
	 *
	 * @param formData The FormData object containing form data to validate.
	 * @return True if validation passes, false otherwise.
	 */
	@Override
	public Boolean selfValidate(FormData formData) {
		this.formData = formData; // Set formData for current instance
		Boolean valid = super.selfValidate(formData); // Call superclass method for initial validation

		// Validate all editable rows
		boolean rowsValid = true;
		FormRowSet rowSet = getRows(formData); // Get rows from FormData

		for (FormRow r : rowSet) {
			String uv = r.getProperty("id"); // Get unique value (id) of row

			FormData rowFormData = formDatas.get(uv); // Get FormData for current unique value

			LogUtil.info(" in method self validate row form data uv", uv); // Log unique value

			if (rowFormData != null) {
				Form form = getEditableForm(uv); // Get editable form for unique value
				FormUtil.executeValidators(form, rowFormData); // Execute validators for form

				if (form.hasError(rowFormData)) { // Check if form has errors
					rowsValid = false; // Set rowsValid to false if errors found

					// Add row validation errors to erreurElemnt map
					for (Map.Entry<String, String> map : rowFormData.getFormErrors().entrySet()) {
						int pos = map.getKey().indexOf("_"); // Find position of underscore in key

						String idElement = uv + map.getKey().substring(pos); // Construct error element ID

						if (erreurElemnt.containsKey(idElement)) {
							erreurElemnt.replace(idElement, map.getValue()); // Replace existing error message
						} else {
							erreurElemnt.put(idElement, map.getValue()); // Add new error message
						}
					}
				}
			}
		}

		if (!rowsValid) {
			valid = false; // Set valid to false if any row validation fails
			String id = FormUtil.getElementParameterName(this); // Get element parameter name
			String errorMsg = AppPluginUtil.getMessage("form.subformRepeater.error.rowData", getClassName(),
					MESSAGE_PATH); // Get error message
			formData.addFormError(id, errorMsg); // Add form error message
		}

		return valid; // Return overall validation status
	}


	/**
	 * Retrieves the decorator based on the form configuration.
	 *
	 * @return The decorator string ('*' if minRow validation is enabled, empty string otherwise).
	 */
	protected String getDecorator() {
		String decorator = ""; // Initialize decorator string

		try {
			String min = getPropertyString("validateMinRow"); // Get minRow validation property

			if ((min != null && !min.isEmpty())) {
				int minNumber = Integer.parseInt(min); // Parse minRow value to integer

				if (minNumber > 0) {
					decorator = "*"; // Set decorator to '*' if minRow is greater than 0
				}
			}
		} catch (Exception e) {
			// Handle exception (if any)
		}

		return decorator; // Return decorator string
	}

	@Override
	public FormRowSet formatData(FormData formData) {
		this.formData = formData;

		// get form rowset
		FormRowSet rowSet = getRows(formData);
		rowSet.setMultiRow(true);

		try {
			int count = 0;
			for (FormRow r : rowSet) {
				// set sorting
				if (getPropertyString("enableSorting") != null && getPropertyString("enableSorting").equals("true")
						&& getPropertyString("sortField") != null && !getPropertyString("sortField").isEmpty()) {
					String sortField = getPropertyString("sortField");
					r.setProperty(sortField, Integer.toString(count));
				}

				count++;
			}
		} catch (Exception ex) {
			LogUtil.error(Grid.class.getName(), ex, "");
		}

		return rowSet;
	}

	/**
	 * Stores inner data for each row in the FormRowSet using form store binders.
	 *
	 * @param rows The FormRowSet containing rows to store.
	 */
	public void storeInnerData(FormRowSet rows) {
		FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService"); // Get FormService bean

		for (FormRow r : rows) {
			String uv = r.getProperty("RS_UNIQUE_VALUE"); // Get unique value (RS_UNIQUE_VALUE) from row properties
			FormData rowFormData = formDatas.get(uv); // Get FormData for current unique value

			// Execute form store binders for each element in the form
			if (rowFormData != null && rowFormData.getStoreBinders().size() > 1) {
				Form form = getEditableForm(uv); // Get editable form for unique value

				// Generate primary key value if not already present
				if (rowFormData.getPrimaryKeyValue() == null || rowFormData.getPrimaryKeyValue().isEmpty()) {
					if (r.getId() == null || r.getId().isEmpty()) {
						r.setId(UuidGenerator.getInstance().getUuid()); // Generate UUID if row ID is null
					}
					rowFormData.setPrimaryKeyValue(r.getId()); // Set primary key value in FormData
				}

				// Execute form store binders recursively for each element in the form
				for (Element e : form.getChildren()) {
					formService.recursiveExecuteFormStoreBinders(form, e, rowFormData);
				}
			}
			r.remove("RS_UNIQUE_VALUE"); // Remove RS_UNIQUE_VALUE from row properties
		}
	}

	/**
	 * Executes default actions when adding a form element in "oneTop" or "oneBottom" mode.
	 * Handles setting workflow variables and running post-processing based on configuration.
	 *
	 * @param formData The FormData object containing form data.
	 */
	public void executeFormActionForDefaultAddForm(FormData formData) {
		if ("oneTop".equals(getPropertyString("addMode")) || "oneBottom".equals(getPropertyString("addMode"))) {
			// Check if setting workflow variables or running post-processing is enabled
			if ("true".equals(getPropertyString("setWorkflowVariable")) || "true".equals(getPropertyString("runPostProcessing"))) {
				Form form = getEditableForm(getPropertyString("addMode")); // Get editable form based on addMode
				FormStoreBinder storeBinder = form.getStoreBinder(); // Get store binder for form
				FormData rowFormData = formDatas.get(getPropertyString("addMode")); // Get FormData for addMode
				FormRowSet submitedRows = rowFormData.getStoreBinderData(storeBinder); // Get submitted rows from store binder
				FormRow submitedRow = submitedRows.get(0); // Get first submitted row
				String id = submitedRow.getId(); // Get ID of submitted row
				rowFormData.setPrimaryKeyValue(id); // Set primary key value in FormData

				// Set workflow variable if enabled
				if ("true".equals(getPropertyString("setWorkflowVariable"))) {
					String activityId = formData.getActivityId(); // Get activity ID from formData
					String processId = formData.getProcessId(); // Get process ID from formData

					// Proceed if activityId or processId is not null
					if (activityId != null || processId != null) {
						Map<String, String> variableMap = new HashMap<String, String>(); // Initialize variableMap
						variableMap = storeWorkflowVariables(getEditableForm(getPropertyString("addMode")), submitedRow, variableMap); // Store workflow variables

						// Send variables to workflow manager if map is not empty
						if (!variableMap.isEmpty()) {
							WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
							if (activityId != null) {
								workflowManager.activityVariables(activityId, variableMap); // Set activity variables
							} else {
								workflowManager.processVariables(processId, variableMap); // Set process variables
							}
						}
					}
				}

				// Execute form post processing if enabled
				if ("true".equals(getPropertyString("runPostProcessing"))) {
					FormUtil.executePostFormSubmissionProccessor(form, rowFormData); // Execute post-processing
				}
			}
		}
	}

	/**
	 * Recursively stores workflow variables from elements into a map.
	 *
	 * @param element     The current element to process.
	 * @param row         The FormRow containing data to store.
	 * @param variableMap The map to store workflow variables.
	 * @return Updated map with stored workflow variables.
	 */
	protected Map<String, String> storeWorkflowVariables(Element element, FormRow row, Map<String, String> variableMap) {
		String variableName = element.getPropertyString(AppUtil.PROPERTY_WORKFLOW_VARIABLE); // Get workflow variable name from element properties

		// Store variable if name is not empty
		if (variableName != null && !variableName.trim().isEmpty()) {
			String id = element.getPropertyString(FormUtil.PROPERTY_ID); // Get element ID
			String value = (String) row.get(id); // Get value from row using element ID
			if (value != null) {
				variableMap.put(variableName, value); // Store variable in map
			}
		}

		// Recursively process child elements
		for (Iterator<Element> i = element.getChildren().iterator(); i.hasNext();) {
			Element child = i.next();
			storeWorkflowVariables(child, row, variableMap); // Recursive call for child elements
		}

		return variableMap; // Return updated map with stored variables
	}


	/**
	 * Constructs and returns the service URL for handling AJAX requests related to FormRepeater.
	 *
	 * @return The constructed service URL as a string.
	 */
	public String getServiceUrl() {
		if ("enable".equals(getPropertyString("addMode"))) {
			String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/plugin/org.joget.FormRepeater/service"; // Base URL for service
			AppDefinition appDef = AppUtil.getCurrentAppDefinition(); // Get current application definition

			// Create nonce for security
			String paramName = FormUtil.getElementParameterName(this); // Get parameter name of element
			String nonce = SecurityUtil.generateNonce(new String[]{"FormRepeater", appDef.getAppId(), appDef.getVersion().toString(), paramName}, 1); // Generate nonce

			try {
				// Construct URL with encoded parameters
				url = url + "?_nonce=" + URLEncoder.encode(nonce, "UTF-8") + "&_paramName=" + URLEncoder.encode(paramName, "UTF-8")
						+ "&_appId=" + URLEncoder.encode(appDef.getAppId(), "UTF-8") + "&_appVersion=" + URLEncoder.encode(appDef.getVersion().toString(), "UTF-8");
				url += "&_enableSorting=" + URLEncoder.encode(getPropertyString("enableSorting"), "UTF-8");
				url += "&_sortField=" + URLEncoder.encode(getPropertyString("sortField"), "UTF-8");
				url += "&_formDefId=" + URLEncoder.encode(getPropertyString("formDefId"), "UTF-8");
				url += "&_deleteMode=" + URLEncoder.encode(getPropertyString("deleteMode"), "UTF-8");
				url += "&_addMode=" + URLEncoder.encode(getPropertyString("addMode"), "UTF-8");
				url += "&_collapsible=" + URLEncoder.encode(getPropertyString("collapsible"), "UTF-8");
				url += "&_processId=" + URLEncoder.encode((this.formData.getProcessId() != null ? this.formData.getProcessId() : ""), "UTF-8");
			} catch (Exception e) {
				// Handle exception if encoding fails
			}

			return url; // Return constructed service URL
		} else {
			return ""; // Return empty string if addMode is not enabled
		}
	}

	/**
	 * Handles HTTP POST requests for the FormRepeater service.
	 * Validates nonce and other parameters before processing the request.
	 *
	 * @param request  The HttpServletRequest object containing the request parameters.
	 * @param response The HttpServletResponse object to send response back.
	 * @throws ServletException If an error occurs during servlet execution.
	 * @throws IOException      If an I/O error occurs during servlet execution.
	 */
	public void webService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String nonce = request.getParameter("_nonce"); // Get nonce parameter from request
		String paramName = request.getParameter("_paramName"); // Get paramName parameter from request
		String appId = request.getParameter("_appId"); // Get appId parameter from request
		String appVersion = request.getParameter("_appVersion"); // Get appVersion parameter from request

		// Verify nonce validity
		if (SecurityUtil.verifyNonce(nonce, new String[]{"FormRepeater", appId, appVersion, paramName})) {
			// Proceed if HTTP method is POST
			if ("POST".equalsIgnoreCase(request.getMethod())) {
				try {
					AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService"); // Get AppService bean
					AppDefinition appDef = appService.getAppDefinition(appId, appVersion); // Get AppDefinition using appId and appVersion

					// Set properties from request parameters
					setProperty("enableSorting", request.getParameter("_enableSorting"));
					setProperty("sortField", request.getParameter("_sortField"));
					setProperty("formDefId", request.getParameter("_formDefId"));
					setProperty("deleteMode", request.getParameter("_deleteMode"));
					setProperty("addMode", request.getParameter("_addMode"));
					setProperty("collapsible", request.getParameter("_collapsible"));

					setCustomParameterName(paramName); // Set custom parameter name

					this.formData = new FormData(); // Initialize FormData
					this.formData.setProcessId(request.getParameter("_processId")); // Set processId from request parameter

					String template = getRowTemplate(null, paramName, "body"); // Generate row template HTML

					if (template != null && !template.isEmpty()) {
						// Return HTML template as response
						response.setContentType("text/html");
						PrintWriter writer = response.getWriter();
						writer.write(template);
					} else {
						response.setStatus(HttpServletResponse.SC_NO_CONTENT); // Set status to NO_CONTENT if template is empty
					}
				} catch (Exception ex) {
					// Handle exception if any error occurs
				}
			} else {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT); // Set status to NO_CONTENT for non-POST requests
			}
		} else {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Set status to FORBIDDEN if nonce verification fails
		}
	}

}
