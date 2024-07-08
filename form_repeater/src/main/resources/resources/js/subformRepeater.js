(function($){

    // Extend jQuery functionality to create a subform repeater plugin
    $.fn.extend({
        subformRepeater : function(o){
            var target = this;

            // Check if the target exists
            if($(target)){

                // Initialize each row found in the table's header, body, and footer
                $(target).find("> table > thead > tr.grid-row, > table > tbody > tr.grid-row, > table > tfoot > tr.grid-row").each(function(){
                    initRow($(this), o);
                });

                // Check if rows should be collapsed by default and if collapsible
                if (o.collapsedByDefault === "true" && o.collapsible === "true") {
                    $(target).find("> table > tbody > tr.grid-row").each(function(){
                        // Collapse rows that are not already collapsed and have a collapsible link
                        if(!$(this).hasClass("collapsed-row") && $(this).find("> td > a.repeater-collapsible").length > 0) {
                            collapseRow($(this), o);
                        }
                    });

                    // Adjust the state of the expand/collapse button and its text
                    var button = $(target).find("> .subform_repeater_action > .repeater_actions_collapsible");
                    $(button).removeClass("rows_collapse");
                    $(button).addClass("rows_expand");
                    $(button).find("span").text(o.messages['expandAll']);
                }

                // Hide the expand/collapse button if there are no rows
                if ($(target).find("> table > tbody > tr.grid-row").length === 0) {
                    $(target).find("> .subform_repeater_action > .repeater_actions_collapsible").hide();
                }

                // Click event handler for expand/collapse button
                $(target).find("> .subform_repeater_action > .repeater_actions_collapsible").click(function(){
                    if ($(this).hasClass("rows_collapse")) {
                        // Expand all collapsed rows
                        $(this).removeClass("rows_collapse");
                        $(this).addClass("rows_expand");
                        $(this).find("span").text(o.messages['expandAll']);

                        $(target).find("> table > tbody > tr.grid-row").each(function(){
                            if(!$(this).hasClass("collapsed-row")) {
                                collapseRow($(this), o);
                            }
                        });
                    } else {
                        // Collapse all expanded rows
                        $(this).addClass("rows_collapse");
                        $(this).removeClass("rows_expand");
                        $(this).find("span").text(o.messages['collapseAll']);

                        $(target).find("> table > tbody > tr.grid-row").each(function(){
                            if($(this).hasClass("collapsed-row")) {
                                expandRow($(this), o);
                            }
                        });
                    }
                    return false; // Prevent default link behavior
                });

                // Click event handler for add row button
                $(target).find("> .subform_repeater_action > .repeater-actions-add").click(function(){
                    addRow(o, target, "list");
                    return false; // Prevent default link behavior
                });

                // Enable sorting functionality if sortable class exists
                var sortable = $(target).find(".sortable");
                if (sortable.length > 0) {
                    $(sortable).sortable({
                        handle: ".order",
                        start: function( event, ui ) {
                            // Adjust width of the subform wrapper during sorting start
                            var subform_wrapper = $(ui.item).find(".subform_wrapper");
                            var width = $(target).find("> table > tbody > tr.grid-row:not(.ui-sortable-helper) > td.subform_wrapper").width();
                            $(subform_wrapper).width(width);
                        },
                        stop: function( event, ui ) {
                            // Reset width of the subform wrapper after sorting stops
                            var subform_wrapper = $(ui.item).find(".subform_wrapper");
                            $(subform_wrapper).css("width", "auto");
                        },
                        update: function( event, ui ) {
                            // Update position index after sorting update
                            updatePositionIndex($(target), o);
                        }
                    });
                }

                // Update position index initially
                updatePositionIndex($(target), o);
            }
            return target; // Return the target element
        }
    });

    // Function to initialize a row
    function initRow(row, o) {
        // Check if the repeater is not read-only
        if(o.readonly === undefined || o.readonly !== 'true'){
            // Click event handler for delete action
            $(row).find("> td > a.repeater-action-delete").click(function(){
                if (confirm(o.messages['deleteRow'])) { // Confirm before deleting
                    var target = $(row).closest(".subform_repeater_container");
                    $(row).remove(); // Remove the row
                    updatePositionIndex(target, o); // Update position index
                }
            });

            // Click event handler for add action
            $(row).find("> td > a.repeater-action-add").click(function(){
                addRow(o, row, "row");
            });

            // Update position index
            updatePositionIndex($(row).closest(".subform_repeater_container"), o);
        }

        // Click event handler for collapsible action
        $(row).find("> td > a.repeater-collapsible").click(function(){
            if ($(row).hasClass("collapsed-row")) {
                expandRow(row, o); // Expand the row
            } else {
                collapseRow(row, o); // Collapse the row
            }
        });
    }

    // Function to update position index
    function updatePositionIndex(target, o) {
        var position = $(target).find("> input.position");
        var uv = "";
        // Iterate through each row to collect unique values
        $(target).find("> table > thead > tr.grid-row, > table > tbody > tr.grid-row, > table > tfoot > tr.grid-row").each(function(){
            uv += $(this).find("> td > input.unique_value").val() + ";";
        });
        $(position).val(uv); // Set position index value
        $(target).trigger("change"); // Trigger change event
    }

    // Function to add a new row via AJAX
    function addRow(o, target, mode) {
        $.ajax({
            type: "POST",
            dataType : "text",
            url: o.url, // URL for adding rows
            success: function(response) {
                var newRow = $(response); // Create a new row from the response

                if (mode === "list") {
                    $(target).find("> table > tbody").append(newRow); // Append new row to the table body
                    initRow(newRow, o); // Initialize the new row
                    $(target).find("> .subform_repeater_action > .repeater_actions_collapsible").show(); // Show expand/collapse button if hidden
                } else {
                    $(target).before(newRow); // Insert new row before the target
                    initRow(newRow, o); // Initialize the new row
                }
            }
        });
    }

    // Function to collapse a row
    function collapseRow(row, o) {
        $(row).addClass("collapsed-row"); // Add collapsed class to the row
        $(row).find("> td > a.repeater-collapsible").attr("title", o.messages['expand']); // Set expand title
        var form = $(row).find("> td > .subform-container"); // Find subform container
        var firstField = $(form).find(".subform-cell"); // Find first field in the subform
        var height = $(firstField).height() + 8; // Calculate height with padding
        form.css("height", height + "px"); // Set fixed height
        form.css("overflow", "hidden"); // Hide overflow
    }

    // Function to expand a row
    function expandRow(row, o) {
        $(row).removeClass("collapsed-row"); // Remove collapsed class from the row
        $(row).find("> td > a.repeater-collapsible").attr("title", o.messages['collapse']); // Set collapse title
        var form = $(row).find("> td > .subform-container"); // Find subform container
        form.css("height", "auto"); // Set height to auto (expand)
        form.css("overflow", "visible"); // Show overflow
    }
})(jQuery);
