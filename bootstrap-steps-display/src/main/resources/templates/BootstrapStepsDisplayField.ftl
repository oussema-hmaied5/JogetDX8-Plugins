<div class="form-cell" ${elementMetaData!}>
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.BootstrapStepsDisplayField/css/bootstrap-steps.css">
    <style type="text/css">
        #${elementParamName!}.steps {
            transform: scale(${scale!});
            margin: 1.2rem auto;
            width: fit-content;
            width: -moz-fit-content;
        }

        #${elementParamName!} .step-circle {
            width: 2.5rem;
            height: 2.5rem;
        }

        #${elementParamName!} .step-circle::before{
            width: calc(8rem + 1rem - 2.5rem);
        }

        #${elementParamName!} .step-content{
            width: 8rem;
            max-width: 8rem;
        }

        #${elementParamName!} .step-text{
            word-break: break-word;
            text-align: center;
        }

        <#if element.properties.connector! == "arrow" >
        #${elementParamName!} .step-circle::before{
            top: 25%;
            width: 15px;
            left: -50px;
            height: 15px;
            background-color: unset !important;
            transform: rotate(-45deg);
            -webkit-transform: rotate(-45deg);
            border-width: 0 2px 2px 0 !important;
            border: solid;
        }
        </#if>
    </style>
    <script type="text/javascript">

        $(function(){
            
            if( FormUtil.getField("${element.properties.statusFieldId!}").size() ){
                updateSteps_${elementParamName!}( FormUtil.getValue("${element.properties.statusFieldId!}") );
            }else{
                updateSteps_${elementParamName!}( "${value!}" );
            }


            FormUtil.getField("${element.properties.statusFieldId!}").change(function(){
                updateSteps_${elementParamName!}( FormUtil.getValue("${element.properties.statusFieldId!}") );
            });

        });

        function updateSteps_${elementParamName!}(value){
            value = value.replace(/"/g, '\\"');
            value = value.replace(/'/g, "\\'");

            $("#${elementParamName!}").find("li.step").removeClass("step-active").removeClass("step-success");
            if( value != ""){
                $("#${elementParamName!}").find(".step[attr-data='" + value + "']").addClass("step-active");
                $("#${elementParamName!}").find(".step[attr-data='" + value + "']").prevAll().addClass("step-success");
            }
        }

    </script>
    
       <ul id="${elementParamName!}" class="steps">
            <#list options as option>
                <li attr-data="${option.value!?html}" class="step">
                  <div class="step-content">
                    <span class="step-circle">${option.numbering!?html}</span>
                    <span class="step-text">${option.label!?html}</span>
                  </div>
                </li>
            </#list>
      </ul>

</div>