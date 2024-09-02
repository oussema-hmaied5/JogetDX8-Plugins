package org.joget.marketplace;

import de.bripkens.gravatar.DefaultImage;
import de.bripkens.gravatar.Gravatar;
import de.bripkens.gravatar.Rating;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.lib.AjaxUniversalTheme;
import org.joget.apps.userview.model.UserviewCategory;
import org.joget.apps.userview.model.UserviewPwaTheme;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.TimeZoneUtil;
import org.joget.directory.model.User;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class EssenceTheme extends AjaxUniversalTheme{
     private final static String MESSAGE_PATH = "message/essenceTheme";
     
    @Override
    public String getName() {
        return "Essence Theme";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getLabel() {
        return getName();
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }
    
    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/essenceTheme.json", null, true, MESSAGE_PATH);
    }
    
    @Override
    public String getJsCssLib(Map<String, Object> data) {
        if (isAjaxContent(data)) {
            return "";
        }

        String jsCssLink = super.getJsCssLib(data);
        if (isGoogleFontAPIValid()) {
            String googleFont = getPropertyString("googleFont");
            jsCssLink += "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n";
            jsCssLink += "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\">\n";
            jsCssLink += "<link rel=\"stylesheet\" href=\"" + data.get("context_path") + "/plugin/" + getClassName() + "/DX8EssenceTheme.css\"></link>\n";
            jsCssLink += "<link href=\"" + getGoogleFontLink(googleFont.replace(" ", "+")) + "\" rel=\"stylesheet\">\n";
            jsCssLink += "<style type=\"text/css\">body, .ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button, div.pq-grid * {\n"
                    + "    font-family: \"" + googleFont + "\", sans-serif !important;\n"
                    + "}</style>";
        } else {
            //use back local font
            jsCssLink += "<link rel=\"stylesheet\" href=\"" + data.get("context_path") + "/plugin/" + getClassName() + "/DX8EssenceTheme.css\"></link>\n";
            jsCssLink += "<link rel=\"stylesheet\" href=\"" + data.get("context_path") + "/plugin/" + getClassName() + "/font/Inter/css/font.css\"/>\n";
            jsCssLink += "<style type=\"text/css\">body, .ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button, div.pq-grid * {\n"
                    + "    font-family: \"Inter\", sans-serif !important;\n"
                    + "}</style>";
        }
        jsCssLink += "<script>"
                + "var theme = '" + getPropertyString("horizontal_menu") + "';"
                + "</script>";
        jsCssLink += "<script src=\"" + data.get("context_path") + "/plugin/" + getClassName() + "/DX8EssenceTheme.js\" defer></script>\n";
        jsCssLink += "<style>" + generateLessCss() + "</style>";
        return jsCssLink;
    }
    
    @Override
    public Set<String> getOfflineStaticResources() {
        Set<String> urls = super.getOfflineStaticResources();
        String contextPath = AppUtil.getRequestContextPath();
        urls.add(contextPath + "/plugin/" + getClassName() + "/DX8EssenceTheme.css");
        return urls;
    }
    
    @Override
    protected String getBreadcrumb(Map<String, Object> data) {
        if (data.containsKey("offlineTemplate")) {
            return "";
        } else {
            String breadcrumb = "<ul class=\"breadcrumb\"><li><a href=\"" + data.get("home_page_link") + "\">" + ResourceBundleUtil.getMessage("theme.universal.home") + "</a> <i class=\"zmdi zmdi-minus\"></i> </li> ";
            if ((Boolean) data.get("is_login_page") || (Boolean) data.get("embed")) {
                return "";
            } else if (userview.getCurrent() != null) {
                boolean categoryExist = false;
                UserviewCategory category = userview.getCurrentCategory();
                if (!(category.getMenus().size() <= 1 && ((Boolean) data.get("combine_single_menu_category"))) && !"yes".equals(category.getPropertyString("hide"))) {
                    breadcrumb += "<li><a href=\"" + getCategoryLink(category, data) + "\">" + StringUtil.stripAllHtmlTag(category.getPropertyString("label")) + "</a> </li>";
                    categoryExist = true;
                }
                if (categoryExist) {
                    breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-1\">"
                        + StringUtil.stripAllHtmlTag(userview.getCurrent().getPropertyString("label")) + "</h1>" + breadcrumb;
                    breadcrumb += "</ul>";
                } else {
                    breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-3\">"
                        + StringUtil.stripAllHtmlTag(userview.getCurrent().getPropertyString("label")) + "</h1>";
                }
            } else if (PROFILE.equals(userview.getParamString("menuId"))) {
                breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-3\">"
                        + ResourceBundleUtil.getMessage("theme.universal.profile") + "</h1>";
            } else if (INBOX.equals(userview.getParamString("menuId"))) {
                breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-3\">"
                        + ResourceBundleUtil.getMessage("theme.universal.inbox") + "</h1>";
            } else if (UserviewPwaTheme.PWA_OFFLINE_MENU_ID.equals(userview.getParamString("menuId")) || UserviewPwaTheme.PAGE_UNAVAILABLE_MENU_ID.equals(userview.getParamString("menuId"))) {
                breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-3\">"
                        + ResourceBundleUtil.getMessage("pwa.offline.breadcrumbTitle") + "</h1>";
            } else {
                breadcrumb = "<h1 class=\"page-heading d-flex text-gray-900 fw-bold fs-3 flex-column justify-content-center my-3\">"
                        + ResourceBundleUtil.getMessage("ubuilder.pageNotFound") + "</h1>";
            }
            return breadcrumb;
        }
    }
    @Override
    public String getHeader(Map<String, Object> data) {
        String headerHtml = "";

        // Call the superclass method to get the existing header HTML
        headerHtml += super.getHeader(data);

        // Add search form HTML near Contact Us
        headerHtml += "<div class=\"header-search\" style=\"display: inline-block; margin-left: auto;\">";
        headerHtml += "<form action=\"http://localhost:8067/jw/web/userview/Platform/home/_/plugins\" method=\"get\" style=\"display: flex; align-items: center;\">";
        headerHtml += "    <input type=\"text\" id=\"app_name\" name=\"d-2556228-fn_plugin_name\" placeholder=\"Search...\" style=\"margin-right: 10px; padding: 5px; border-radius: 5px; border: 1px solid #ccc;\" />";
        headerHtml += "    <button type=\"submit\" class=\"btn btn-primary\" style=\"background-color: #007bff; border: none; padding: 5px 10px; border-radius: 5px;\">Search</button>";
        headerHtml += "</form>";
        headerHtml += "</div>";

        return headerHtml;
    }


    @Override
    protected String getUserMenu(Map<String, Object> data) {
        String html = "";
        if ((Boolean) data.get("is_logged_in")) {
            User user = (User) data.get("user");
            String email = user.getEmail();
            if (email == null) {
                email = "";
            }
            if (email.contains(";")) {
                email = email.split(";")[0];
            }
            if (email.contains(",")) {
                email = email.split(",")[0];
            }

            String profileImageTag = "";
            if (getPropertyString("userImage").isEmpty()) {
                String url = (email != null && !email.isEmpty())
                        ? new Gravatar()
                                .setSize(40)
                                .setHttps(true)
                                .setRating(Rating.PARENTAL_GUIDANCE_SUGGESTED)
                                .setStandardDefaultImage(DefaultImage.IDENTICON)
                                .getUrl(email)
                        : "//www.gravatar.com/avatar/default?d=identicon";
                profileImageTag = "<img class=\"gravatar\" alt=\"gravatar\" width=\"40\" height=\"40\" data-lazysrc=\"" + url + "\" onError=\"this.onerror = '';this.style.display='none';\"/> ";
            } else if ("hashVariable".equals(getPropertyString("userImage"))) {
                String url = AppUtil.processHashVariable(getPropertyString("userImageUrlHash"), null, StringUtil.TYPE_HTML, null, AppUtil.getCurrentAppDefinition());
                if (AppUtil.containsHashVariable(url) || url == null || url.isEmpty()) {
                    url = data.get("context_path") + "/" + getPathName() + "/user.png";
                }
                profileImageTag = "<img alt=\"profile\" width=\"40\" height=\"40\" src=\"" + url + "\" /> ";
            }

            html += "<li class=\"user-link dropdown\">\n"
                    + "    <a data-toggle=\"dropdown\" href=\"javascript:;\" class=\"btn dropdown-toggle type-" + getPropertyString("userImage") + "\">\n"
                    + "	     " + profileImageTag + "\n"
                    + "	     <span class=\"caret\"></span>\n"
                    + "    </a>\n";

            html += "<ul class=\"dropdown-menu\">\n";
            if (!"true".equals(getPropertyString("profile")) && !user.getReadonly()) {
                html += "<div class=\"profile-item px-3\">\n"
                        + "        <div class=\"menu-content d-flex align-items-center\">\n"
                        + "            <!--begin::Avatar-->\n"
                        + "            <div class=\"symbol symbol-50px me-5\">\n"
                        +               profileImageTag
                        + "            </div>\n"
                        + "            <!--end::Avatar-->\n"
                        + "\n"
                        + "            <!--begin::Username-->\n"
                        + "            <div class=\"d-flex flex-column\">\n"
                        + "                <div class=\"profile-name fw-bold d-flex align-items-center fs-5\">\n"
                        + "                    "+user.getFirstName()+user.getLastName()
                        + "                </div>\n"
                        + "\n"
                        + "                <a href=\"#\" class=\"profile-email fw-semibold text-muted text-hover-primary fs-7\">\n"
                        + "                    " + email + "                </a>\n"
                        + "            </div>\n"
                        + "            <!--end::Username-->\n"
                        + "        </div>\n"
                        + "    </div>";
                html += "    <li><a href=\"" + data.get("base_link") + PROFILE + "\">" + ResourceBundleUtil.getMessage("theme.universal.profile") + "</a></li>\n";
            }

            Object[] shortcut = (Object[]) getProperty("userMenu");
            if (shortcut != null && shortcut.length > 0) {
                for (Object o : shortcut) {
                    Map link = (HashMap) o;
                    String href = link.get("href").toString();
                    String label = link.get("label").toString();
                    String target = (link.get("target") == null) ? "" : link.get("target").toString();

                    if ("divider".equalsIgnoreCase(label)) {
                        html += "<li class=\"divider\"></li>\n";
                    } else if (href.isEmpty()) {
                        html += "<li class=\"dropdown-menu-title\"><span>" + label + "</span></li>\n";
                    } else {
                        if (!href.contains("/")) {
                            href = data.get("base_link") + href;
                        }
                        html += "<li><a href=\"" + href + "\" target=\"" + target + "\">" + label + "</a></li>\n";
                    }
                }
            }

            html += "    <li><a href=\"" + data.get("logout_link") + "\">" + ResourceBundleUtil.getMessage("theme.universal.logout") + "</a></li>\n"
                    + "</ul>";

        } else {
            html += "<li class=\"user-link\">\n"
                    + "    <a href=\"" + data.get("login_link") + "\" class=\"btn\">\n"
                    + "	     <i class=\"fa fa-user white\"></i> " + ResourceBundleUtil.getMessage("ubuilder.login") + "\n"
                    + "    </a>\n";
        }
        html += "</li>";
        return html;
    }

    @Override
    protected String getInbox(Map<String, Object> data) {
        String html = "";
        
        if (!getPropertyString("inbox").isEmpty()) {
            String url = data.get("context_path") + "/web/json/plugin/" + getClassName() + "/service?_a=getAssignment";
            if ("current".equals(getPropertyString("inbox"))) {
                try {
                    url += "&appId=" + URLEncoder.encode(userview.getParamString("appId"), "UTF-8");
                } catch (UnsupportedEncodingException e){}
            }
            html += "<li class=\"inbox-notification dropdown\" data-url=\"" + url + "\">\n"
                  + "    <a data-toggle=\"dropdown\" href=\"javascript:;\" class=\"btn dropdown-toggle\">\n"
                  + "	 <i class=\"far fa-bell\"></i><span class=\"badge red\">0</span>\n"
                  + "    </a>\n"
                  + "    <ul class=\"dropdown-menu notifications\">\n"
                  + "        <li class=\"dropdown-menu-title\"><h3>" + AppPluginUtil.getMessage("theme.essenceTheme.notification", getClassName(), MESSAGE_PATH) + "</h3> <span><strong class=\"count\">0</strong> " + AppPluginUtil.getMessage("theme.essenceTheme.task", getClassName(), MESSAGE_PATH) + "</span><a href=\"#\" class=\"refresh\" title=\"" + ResourceBundleUtil.getMessage("general.method.label.refresh") + "\"><i class=\"fa fa-refresh\"></i></a></li>"
                  + "        <li class=\"loading\"><a><span><i class=\"fa fa-spinner fa-spin fa-3x\"></i></span></a></li>\n"
                  + "        <li class=\"all-assignment\"><a href=\"" + data.get("base_link") + INBOX + "\" class=\"dropdown-menu-sub-footer\">" + ResourceBundleUtil.getMessage("theme.universal.viewAllTask") + "</a></li>\n"  
                  + "    </ul>\n"
                  + "<li>";
        }
        
        return html;
    }
    
    @Override
    public String getLayout(Map<String, Object> data) {
        if ("dark-header".equals(getPropertyString("horizontal_menu")) || "light-header".equals(getPropertyString("horizontal_menu"))) {
            data.put("body_classes", data.get("body_classes").toString() + " horizontal_menu inline_menu");
        } else if ("no".equals(getPropertyString("horizontal_menu"))) {
            data.put("body_classes", data.get("body_classes").toString() + " horizontal_menu no_menu");
        }

        if (getPropertyString("horizontal_menu") != null) {
            data.put("body_classes", data.get("body_classes").toString() + " " + getPropertyString("horizontal_menu"));
        }

        if ("true".equals(getPropertyString("roundedFieldDesign"))) {
            data.put("body_classes", data.get("body_classes").toString() + " rounded-field");
        }

        return super.getLayout(data);
    }

    @Override
    public String getContentContainer(Map<String, Object> data) {
        if (isAjaxContent(data)) {
            String contentInnerBefore = getBreadcrumb(data);
            if (getPropertyString("fontControl").equalsIgnoreCase("true")) {
                contentInnerBefore += getFontSizeController(data);
            }
            data.put("content_inner_before", contentInnerBefore);
            return null;
        } else {
            if (!getPropertyString("horizontal_menu").equalsIgnoreCase("dark-sidebar") && !getPropertyString("horizontal_menu").equalsIgnoreCase("light-sidebar")) {
                data.put("hide_nav", true);
            }

            if (showHomeBanner()) {
                data.put("main_container_before", "<div class=\"home_banner\"><div class=\"home_banner_inner\">" + getPropertyString("homeAttractBanner") + "</div></div>");
            }

            data.put("main_container_classes", "container-fluid-full");
            data.put("main_container_inner_classes", "row-fluid");
            data.put("sidebar_classes", "span2");
            if (((Boolean) data.get("embed")) || ((Boolean) data.get("hide_nav"))) {
                data.put("content_classes", "span12");
            } else {
                data.put("content_classes", "span10");
            }

            String ContentInnerBefore = getBreadcrumb(data);
            if (getPropertyString("fontControl").equalsIgnoreCase("true")) {
                ContentInnerBefore += getFontSizeController(data);
            }
            data.put("content_inner_before", ContentInnerBefore);
            return UserviewUtil.getTemplate(this, data, "/templates/userview/contentContainer.ftl");
        }
    }

    public boolean isGoogleFontAPIValid() {
        boolean useGoogleFonts = !"true".equalsIgnoreCase(getPropertyString("usingLocalFonts"));
        // Check if Google Font usage is enabled.
        if (useGoogleFonts) {
            String googleFont = getPropertyString("googleFont");
            String googleFontLink = getGoogleFontLink(googleFont.replace(" ", "+"));

            if (!googleFontLink.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    // Generate embedded Google Font link with API call.
    public String getGoogleFontLink(String googleFont) {
        String googleFontUrl = "";
        String googleFontAPI = getPropertyString("googleFontAPIKey");
        if (googleFontAPI != null && !googleFontAPI.isEmpty()) {
            try {
                //Url to retrieve the font details 
                URL url = new URL("https://www.googleapis.com/webfonts/v1/webfonts?key=" + googleFontAPI + "&family=" + googleFont);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray items = jsonObject.getJSONArray("items");
                    
                    // Generate the embedded Google Font link
                    List<String> apiUrl = new ArrayList<>();
                    apiUrl.add("https://fonts.googleapis.com/css2?family=");
                    apiUrl.add(googleFont);
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        // Get variants array
                        JSONArray variants = item.getJSONArray("variants");
                        List<String> normalWeight = new ArrayList<>();
                        List<String> italicWeight = new ArrayList<>();
                        boolean addcomma = true;
                        for (int j = 0; j < variants.length(); j++) {
                            String variantString = variants.getString(j);
                            if (variantString.equalsIgnoreCase("italic")) {
                                addcomma = false;
                                apiUrl.add(":");
                                apiUrl.add("ital,");
                                italicWeight.add("400");
                            } else if (variantString.equalsIgnoreCase("greek")) {
                                apiUrl.add("&subset=");
                                apiUrl.add("greek");
                            } else if (variantString.equalsIgnoreCase("regular")) {
                                normalWeight.add("400");
                            } else {
                                if (variantString.contains("italic")) {
                                    italicWeight.add(variantString.replace("italic", ""));
                                } else {
                                    normalWeight.add(variantString);
                                }
                            }
                        }
                        String weight = "wght@";
                        if (addcomma) {
                            weight = ":wght@";
                        }
                        //Modify the value when the italic variant exists: 0 for normal, 1 for italic.
                        if (!italicWeight.isEmpty()) {
                            weight += String.join(";", modifyValues(normalWeight, "0,"));
                            weight += ";" + String.join(";", modifyValues(italicWeight, "1,"));
                        } else {
                            weight += String.join(";", normalWeight);
                        }
                        apiUrl.add(weight);
                    }
                    apiUrl.add("&display=swap");
                    googleFontUrl = String.join("", apiUrl);
                }
            } catch (IOException e) {
                LogUtil.error(getClass().getName(), e, "Failed to generate embed Google Font link. Please ensure that you have entered a valid API Key");
            }
        }
        return googleFontUrl;
    }
    
    public static String[] modifyValues(List<String> list, String extraValue) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, extraValue + list.get(i));
        }
        return list.toArray(new String[0]);
    }
    
    @Override
    protected String generateLessCss() {
        String mode = getPropertyString("horizontal_menu");

        String css = "body." + mode + "{";
        if (!getPropertyString("dx8colorScheme").isEmpty()) {
            String[] colors = getPropertyString("dx8colorScheme").split(";");
            for (int i=0; i < colors.length; i++) {
                if (!colors[i].isEmpty()) {
                    css += "--theme-color"+(i+1)+":"+colors[i]+ ";";
                }
            }
        }
        if (!getPropertyString("dx8backgroundImage").isEmpty()) {
            css += "--theme-background-image:url('"+getPropertyString("dx8backgroundImage")+ "');";
        }
        if (!getPropertyString("dx8background").isEmpty()) {
            css += "--theme-background:"+getPropertyString("dx8background")+ ";";
        }
        if (!getPropertyString("dx8contentbackground").isEmpty()) {
            css += "--theme-content-background:"+getPropertyString("dx8contentbackground")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-headerColor").isEmpty()) {
            css += "--theme-header:"+getPropertyString("dx8-" + mode + "-headerColor")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-headerFontColor").isEmpty()) {
            css += "--theme-header-font:"+getPropertyString("dx8-" + mode + "-headerFontColor")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navBackground").isEmpty()) {
            css += "--theme-sidebar:"+getPropertyString("dx8-" + mode + "-navBackground")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navLinkBackground").isEmpty()) {
            css += "--theme-sidebar-link-bg:"+getPropertyString("dx8-" + mode + "-navLinkBackground")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navLinkColor").isEmpty()) {
            css += "--theme-sidebar-link:"+getPropertyString("dx8-" + mode + "-navLinkColor")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navLinkIcon").isEmpty()) {
            css += "--theme-sidebar-icon:"+getPropertyString("dx8-" + mode + "-navLinkIcon")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navBadge").isEmpty()) {
            css += "--theme-sidebar-badge:"+getPropertyString("dx8-" + mode + "-navBadge")+";";
        }
        if (!getPropertyString("dx8-" + mode + "-navBadgeText").isEmpty()) {
            css += "--theme-sidebar-badge-text:"+getPropertyString("dx8-" + mode + "-navBadgeText")+";";
        }
        if (!getPropertyString("dx8-" + mode + "-navActiveLinkBackground").isEmpty()) {
            css += "--theme-sidebar-active-link-bg:"+getPropertyString("dx8-" + mode + "-navActiveLinkBackground")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navActiveLinkColor").isEmpty()) {
            css += "--theme-sidebar-active-link:"+getPropertyString("dx8-" + mode + "-navActiveLinkColor")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navActiveIconColor").isEmpty()) {
            css += "--theme-sidebar-active-icon:"+getPropertyString("dx8-" + mode + "-navActiveIconColor")+ ";";
        }
        if (!getPropertyString("dx8-" + mode + "-navScrollbarThumb").isEmpty()) {
            css += "--theme-nav-scrollbar-thumb:"+getPropertyString("dx8-" + mode + "-navScrollbarThumb")+ ";";
        }
        if (!getPropertyString("dx8buttonBackground").isEmpty()) {
            css += "--theme-button-bg:"+getPropertyString("dx8buttonBackground")+ ";";
        }
        if (!getPropertyString("dx8buttonColor").isEmpty()) {
            css += "--theme-button:"+getPropertyString("dx8buttonColor")+ ";";
        }
        if (!getPropertyString("dx8primaryColor").isEmpty()) {
            css += "--theme-primary:"+getPropertyString("dx8primaryColor")+ ";";
        }
        if (!getPropertyString("dx8headingBgColor").isEmpty()) {
            css += "--theme-heading-bg:"+getPropertyString("dx8headingBgColor")+ ";";
        }
        if (!getPropertyString("dx8headingFontColor").isEmpty()) {
            css += "--theme-heading-color:"+getPropertyString("dx8headingFontColor")+ ";";
        }
        if (!getPropertyString("dx8fontColor").isEmpty()) {
            css += "--theme-font-color:"+getPropertyString("dx8fontColor")+ ";";
        }
        if (!getPropertyString("dx8contentFontColor").isEmpty()) {
            css += "--theme-content-color:"+getPropertyString("dx8contentFontColor")+ ";";
        }
        if (!getPropertyString("dx8footerBackground").isEmpty()) {
            css += "--theme-footer-bg:"+getPropertyString("dx8footerBackground")+ ";";
        }
        if (!getPropertyString("dx8footerColor").isEmpty()) {
            css += "--theme-footer:"+getPropertyString("dx8footerColor")+ ";";
        }
        if (!getPropertyString("dx8linkColor").isEmpty()) {
            css += "--theme-link:"+getPropertyString("dx8linkColor")+ ";";
        }
        if (!getPropertyString("dx8linkActiveColor").isEmpty()) {
            css += "--theme-link-active:"+getPropertyString("dx8linkActiveColor")+ ";";
        }
        
        if (mode.equalsIgnoreCase("dark-header") || mode.equalsIgnoreCase("light-header")) {
            if (!getPropertyString("dx8-" + mode + "-subMenuNavLinkColor").isEmpty()) {
                css += "--theme-submenu-link:" + getPropertyString("dx8-" + mode + "-subMenuNavLinkColor") + ";";
            }
            if (!getPropertyString("dx8-" + mode + "-navLinkIcon").isEmpty()) {
                css += "--theme-submenu-icon:" + getPropertyString("dx8-" + mode + "-subMenuNavLinkIcon") + ";";
            }
            if (!getPropertyString("dx8-" + mode + "-subMenuNavActiveLinkBackground").isEmpty()) {
                css += "--theme-submenu-active-link-bg:" + getPropertyString("dx8-" + mode + "-subMenuNavActiveLinkBackground") + ";";
            }
            if (!getPropertyString("dx8-" + mode + "-subMenuNavActiveLinkColor").isEmpty()) {
                css += "--theme-submenu-active-link:" + getPropertyString("dx8-" + mode + "-subMenuNavActiveLinkColor") + ";";
            }
            if (!getPropertyString("dx8-" + mode + "-subMenuNavActiveIconColor").isEmpty()) {
                css += "--theme-submenu-active-icon:" + getPropertyString("dx8-" + mode + "-subMenuNavActiveIconColor") + ";";
            }
        }
        css += "}";
        return css;        
    }
    
    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("_a");
        if (action == null || action.isEmpty()) {
            action = request.getParameter("action");
        }
        
        if ("getAssignment".equals(action)) {
            try {
                String appId = request.getParameter("appId");
                WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                if (appId != null && appId.isEmpty()) {
                    appId = null;
                }
                int count = wm.getAssignmentSize(appId, null, null);
                Collection<WorkflowAssignment> assignments = wm.getAssignmentListLite(appId, null, null, null, "a.activated", true, 0, 5);
        
                JSONObject jsonObj = new JSONObject();
                jsonObj.accumulate("count", count);
                
                String format = AppUtil.getAppDateFormat();
                Collection<Map<String, String>> datas = new ArrayList<Map<String, String>>();
                for (WorkflowAssignment a : assignments) {
                    Map<String, String> data = new HashMap<String, String>();
                    data.put("processId", a.getProcessId());
                    data.put("processDefId", a.getProcessDefId());
                    data.put("processRequesterId", a.getProcessRequesterId());
                    data.put("processName", a.getProcessName());
                    data.put("processVersion", a.getProcessVersion());
                    data.put("activityId", a.getActivityId());
                    data.put("activityDefId", a.getActivityDefId());
                    data.put("activityName", a.getActivityName());
                    data.put("assigneeName", a.getAssigneeName());
                    data.put("dateCreated", TimeZoneUtil.convertToTimeZone(a.getDateCreated(), null, format));
                    datas.add(data);
                }
                
                jsonObj.put("data", datas);

                jsonObj.write(response.getWriter());
            } catch (Exception ex) {
                LogUtil.error(this.getClass().getName(), ex, "Get assignment error!");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } else if (action.equals("getFontFamily")) {
            JSONArray jsonArray = new JSONArray();
            String googleFontAPI = request.getParameter("googleFontAPIKey");
            if (googleFontAPI != null && !googleFontAPI.isEmpty()) {
                try {
                    URL url = new URL("https://www.googleapis.com/webfonts/v1/webfonts?key=" + googleFontAPI + "&sort=alpha&capability=WOFF2");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // Read response
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder stringResponse = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            stringResponse.append(inputLine);
                        }
                        in.close();

                        JSONObject jsonObject = new JSONObject(stringResponse.toString());
                        JSONArray itemsArray = jsonObject.getJSONArray("items");

                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject item = itemsArray.getJSONObject(i);
                            String family = item.getString("family");
                            Map<String, String> op = new HashMap<String, String>();
                            op.put("value", family);
                            op.put("label", family);
                            jsonArray.put(op);
                        }
                    }
                } catch (IOException e) {
                    LogUtil.error(getClass().getName(), e, "Failed to retrieve Google Font family list. Please ensure that you have entered a valid API Key");
                }
            }
            jsonArray.write(response.getWriter());
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
