<div class="typeAheadMultiSelect">

    <#if element.datalist ??>

        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/bootstrap.min.css" type="text/css">

        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/bootstrap-multiselect.css" type="text/css">

        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/prettify.css" type="text/css">

        <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/bootstrap-multiselect.js"></script>

        <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/prettify.js"></script>

        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.plugin.TypeAheadMultiSelectSearch/lib/jquerysctipttop.css" type="text/css">

    </#if>

    <style>
        ul.multiselect-container {
            width:fit-content
        }

        ul.multiselect-container>li>a label.checkbox input[type="checkbox"]{
            min-width:auto;
            display: block;
        }

        ul.multiselect-container div.input-group input[type="text"]{
            width:100%;
            min-width: auto;
        }

        .typeAheadMultiSelect  .btn-group{
            margin-bottom:13px;
        }
    </style>

    <#if element.datalist ??>
            <select id="${name!?replace(".", "_")}" name="${name!}" multiple="multiple">
                <#list options as option>
                    <option value="${option.value!?html}" grouping="${option.grouping!?html}" <#if values?? && values?seq_contains(option.value!)>selected</#if> >${option.label!?html}</option>
                </#list>                                                                      
            </select>
            <input type="hidden" name="${name!}" value=""/>
    <#else>
            <input id="${name!?replace(".", "_")}" name="${name!}" type="text" size="10" placeholder="${label!?html}"/>
    </#if>

    <#if (element.properties.controlField?? && element.properties.controlField! != "") >
        <script>
            FormUtil = {
                controlFields : [],
                setControlField : function(fieldId, selector) {
                    if (selector !== undefined) {
                        $(selector).addClass("control-field");
                    } else {
                        $("[name='"+fieldId+"']:not(form)").addClass("control-field");
                    }
                    if (FormUtil.controlFields.indexOf(fieldId) === -1) {
                        FormUtil.controlFields.push(fieldId);
                    }
                },
                isControlField : function(fieldId, field) {
                    if (FormUtil.controlFields.indexOf(fieldId) !== -1 || $(field).hasClass("control-field")) {
                        return true;
                    } else {
                        //handle for subform
                        for (var i = 0; i < FormUtil.controlFields.length; i++) {
                            if (FormUtil.controlFields[i].startsWith("_") && fieldId.endsWith(FormUtil.controlFields[i])) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
            };
        </script>
        <script type="text/javascript" src="${contextPath}/plugin/org.joget.plugin.enterprise.SelectBoxDataListFilterType/js/jquery.dynamicoptions.js"></script>
    </#if>

    <!-- https://www.jqueryscript.net/demo/jQuery-Multiple-Select-Plugin-For-Bootstrap-Bootstrap-Multiselect/-->
    <script type="text/javascript">
        $(document).ready(function(){
            var myvariable = "${label!?html}";

            $('#${name!}').multiselect({

                <#if (element.properties.selectAll?? && element.properties.selectAll! != "") >
                    includeSelectAllOption: true,
                </#if>

                <#if (element.properties.searchFilter?? && element.properties.searchFilter! != "") >
                    enableCaseInsensitiveFiltering: true,
                </#if>

                nonSelectedText: myvariable,

                buttonText: function(options) {
                    if (options.length == 0) {
                       return myvariable;
                    }
                    else if (options.length > 0) {
                      return options.length + ' selected  ';
                    }
                }

            });

            if($("#${name!}").val() == "") {
                $("#${name!}").css("color", "gray");
            }

            $("#${name!}").change(function(){
                if($(this).val() == "") {
                    $(this).css("color", "gray");
                } else {
                    $(this).css("color", "inherit");
                }
            });

            <#if (element.properties.controlField?? && element.properties.controlField! != "") >
                $("#${name!}").dynamicOptions({
                    controlField : "${element.properties.controlFieldName!}",
                    paramName : "${name!}",
                    type : "selectbox",
                    readonly : "false",
                    nonce : "${element.properties.nonce!}",
                    binderData : "${element.properties.binderData!}",
                    appId : "${element.properties.appId!}",
                    appVersion : "${element.properties.appVersion!}",
                    contextPath : "${request.contextPath}"
                });

                $("#${name!}").change( function(){
                    $("#${name!}").multiselect("rebuild");
                });
            </#if>
        });
    </script>

</div>