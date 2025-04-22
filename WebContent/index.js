/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleStarResult(resultData) {
    console.log("handleStarResult: populating star table from resultData");

    // Populate the star table
    // Find the empty table body by id "star_table_body"
    let starTableBodyElement = jQuery("#star_table_body");

    // Iterate through resultData, no more than 10 entries
    for (let i = 0; i < Math.min(20, resultData.length); i++) {

        // Concatenate the html tags with resultData jsonObject
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<th>" +
            '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">' +
            resultData[i]["movie_title"] +
            '</a>' +
            "</th>";
        rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_genres"] + "</th>";
        //rowHTML += "<th>" + resultData[i]["movie_stars"] + "</th>";
        let starsArray = resultData[i]["movie_stars"];
        let starsString = starsArray.map(star =>
            `<a href="single-star.html?id=${star.star_id}">${star.star_name}</a>`
        ).join(", ");
        rowHTML += "<th>" + starsString + "</th>";

        rowHTML += "<th>" + resultData[i]["movie_rating"] + "</th>";
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        starTableBodyElement.append(rowHTML);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleStarResult
// 1) Fetch session + username
$.ajax({
    url: "api/index",
    method: "GET",
    success: function(data) {
        $("#user-info").text("Signed in as: " + data.username);
    },
    error: function() {
        // no session → bounce to login
        window.location.replace("login.html");
    }
});

// 2) Logout click handler
$("#logout-button").click(function() {
    // hitting /logout invalidates and sends you to login.html
    window.location.replace("logout");
});

jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/movies", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleStarResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});