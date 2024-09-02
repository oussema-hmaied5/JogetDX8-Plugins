document.addEventListener("DOMContentLoaded", function() {
    // Create the search form
    var searchFormDiv = document.createElement("div");
    searchFormDiv.classList.add("header-search");
    searchFormDiv.style.cssText = "display: inline-block; margin-left: auto;";

    var form = document.createElement("form");
    form.action = "http://localhost:8067/jw/web/userview/Platform/home/_/plugins";
    form.method = "get";
    form.style.cssText = "display: flex; align-items: center;";

    var input = document.createElement("input");
    input.type = "text";
    input.id = "app_name";
    input.name = "d-2556228-fn_plugin_name";
    input.placeholder = "Search...";
    input.style.cssText = "margin-right: 10px; padding: 5px; border-radius: 5px; border: 1px solid #ccc;";

    var button = document.createElement("button");
    button.type = "submit";
    button.classList.add("btn", "btn-primary");
    button.style.cssText = "background-color: #007bff; border: none; padding: 5px 10px; border-radius: 5px;";
    button.textContent = "Search";

    // Append input and button to form
    form.appendChild(input);
    form.appendChild(button);

    // Append form to the search form div
    searchFormDiv.appendChild(form);

    // Find the target location to insert the search form
    var headerInfo = document.getElementById("header-info");
    if (headerInfo) {
        // Insert the search form after the header-info div
        headerInfo.parentNode.insertBefore(searchFormDiv, headerInfo.nextSibling);
    } else {
        // Fallback: Add search form to the header if header-info is not found
        var navbar = document.querySelector(".navbar-inner .container-fluid");
        if (navbar) {
            navbar.appendChild(searchFormDiv);
        }
    }
});
