<ul class="steps">
     <#list options as option>
         <li attr-data="${option.value!?html}" class="step ${option.class!?html}">
           <div class="step-content">
             <span class="step-circle">${option.numbering!?html}</span>
             <span class="step-text">${option.label!?html}</span>
           </div>
         </li>
     </#list>
</ul>