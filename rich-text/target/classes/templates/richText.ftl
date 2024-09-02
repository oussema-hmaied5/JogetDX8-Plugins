<link rel="stylesheet" href="${request.contextPath}/plugin/org.melkart.plugins.RichText/tiny_mce/js/tinymce/plugins/codesample/css/prism.css">
<link rel="stylesheet" type="text/css" id="u0" href="${request.contextPath}/plugin/org.melkart.plugins.RichText/tiny_mce/js/tinymce/skins/lightgray/skin.min.css">
<link rel="stylesheet" type="text/css" id="u0" href="${request.contextPath}/plugin/org.melkart.plugins.RichText/tiny_mce/js/tinymce/skins/lightgray/content.min.css">
<link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/org.melkart.plugins.RichText/css/custom.css">

<div class="form-cell" ${elementMetaData!}>
  <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
  <div class="form-cell-value">
    <textarea style="visibility: hidden" id="${elementParamName!}_${element.properties.elementUniqueKey!}" name="${elementParamName!}" value="${value!?html}" class="${elementParamName!}_${element.properties.elementUniqueKey!} tinymce">${value!?html}</textarea>
  </div>
</div>

<script type="text/javascript" src="${request.contextPath}/plugin/org.melkart.plugins.RichText/tiny_mce/js/tinymce/tinymce.min.js"></script>

<script>
  (function() {
    tinymce.init({
      selector: '#${elementParamName!}_${element.properties.elementUniqueKey!}',
      height: ${defautHeight!},
      plugins: [
        'advlist autolink lists link image charmap print preview hr anchor pagebreak',
        'searchreplace wordcount visualblocks visualchars code fullscreen',
        'insertdatetime media nonbreaking table contextmenu directionality',
        'emoticons paste textcolor colorpicker textpattern imagetools codesample toc'
      ],
      toolbar1: 'undo redo | insert | styleselect fontsizeselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image media table codesample | forecolor backcolor emoticons | print preview',
      menubar: 'edit insert view format table tools',
      image_advtab: true,
      relative_urls: false,
      convert_urls: false,
      valid_elements: '*[*]',
      <#if element.properties.readonly! == 'true'>
        readonly: true,
      </#if>
      setup: function (ed) {
        ed.on('init', function (ed) {
          $('#${elementParamName!}_${element.properties.elementUniqueKey!}').css('visibility', 'visible');
        });
      }
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