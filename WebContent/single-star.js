/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it knows which id to look for.
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data into the correct html elements.
 */

/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, reads the jsonObject and populates data into html elements
 * @param resultData jsonObject
 */
function handleResult(resultData) {
    console.log("handleResult: populating star info from resultData");

    let starInfoElement = jQuery("#star_info");
    let movieTableBodyElement = jQuery("#movie_table_body");

    let star = resultData[0]; // Only one star object

    let yearOfBirth = star["year_of_birth"];
    if (!yearOfBirth || yearOfBirth === "null" || yearOfBirth === null) {
        yearOfBirth = "N/A";
    }

    // Populate the basic star info
    starInfoElement.append(
        "<p><b>Star Name:</b> " + star["star_name"] + "</p>" +
        "<p><b>Year of Birth:</b> " + yearOfBirth + "</p>"
    );

    console.log("handleResult: populating movie table from resultData");

    let rowHTML = "<tr>";
    rowHTML += "<th>" + star["star_name"] + "</th>";
    rowHTML += "<th>" + yearOfBirth + "</th>";
    rowHTML += "<th>" + (star["star_movies"] || "") + "</th>";
    rowHTML += "</tr>";

    movieTableBodyElement.append(rowHTML);
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Get star id from URL
let starId = getParameterByName('id');

$.ajax({
    url: "api/index",
    method: "GET",
    dataType: "json",
    success: function(data) {
        console.log("parsed session data:", data);
        $("#user-info").text("Signed in as: " + data.username);
    },
    error: function() {
        window.location.replace("login.html");
    }
});

// Logout click handler
$("#logout-button").click(function() {
    window.location.replace("logout");
});
$("#checkout-button").click(() => window.location.href = "shopping-cart.html");

// Load star info
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-star?id=" + starId,
    success: handleResult
});
