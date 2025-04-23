/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {
    let tableBody = jQuery("#movie_table_body");
    tableBody.empty();

    resultData.forEach(movie => {
        let row = `
            <tr>
                <td><a href="single-movie.html?id=${movie.id}">${movie.title}</a></td>
                <td>${movie.year}</td>
                <td>${movie.director}</td>
            </tr>`;
        tableBody.append(row);
    });
}


// Get id from URL
let genre = getParameterByName('genre');
$.ajax({
    url: "api/index",
    method: "GET",
    dataType: "json",      // ← tell jQuery to JSON‑parse the response
    success: function(data) {
        console.log("parsed session data:", data);
        $("#user-info").text("Signed in as: " + data.username);
    },
    error: function() {
        window.location.replace("login.html");
    }
});
$("#logout-button").click(function() {
    // hitting /logout invalidates and sends you to login.html
    window.location.replace("logout");
});

let letter = getParameterByName('startsWith');
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/browse-title?startsWith=${letter}`,
    success: handleResult
});