<link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.BootstrapStepsDisplayField/css/bootstrap-steps.css">
<link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.BootstrapStepsDisplayField/css/stepsIconFormatter.css">
<link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.BootstrapStepsDisplayField/css/pe-icon-7-stroke.min.css">

<style type="text/css">
    td.column_${columnName!} .step-circle {
        width: 2rem;
        height: 2rem;
    }

    td.column_${columnName!} .step-circle::before{
        width: calc(6rem + 1rem - 2rem);
    }

    td.column_${columnName!} .stepsIcon{
        margin: 0;
    }

    td.column_${columnName!} .stepsIcon .step .step-title{
        font-size: 14px;
    }

    td.column_${columnName!} .stepsIcon .step .step-icon {
        background-color: ${element.properties.iconBackgroundColor!};
    }

    td.column_${columnName!} .step-icon i{
        color: ${element.properties.iconColor!};
        font-size: 20px !important;
        vertical-align: middle;
        padding-top: 4px !important;
    }

    td.column_${columnName!} .stepsIcon .step.completed .step-icon-wrap::before,
    td.column_${columnName!} .stepsIcon .step.completed .step-icon-wrap::after {
        background-color: ${element.properties.iconBackgroundColor!};
    }

    td.column_${columnName!} .stepsIcon .step.completed .step-icon {
        border-color: ${element.properties.iconBackgroundColorComplete!};
        background-color: ${element.properties.iconBackgroundColorComplete!};
        color: ${element.properties.iconColor!};
    }
    
    <#if element.properties.connector! == "arrow" >
        td.column_${columnName!} .step-icon-wrap::before{
            top: 39% !important;
            width: 10px !important;
            left: -5px !important;
            height: 10px !important;
            background-color: unset !important;
            transform: rotate(-45deg) !important;
            -webkit-transform: rotate(-45deg) !important;
            border: solid !important;
            border-width: 0 2px 2px 0 !important;
        }

        td.column_${columnName!} .step-icon-wrap::after{
            display: none !important;
        }
    </#if>
</style>