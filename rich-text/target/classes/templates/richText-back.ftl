<link href="${request.contextPath}/plugin/org.melkart.plugins.RichText/css/quill.snow.css" rel="stylesheet" />
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css"/>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" />

<div class="form-cell" ${elementMetaData!}>
  <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
  <div class="form-cell-value">
    <div id="melkart-richtext-editor_${element.properties.elementUniqueKey!}">
      ${value!?html}
    </div>
    <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="${elementParamName!}_${element.properties.elementUniqueKey!}" />
  </div>
</div>

<script src="${request.contextPath}/plugin/org.melkart.plugins.RichText/js/highlight.min.js"></script>
<script src="${request.contextPath}/plugin/org.melkart.plugins.RichText/js/quill.js"></script>
<script src="${request.contextPath}/plugin/org.melkart.plugins.RichText/js/katex.min.js"></script>
<script>
  (function() {
    const toolbarOptions = [
      ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
      ['blockquote', 'code-block'],
      ['link', 'image', 'video', 'formula'],

      [{ 'header': 1 }, { 'header': 2 }],               // custom button values
      [{ 'list': 'ordered'}, { 'list': 'bullet' }, { 'list': 'check' }],
      [{ 'script': 'sub'}, { 'script': 'super' }],      // superscript/subscript
      [{ 'indent': '-1'}, { 'indent': '+1' }],          // outdent/indent
      [{ 'direction': 'rtl' }],                         // text direction

      [{ 'size': ['small', false, 'large', 'huge'] }],  // custom dropdown
      [{ 'header': [1, 2, 3, 4, 5, 6, false] }],

      [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
      [{ 'font': [] }],
      [{ 'align': [] }],

      ['clean']                                         // remove formatting button
    ];
    const quill_${element.properties.elementUniqueKey!} = new Quill('#melkart-richtext-editor_${element.properties.elementUniqueKey!}', {
      <#if element.properties.readonly! != 'true'>
        modules: {
          syntax: true,
          toolbar: toolbarOptions,
        },
      <#else>
        modules: {
          toolbar: null,
        },
        readOnly: true,
      </#if>
      theme: 'snow'
    });
    <#--  const val = $('.${elementParamName!}_${element.properties.elementUniqueKey!}').val()
    if (val && val.length > 0) {
      quill_${element.properties.elementUniqueKey!}.setText(val);
    }  -->
    $(".${elementParamName!}_${element.properties.elementUniqueKey!}").parents("form").on("submit", function() {
      $(".${elementParamName!}_${element.properties.elementUniqueKey!}").val(quill_${element.properties.elementUniqueKey!}.getSemanticHTML());
    });
    <#if fullWidth! == 'true'>
      $(".${elementParamName!}_${element.properties.elementUniqueKey!}").parents(".form-cell-value").addClass("rich-text-full-width");
    </#if>
  })()
</script>

<style>
  .rich-text-full-width {
    width: 100% !important;
  }
</style>