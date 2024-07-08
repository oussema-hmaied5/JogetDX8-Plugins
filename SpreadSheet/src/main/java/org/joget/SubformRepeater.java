package org.joget;

import java.io.File;
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

import jdk.jpackage.internal.Log;
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
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.*;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SubformRepeater extends Grid implements PluginWebSupport {

	private final static String MESSAGE_PATH = "message/form/SubformRepeater";
	private Map<FormData, FormRowSet> cachedRowSet = new HashMap<FormData, FormRowSet>();
	private FormData formData;
	private Map<String, Map<String, FormRowSet>> optionBinderData = new HashMap<String, Map<String, FormRowSet>>();
	private Map<String, FormData> formDatas = new HashMap<String, FormData>();
	private Map<String, FormRow> existing = new HashMap<String, FormRow>();
	private Map<String, Form> forms = new HashMap<String, Form>();
	private Map<String, FormDefinition> formDefs = new HashMap<String, FormDefinition>();
	// erreur element with id
	private Map<String, String> erreurElemnt = new HashMap<String, String>();

	private Boolean validForm = true;

	@Override
	public String getName() {
		return "Spread Sheet";
	}

	@Override
	public String getVersion() {
		return "7.0.3";
	}

	@Override
	public String getClassName() {
		return getClass().getName();
	}

	@Override
	public String getLabel() {
		// support i18n
		return AppPluginUtil.getMessage("org.joget.SubformRepeater.pluginLabel", getClassName(), MESSAGE_PATH);
	}

	@Override
	public String getDescription() {
		// support i18n
		return AppPluginUtil.getMessage("org.joget.SubformRepeater.pluginDesc", getClassName(), MESSAGE_PATH);
	}

	@Override
	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClass().getName(), "/properties/subformRepeater.json", null, true,
				MESSAGE_PATH);
	}

	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>" + getLabel() + "</label>";
	}

	@Override
	public void setStoreBinder(FormStoreBinder storeBinder) {
		if (storeBinder != null) {
			super.setStoreBinder(new SubformRepeaterStoreBinderWrapper(this, storeBinder));
		} else {
			super.setStoreBinder(null);
		}
	}

	@Override
	public String renderTemplate(FormData formData, Map dataModel) {

		String template = "subformRepeater.ftl";

		this.formData = formData;

		// set validator decoration
		String decoration = FormUtil.getElementValidatorDecoration(this, formData);
		dataModel.put("decoration", decoration);
		dataModel.put("customDecorator", getDecorator());

		// set rows
		FormRowSet rows = getRows(formData);
		if (rows == null) {
			rows = new FormRowSet();
		}
		dataModel.put("rows", rows);

		AppDefinition appDef = AppUtil.getCurrentAppDefinition();
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext()
				.getBean("formDefinitionDao");
		FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef);
		if (formDef != null) {

			String formName = formDef.getName();
			String tableName = formDef.getTableName();
			String desc = formDef.getDescription();

			LogUtil.info("Plugin Options - 1", formData.getPrimaryKeyValue());
			if (formData.getPrimaryKeyValue() != null) {
				LogUtil.info("id util", FormUtil.getUniqueKey());
				FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
				for (FormRow r : cachedRowSet.get(formData)) {
					r.setProperty("id_master", formData.getPrimaryKeyValue());
				}
				formDataDao.saveOrUpdate(getPropertyString("formDefId"), formDef.getTableName(),
						cachedRowSet.get(formData));
				// getRows(formData);
			}
		}
		String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
		return html;
	}

	/**
	 * Return the grid data
	 *
	 * @param formData
	 * @return A FormRowSet containing the grid cell data.
	 */
	@Override
	protected FormRowSet getRows(FormData formData) {
		this.formData = formData;
		LogUtil.info("id util22", formData.getActivityId());
		FormRowSet rowSetGlobal = new FormRowSet();

		if (!cachedRowSet.containsKey(formData)) {
			AppDefinition appDef = AppUtil.getCurrentAppDefinition();
			FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext().getBean("formDefinitionDao");
			FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef);

			if (formDef != null) {
				String id = getPropertyString(FormUtil.PROPERTY_ID);
				String param = FormUtil.getElementParameterName(this);
				FormRowSet rowSet = new FormRowSet();
				rowSet.setMultiRow(true);

				if (!FormUtil.isReadonly(this, formData)) {
					String position = formData.getRequestParameter(param + "_position");

					if (position != null) {
							String[] uniqueValues = position.split(";");

						// Retrieve original data
						getBinderData(id, false);

						if (this.formData.getPrimaryKeyValue() == null) {
							String primaryKeyValue = UuidGenerator.getInstance().getUuid();
							this.formData.setPrimaryKeyValue(primaryKeyValue);
						}

						for (String uv : uniqueValues) {
							if (!uv.isEmpty()) {
								String paramPrefix = uv + "_" + param;
								String rId = formData.getRequestParameter(paramPrefix + "_" + FormUtil.PROPERTY_ID);
								FormRow r = null;
								if ("disable".equals(getPropertyString("editMode")) && rId != null && !rId.isEmpty() && existing.containsKey(rId)) {
									r = existing.get(rId);
								} else {
									FormRow data = null;
									if (rId != null && !rId.isEmpty() && existing.containsKey(rId)) {
										data = existing.get(rId);
									}
									r = getSubmittedData(uv, paramPrefix, param, data, formData);
								}
								r.setProperty("RS_UNIQUE_VALUE", uv);
								r.setId(uv);
								if (this.formData.getPrimaryKeyValue() != null) {
									r.setProperty("id_master", this.formData.getPrimaryKeyValue());
								}
								rowSet.add(r);
							}
						}

						LogUtil.info("Plugin Options - 1.5", formData.getPrimaryKeyValue());

						if (this.formData.getPrimaryKeyValue() != null) {
							try {
								FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
								FormRowSet results = new FormRowSet();
								results.setMultiRow(true);
								String condition = " WHERE c_id_master = ? ";
								Object[] conditionParams = new Object[] { this.formData.getPrimaryKeyValue() };

								results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(), condition, conditionParams, null, false, null, null);
								List<String> toDelete = new ArrayList<>();
								List<String> distUv = Arrays.asList(uniqueValues);

								for (FormRow v : results) {
									if (!distUv.contains(v.getId())) {
										toDelete.add(v.getId());
									}
								}

								LogUtil.info("List TO DELETE", toDelete.toString());
								FileUtil.checkAndUpdateFileName(rowSet, formDef.getTableName(), null);

								formDataDao.delete(getPropertyString("formDefId"), formDef.getTableName(), toDelete.toArray(new String[0]));

								if (validForm) {
									formDataDao.saveOrUpdate(getPropertyString("formDefId"), formDef.getTableName(), rowSet);
								}
								FileUtil.checkAndUpdateFileName(rowSet, formDef.getTableName(), null);
								FileUtil.storeFileFromFormRowSet(rowSet, formDef.getTableName(), null);
								rowSetGlobal = rowSet;

							} catch (NullPointerException e) {
								LogUtil.info("IO", "Caught the NullPointerException");
							}
						}
					}
				} else {
					rowSet = getBinderData(id, true);
					rowSetGlobal = rowSet;

				}

				FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
				FormRowSet results = new FormRowSet();
				results.setMultiRow(true);
				String condition = " WHERE c_id_master = ? ";
				Object[] conditionParams = new Object[] { this.formData.getPrimaryKeyValue() };

				results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(), condition, conditionParams, null, false, null, null);

				if (validForm) {
					cachedRowSet.put(formData, results);
				}

			}
		}
		for (FormRow rowG : rowSetGlobal) {

			for (FormRow row : cachedRowSet.get(formData)) {

				if (rowG.getId().equals(row.getId())) {

					String path = FileUtil.getUploadPath("", row.getId());

					row.setTempFilePathMap(rowG.getTempFilePathMap());
					row.setDeleteFilePathMap(rowG.getDeleteFilePathMap());

				}

			}

		}

		return cachedRowSet.get(formData);
	}

	protected FormRow getSubmittedData(String uv, String prefix, String paramName, FormRow rowData, FormData formData) {
		FormData rowFormData = new FormData();
		LogUtil.info("sd id", formData.getPrimaryKeyValue());
		for (String key : formData.getRequestParams().keySet()) {
			if (key.startsWith(uv) || key.contains("_" + uv + "_")) {
				rowFormData.addRequestParameterValues(key, formData.getRequestParameterValues(key));
			}
		}

		Form form = getEditableForm(uv);
		setOptionBinderData(form.getPropertyString(FormUtil.PROPERTY_ID), form, form, rowFormData);

		// set laod bidner data
		if (rowData != null) {
			FormLoadBinder loadBinder = form.getLoadBinder();
			FormRowSet rowSet = new FormRowSet();
			rowSet.add(rowData);
			rowFormData.setLoadBinderData(loadBinder, rowSet);
		}
		FormUtil.executeElementFormatDataForValidation(form, rowFormData);
		FormUtil.executeElementFormatData(form, rowFormData);
		formDatas.put(uv, rowFormData);

		FormStoreBinder storeBinder = form.getStoreBinder();
		FormRowSet submitedRows = rowFormData.getStoreBinderData(storeBinder);

		if (submitedRows != null && !submitedRows.isEmpty()) {
			return submitedRows.get(0);
		} else {
			return new FormRow();
		}
	}

	protected FormRowSet getBinderData(String id, boolean sort) {
		FormRowSet rowSet = null;

		// read from 'value' property
		String json = getPropertyString(FormUtil.PROPERTY_VALUE);

		try {
			rowSet = parseFormRowSetFromJson(json);
		} catch (Exception ex) {
			LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
		}

		// load from binder if available
		FormRowSet binderRowSet = formData.getLoadBinderData(this);
		if (binderRowSet != null) {
			if (!binderRowSet.isMultiRow()) {
				// parse from String
				if (!binderRowSet.isEmpty()) {
					FormRow row = binderRowSet.get(0);
					String jsonValue = row.getProperty(id);
					LogUtil.info("json", jsonValue);
					try {
						rowSet = parseFormRowSetFromJson(jsonValue);
					} catch (Exception ex) {
						LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
					}
				}
			} else {
				rowSet = binderRowSet;
			}
		}

		if (sort && rowSet != null && getPropertyString("enableSorting") != null
				&& getPropertyString("enableSorting").equals("true") && getPropertyString("sortField") != null
				&& !getPropertyString("sortField").isEmpty()) {
			// sort data by a field
			final String sortField = getPropertyString("sortField");
			Collections.sort(rowSet, new Comparator<FormRow>() {
				public int compare(FormRow row1, FormRow row2) {
					String number1 = row1.getProperty(sortField);
					String number2 = row2.getProperty(sortField);

					if (number1 != null && number2 != null) {
						try {
							return Integer.parseInt(number1) - Integer.parseInt(number2);
						} catch (Exception e) {
							// ignore
						}
					}
					return 0;
				}
			});
		}

		for (FormRow r : rowSet) {
			existing.put(r.getId(), r);
		}
		// return rowSet;

		AppDefinition appDef = AppUtil.getCurrentAppDefinition();
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext()
				.getBean("formDefinitionDao");
		FormDefinition formDef = formDefinitionDao.loadById(getPropertyString("formDefId"), appDef);
		FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
		FormRowSet results = new FormRowSet();
		results.setMultiRow(true);
		String condition = null;
		Object[] conditionParams = new Object[1];
		conditionParams[0] = this.formData.getPrimaryKeyValue();
		LogUtil.info("getSD id", this.formData.getPrimaryKeyValue());

		String labelColumn = (String) getProperty("labelColumn");
		condition = " WHERE c_id_master = ? ";
		results = formDataDao.find(getPropertyString("formDefId"), formDef.getTableName(), condition, conditionParams,
				labelColumn, false, null, null);

		return results;
	}

	protected Form getEditableForm(String uniqueValue) {
		Form editableForm = forms.get(uniqueValue);
		if (editableForm == null) {
			editableForm = createForm(uniqueValue, getPropertyString("formDefId"), false);
			forms.put(uniqueValue, editableForm);
		}
		return editableForm;
	}

	protected Form getReadonlyForm(String uniqueValue) {
		Form readonlyForm = forms.get(uniqueValue);
		if (readonlyForm == null) {
			String formDefId = getPropertyString("editFormDefId");
			if (formDefId.isEmpty()) {
				formDefId = getPropertyString("formDefId");
			}
			readonlyForm = createForm(uniqueValue, formDefId, true);
			forms.put(uniqueValue, readonlyForm);
		}
		return readonlyForm;
	}

	protected Form createForm(String uniqueValue, String formDefId, boolean readonly) {
		Form form = null;
		AppDefinition appDef = AppUtil.getCurrentAppDefinition();
		FormService formService = (FormService) FormUtil.getApplicationContext().getBean("formService");
		FormDefinitionDao formDefinitionDao = (FormDefinitionDao) FormUtil.getApplicationContext()
				.getBean("formDefinitionDao");

		FormDefinition formDef = formDefs.get(formDefId);
		if (formDef == null) {
			formDef = formDefinitionDao.loadById(formDefId, appDef);
			formDefs.put(formDefId, formDef);
		}
		if (formDef != null) {
			String json = formDef.getJson();
			WorkflowAssignment wfAssignment = null;
			if (this.formData != null && this.formData.getProcessId() != null
					&& !this.formData.getProcessId().isEmpty()) {
				formData.setProcessId(this.formData.getProcessId());
				WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
				wfAssignment = wm.getAssignmentByProcess(this.formData.getProcessId());

			}
			json = AppUtil.processHashVariable(json, wfAssignment, StringUtil.TYPE_JSON, null);

			// use the json definition to create the subform
			try {
				form = (Form) formService.createElementFromJson(json);
				form.setParent(this);

				// if id field not exist, automatically add an id hidden field
				Element idElement = FormUtil.findElement(FormUtil.PROPERTY_ID, form, formData);
				if (idElement == null) {
					Collection<Element> subFormElements = form.getChildren();
					idElement = new HiddenField();
					idElement.setProperty(FormUtil.PROPERTY_ID, FormUtil.PROPERTY_ID);
					idElement.setParent(form);
					subFormElements.add(idElement);
				}

				FormData tempFormData = new FormData();
				loadOptionBinders(formDefId, form, form, tempFormData);

				// recursively update parameter names for child elements
				String parentId = uniqueValue + "_" + FormUtil.getElementParameterName(this);
				updateElement(form, parentId, readonly);
			} catch (Exception e) {
				LogUtil.error(AbstractSubForm.class.getName(), e, null);
			}
		}

		return form;
	}

	protected void updateElement(Element element, String prefix, boolean readonly) {
		if (prefix == null) {
			prefix = "";
		} else {
			String paramName = prefix;
			if (element.getParent() != this) {
				paramName += "_" + element.getPropertyString(FormUtil.PROPERTY_ID);
			}
			element.setCustomParameterName(paramName);
		}

		if (element.getParent() != this && (element instanceof Form || element instanceof AbstractSubForm)) {
			String formId = element.getPropertyString(FormUtil.PROPERTY_ID);
			if (formId == null) {
				formId = "";
			}
			prefix += "_" + formId;
		}

		boolean readonlyLabel = Boolean.parseBoolean(getPropertyString(FormUtil.PROPERTY_READONLY_LABEL));
		Collection<Element> children = element.getChildren();
		for (Element child : children) {
			if (readonly) {
				child.setProperty(FormUtil.PROPERTY_READONLY, "true");
			}
			if (readonlyLabel) {
				child.setProperty(FormUtil.PROPERTY_READONLY_LABEL, "true");
			}
			updateElement(child, prefix, readonly);
		}
	}

	protected void setOptionBinderData(String formDefId, Form form, Element element, FormData formData) {
		Map<String, FormRowSet> dataSet = optionBinderData.get(formDefId);
		if (dataSet != null && !dataSet.isEmpty()) {
			FormLoadBinder binder = (FormLoadBinder) element.getOptionsBinder();
			if (binder != null && !FormUtil.isAjaxOptionsSupported(element, formData)) {
				String key = element.getPropertyString(FormUtil.PROPERTY_ID);
				if (element.getParent() != null) {
					key = element.getParent().getPropertyString(FormUtil.PROPERTY_ID) + "_" + key;
				}

				FormRowSet rows = dataSet.get(key);
				if (rows != null) {
					formData.setOptionsBinderData(binder, rows);
				}
			}
			Collection<Element> children = element.getChildren(formData);
			if (children != null) {
				for (Element child : children) {
					setOptionBinderData(formDefId, form, child, formData);
				}
			}
		}
	}

	public void loadOptionBinders(String formDefId, Form form, Element element, FormData formData) {
		FormLoadBinder binder = (FormLoadBinder) element.getOptionsBinder();
		if (binder != null && !FormUtil.isAjaxOptionsSupported(element, formData)) {
			String primaryKeyValue = (formData != null) ? element.getPrimaryKeyValue(formData) : null;
			FormRowSet data = binder.load(element, primaryKeyValue, formData);
			if (data != null) {
				Map<String, FormRowSet> dataSet = optionBinderData.get(formDefId);
				if (dataSet == null) {
					dataSet = new HashMap<String, FormRowSet>();
					optionBinderData.put(formDefId, dataSet);
				}

				String key = element.getPropertyString(FormUtil.PROPERTY_ID);
				if (element.getParent() != null) {
					key = element.getParent().getPropertyString(FormUtil.PROPERTY_ID) + "_" + key;
				}

				dataSet.put(key, data);
			}
		}
		Collection<Element> children = element.getChildren(formData);
		if (children != null) {
			for (Element child : children) {
				loadOptionBinders(formDefId, form, child, formData);
			}
		}
	}

	public String getRowTemplate(Map rowMap, String elementParamName, String mode) {
		FormRow row = null;
		if (rowMap != null) {
			row = new FormRow();
			row.putAll(rowMap);
		}

		String uniqueValue = "uv" + Long.toString(System.currentTimeMillis());

		if (row != null && row.getProperty("id") != null) {
			uniqueValue = row.getProperty("id");
		} else if ("oneTop".equals(mode) || "oneBottom".equals(mode)) {
			uniqueValue = mode;
		}

		// Skip the form on top or bottom to be rendered in table body
		if (("oneTop".equals(uniqueValue) && !"oneTop".equals(mode))
				|| ("oneBottom".equals(uniqueValue) && !"oneBottom".equals(mode))) {
			return "";
		}

		String rId = null;
		if (row != null && row.getId() != null) {
			rId = row.getId();
		}

		Form form = null;
		String readonlyCss = "";
		if (FormUtil.isReadonly(this, formData) || ("disable".equals(getPropertyString("editMode")) && rId != null
				&& !rId.isEmpty() && existing.containsKey(rId))) {
			form = getReadonlyForm(uniqueValue);
			readonlyCss = " readonly";
		} else {
			form = getEditableForm(uniqueValue);
		}

		if (form == null) {
			return "";
		}

		String cssClass = "grid-row " + mode;
		if (row == null || row.getId() == null) {
			cssClass += " new";
		}
		String html = "<tr class=\"" + cssClass + "\">";

		if (!FormUtil.isReadonly(this, formData) && getPropertyString("enableSorting") != null
				&& getPropertyString("enableSorting").equals("true") && getPropertyString("sortField") != null
				&& !getPropertyString("sortField").isEmpty()) {
			if (("oneTop".equals(getPropertyString("addMode")) || "oneBottom".equals(getPropertyString("addMode")))
					&& "disable".equals(getPropertyString("editMode"))) {
				// ignore
			} else if (!("oneTop".equals(mode) || "oneBottom".equals(mode))) {
				html += "<td class=\"order\"><a title=\""
						+ AppPluginUtil.getMessage("form.subformRepeater.sort", getClassName(), MESSAGE_PATH)
						+ "\"><span ></span></a></td>";
			} else {
				html += "<td></td>";
			}
		}
		html += "<td class=\"subform_wrapper\">";
		html += "<input type=\"hidden\" class=\"unique_value\" name=\"" + elementParamName + "_unique_value\" value=\""
				+ uniqueValue + "\" />";

		FormData rowFormData = formDatas.get(uniqueValue);
		if (rowFormData == null) {
			rowFormData = new FormData();
			setOptionBinderData(form.getPropertyString(FormUtil.PROPERTY_ID), form, form, rowFormData);
			if (row != null) {
				// set load binder data
				FormLoadBinder loadBinder = form.getLoadBinder();
				FormRowSet rowSet = new FormRowSet();
				rowSet.add(row);
				rowFormData.setLoadBinderData(loadBinder, rowSet);
			}
		}

		// Load data for child
		rowFormData.setPrimaryKeyValue(rId);
		Collection<Element> children = form.getChildren(rowFormData);
		if (children != null) {
			for (Element child : children) {
				FormUtil.executeLoadBinders(child, rowFormData);
			}
		}

		// Get form template
		html += "<div class=\"subform-container no-frame" + readonlyCss + "\">";

		String formHtml = form.render(rowFormData, false);
		formHtml = formHtml.replaceAll("\"form-section", "\"subform-section");
		formHtml = formHtml.replaceAll("\"form-column", "\"subform-column");
		formHtml = formHtml.replaceAll("\"form-cell", "\"subform-cell");

		// Fix form Hash Variable not working
		if (rId != null && !rId.isEmpty() && formHtml.contains("{recordId}")) {
			formHtml = formHtml.replaceAll(StringUtil.escapeRegex("{recordId}"), rId);
			formHtml = AppUtil.processHashVariable(formHtml, null, null, null);
		}

		Document doc = Jsoup.parse(formHtml);

		String m;

		for (org.jsoup.nodes.Element div : doc.select("div.subform-column").select("div.subform-cell")) {
			m = this.validateMultiRow(div.toString(), rowFormData.getPrimaryKeyValue());

			if (!m.isEmpty()) {
				div.after("<span class=\"form-error-message\" style=\"display: flex; justify-content: center;\">" + m
						+ "</span>" + "</div></td>\n");
			}
		}

		formHtml = doc.html();

		formHtml += "<script>$(document).ready(function() { $('input[type=\"file\"]').fileUploadField(); });</script>";

		html += formHtml;

		html += "</div>";
		html += "</td>";

		if ("enable".equals(getPropertyString("deleteMode")) || "enable".equals(getPropertyString("addMode"))
				|| "true".equals(getPropertyString("collapsible"))) {
			html += "<td class=\"repeater-action\">";
			if (!("oneTop".equals(mode) || "oneBottom".equals(mode))) {
				if (!FormUtil.isReadonly(this, formData) && "enable".equals(getPropertyString("addMode"))) {
					html += "<a class=\"repeater-action-add add-row-before\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.add", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>";
				}
				if ("true".equals(getPropertyString("collapsible"))) {
					html += "<a class=\"repeater-collapsible\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.collapse", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>";
				}
				if (readonlyCss.isEmpty() && "enable".equals(getPropertyString("deleteMode"))) {
					html += "<a class=\"repeater-action-delete\" title=\""
							+ AppPluginUtil.getMessage("form.subformRepeater.delete", getClassName(), MESSAGE_PATH)
							+ "\"><span></span></a>";
				}
			}
			html += "</td>";
		}
		html += "</tr>";

		if ("Header".equals(mode)) {
			Document docHeader = Jsoup.parse(html);
			Elements headers = docHeader.select("label");
			String headerHtml = "";
			for (org.jsoup.nodes.Element header : headers) {
				headerHtml += "<th>" + header.text() + "</th>\n";
			}

			if (!"true".equals(getPropertyString("readonly"))) {
				headerHtml += "<th>Actions</th>\n";
			}

			return headerHtml;
		} else if ("body".equals(mode)) {
			Document docBody = Jsoup.parse(html);
			Elements divs = docBody.select("div.subform-column").select("div.subform-cell");
			String bodyHtml = "<tr class=\"" + cssClass + "\"><div class=\"subform-container no-frame\">"
					+ docBody.select("input[type=hidden]").toString();

			bodyHtml += "<td style=\"display:none\" class=\"subform_wrapper\">";
			bodyHtml += "<input type=\"hidden\" class=\"unique_value\" name=\"" + elementParamName
					+ "_unique_value\" value=\"" + uniqueValue + "\" />";

			String validationMessage;

			for (org.jsoup.nodes.Element div : divs) {
				div.select("label").remove();
				div.attr("style", "display: flex;flex-direction: row;justify-content: center;align-content: center;");

				validationMessage = this.validateMultiRow(div.toString(), rowFormData.getPrimaryKeyValue());

				if (validationMessage.isEmpty()) {
					bodyHtml += "<td><div>" + docBody.select("div.subform-container").select("input[type=hidden]").toString()
							+ div + "<div></td>\n";
				} else {
					bodyHtml += "<td><div>" + docBody.select("div.subform-container").select("input[type=hidden]").toString()
							+ div
							+ "<span class=\"form-error-message\" style=\"display: flex; justify-content: center;\">"
							+ validationMessage + "</span>" + "</div></td>\n";
				}
			}

			if (!"true".equals(getPropertyString("readonly"))) {
				bodyHtml += "<td class=\"repeater-action\">" + docBody.select("a.repeater-action-delete").toString()
						+ "</td>\n </div></tr>";
			}
			return bodyHtml;
		}

		return html;
	}


	public String validateMultiRow(String divs, String pk) {

		String ch = "";
		String uv;
		String field;

		for (Map.Entry<String, String> map : erreurElemnt.entrySet()) {

			int pos = map.getKey().indexOf("_");

			uv = (String) map.getKey().subSequence(0, pos);
			field = map.getKey().substring(pos);

			if (uv.equals(pk) && divs.contains(field)) {

				return map.getValue();

			}

		}

		return ch;

	}

	/**
	 * Multirow validator
	 */
	@Override
	public Boolean selfValidate(FormData formData) {

		this.formData = formData;
		Boolean valid = super.selfValidate(formData);


		// run validation for all editable row
		boolean rowsValid = true;
		FormRowSet rowSet = getRows(formData);

		for (FormRow r : rowSet) {

			// if have formData mean it is editable
			String uv = r.getProperty("id");

			// get form data
			FormData rowFormData = formDatas.get(uv);

			LogUtil.info(" in methode self validate row form data uv", uv);

			if (rowFormData != null) {

				Form form = getEditableForm(uv);
				FormUtil.executeValidators(form, rowFormData);

				//LogUtil.info(" in methode self validate row form data uv", uv);

				if (form.hasError(rowFormData)) {

					LogUtil.info("     form.hasError(rowFormData)     ", "");

					rowsValid = false;


					// add error row validator to form data
					for (Map.Entry<String, String> map : rowFormData.getFormErrors().entrySet()) {

						int pos = map.getKey().indexOf("_");

						String idElement = uv + map.getKey().substring(pos);

						if (erreurElemnt.containsKey(idElement)) {

							erreurElemnt.replace(idElement, map.getValue());
						} else {

							erreurElemnt.put(idElement, map.getValue());
						}

					}

				}
			}
		}
		if (!rowsValid) {

			valid = false;
			String id = FormUtil.getElementParameterName(this);
			String errorMsg = AppPluginUtil.getMessage("form.subformRepeater.error.rowData", getClassName(),
					MESSAGE_PATH);
			formData.addFormError(id, errorMsg);
			// LogUtil.info("---->Error Multirow ", errorMsg);

			validForm=false;

		}



		return valid;
	}

	protected String getDecorator() {
		String decorator = "";

		try {
			String min = getPropertyString("validateMinRow");

			if ((min != null && !min.isEmpty())) {
				int minNumber = Integer.parseInt(min);
				if (minNumber > 0) {
					decorator = "*";
				}
			}
		} catch (Exception e) {
		}

		return decorator;
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

	public void storeInnerData(FormRowSet rows) {
		FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");

		for (FormRow r : rows) {
			// if have formData mean it is editable
			String uv = r.getProperty("RS_UNIQUE_VALUE");
			FormData rowFormData = formDatas.get(uv);
			if (rowFormData != null && rowFormData.getStoreBinders().size() > 1) {
				Form form = getEditableForm(uv);

				if (rowFormData.getPrimaryKeyValue() == null || rowFormData.getPrimaryKeyValue().isEmpty()) {
					if (r.getId() == null || r.getId().isEmpty()) {
						r.setId(UuidGenerator.getInstance().getUuid());
					}
					rowFormData.setPrimaryKeyValue(r.getId());
				}

				// skip the form elemnt and start with its child
				for (Element e : form.getChildren()) {
					formService.recursiveExecuteFormStoreBinders(form, e, rowFormData);
				}
			}
			// remove uv
			r.remove("RS_UNIQUE_VALUE");
		}
	}

	public void executeFormActionForDefaultAddForm(FormData formData) {
		if ("oneTop".equals(getPropertyString("addMode")) || "oneBottom".equals(getPropertyString("addMode")) ) {
			if ("true".equals(getPropertyString("setWorkflowVariable"))
					|| "true".equals(getPropertyString("runPostProcessing"))) {
				Form form = getEditableForm(getPropertyString("addMode"));
				FormStoreBinder storeBinder = form.getStoreBinder();
				FormData rowFormData = formDatas.get(getPropertyString("addMode"));
				FormRowSet submitedRows = rowFormData.getStoreBinderData(storeBinder);
				FormRow submitedRow = submitedRows.get(0);
				String id = submitedRow.getId();
				rowFormData.setPrimaryKeyValue(id);

				// set workflow variable
				if ("true".equals(getPropertyString("setWorkflowVariable"))) {
					String activityId = formData.getActivityId();
					String processId = formData.getProcessId();
					if (activityId != null || processId != null) {

						Map<String, String> variableMap = new HashMap<String, String>();
						variableMap = storeWorkflowVariables(getEditableForm(getPropertyString("addMode")), submitedRow,
								variableMap);

						if (!variableMap.isEmpty()) {
							WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext()
									.getBean("workflowManager");
							if (activityId != null) {
								workflowManager.activityVariables(activityId, variableMap);
							} else {
								workflowManager.processVariables(processId, variableMap);
							}
						}
					}
				}

				// execute form post processing
				if ("true".equals(getPropertyString("runPostProcessing"))) {
					FormUtil.executePostFormSubmissionProccessor(form, rowFormData);
				}
			}
		}
	}

	protected Map<String, String> storeWorkflowVariables(Element element, FormRow row,
														 Map<String, String> variableMap) {
		String variableName = element.getPropertyString(AppUtil.PROPERTY_WORKFLOW_VARIABLE);
		if (variableName != null && !variableName.trim().isEmpty()) {
			String id = element.getPropertyString(FormUtil.PROPERTY_ID);
			String value = (String) row.get(id);
			if (value != null) {
				variableMap.put(variableName, value);
			}
		}
		for (Iterator<Element> i = element.getChildren().iterator(); i.hasNext();) {
			Element child = i.next();
			storeWorkflowVariables(child, row, variableMap);
		}
		return variableMap;
	}

	public String getServiceUrl() {
		if ("enable".equals(getPropertyString("addMode"))) {
			String url = WorkflowUtil.getHttpServletRequest().getContextPath()
					+ "/web/json/plugin/org.joget.SubformRepeater/service";
			AppDefinition appDef = AppUtil.getCurrentAppDefinition();

			// create nonce
			String paramName = FormUtil.getElementParameterName(this);
			String nonce = SecurityUtil.generateNonce(
					new String[] { "SubformRepeater", appDef.getAppId(), appDef.getVersion().toString(), paramName },
					1);
			try {
				url = url + "?_nonce=" + URLEncoder.encode(nonce, "UTF-8") + "&_paramName="
						+ URLEncoder.encode(paramName, "UTF-8") + "&_appId="
						+ URLEncoder.encode(appDef.getAppId(), "UTF-8") + "&_appVersion="
						+ URLEncoder.encode(appDef.getVersion().toString(), "UTF-8");
				url += "&_enableSorting=" + URLEncoder.encode(getPropertyString("enableSorting"), "UTF-8");
				url += "&_sortField=" + URLEncoder.encode(getPropertyString("sortField"), "UTF-8");
				url += "&_formDefId=" + URLEncoder.encode(getPropertyString("formDefId"), "UTF-8");
				url += "&_deleteMode=" + URLEncoder.encode(getPropertyString("deleteMode"), "UTF-8");
				url += "&_addMode=" + URLEncoder.encode(getPropertyString("addMode"), "UTF-8");
				url += "&_collapsible=" + URLEncoder.encode(getPropertyString("collapsible"), "UTF-8");
				url += "&_processId=" + URLEncoder
						.encode((this.formData.getProcessId() != null ? this.formData.getProcessId() : ""), "UTF-8");
			} catch (Exception e) {
			}
			return url;
		} else {
			return "";
		}
	}

	public void webService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String nonce = request.getParameter("_nonce");
		String paramName = request.getParameter("_paramName");
		String appId = request.getParameter("_appId");
		String appVersion = request.getParameter("_appVersion");

		if (SecurityUtil.verifyNonce(nonce, new String[] { "SubformRepeater", appId, appVersion, paramName })) {
			if ("POST".equalsIgnoreCase(request.getMethod())) {
				try {
					AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
					AppDefinition appDef = appService.getAppDefinition(appId, appVersion);

					setProperty("enableSorting", request.getParameter("_enableSorting"));
					setProperty("sortField", request.getParameter("_sortField"));
					setProperty("formDefId", request.getParameter("_formDefId"));
					setProperty("deleteMode", request.getParameter("_deleteMode"));
					setProperty("addMode", request.getParameter("_addMode"));
					setProperty("collapsible", request.getParameter("_collapsible"));

					setCustomParameterName(paramName);

					this.formData = new FormData();
					this.formData.setProcessId(request.getParameter("_processId"));

					String template = getRowTemplate(null, paramName, "body");
					System.out.println("template = " + template.length());
					if (template != null && !template.isEmpty()) {
						// return html
						response.setContentType("text/html");
						PrintWriter writer = response.getWriter();
						writer.write(template);
					} else {
						response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					}
				} catch (Exception ex) {
				}
			} else {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}
}
