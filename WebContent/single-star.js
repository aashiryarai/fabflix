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
 * Retrieve parameter from request URL
 */
function getParameterByName(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}

/**
 * Save session before leaving
 */
function saveSessionState() {
    // You should already have stored necessary info from previous page
    // (currentParams, sortBy, sortOrder, etc.)
    // Nothing special here if you're just viewing single star.
}

/**
 * Go back to previous browsing page
 */
function goBackToPrevious() {
    const origin = sessionStorage.getItem("origin_page");
    if (origin) {
        window.location.href = origin;
    } else {
        window.location.href = "index.html"; // fallback
    }
}

/**
 * Handles the data returned by the API
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

$("#logout-button").click(() => window.location.replace("logout"));

$("#back-button").click(() => goBackToPrevious());

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-star?id=" + starId,
    success: handleResult
});
