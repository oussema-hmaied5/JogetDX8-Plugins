<div class="stepsIcon d-flex flex-wrap flex-sm-nowrap justify-content-between padding-top-2x padding-bottom-1x">
    <#list options as option>
        <div attr-data="${option.value!?html}" class="step ${option.class!?html}">
            <div class="step-icon-wrap">
                <div class="step-icon"><i class="${option.icon!?html}"></i></div>
            </div>
            <h4 class="step-title">${option.label!?html}</h4>
        </div>
    </#list>
</div>