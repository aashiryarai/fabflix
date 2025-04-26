function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function handleResult(resultData) {
    console.log("handleResult: populating movie info");

    let movieInfoElement = jQuery("#movie_info");
    let movieTableBodyElement = jQuery("#movie_table_body");

    let movie = resultData[0]; // Only ONE movie object

    movieInfoElement.append(
        "<p><b>Title:</b> " + movie["movie_title"] + "</p>" +
        "<p><b>Year:</b> " + movie["movie_year"] + "</p>" +
        "<p><b>Director:</b> " + movie["movie_director"] + "</p>" +
        "<p><b>Rating:</b> " + movie["movie_rating"] + "</p>"
    );

    let rowHTML = "<tr>";

    rowHTML += "<th>" + movie["movie_title"] + "</th>";
    rowHTML += "<th>" + movie["movie_year"] + "</th>";
    rowHTML += "<th>" + movie["movie_director"] + "</th>";

    // Genres - already formatted HTML
    rowHTML += "<th>" + movie["movie_genres"] + "</th>";

    // Stars - already formatted HTML
    rowHTML += "<th>" + movie["movie_stars"] + "</th>";

    rowHTML += "<th>" + movie["movie_rating"] + "</th>";

    rowHTML += "</tr>";

    movieTableBodyElement.append(rowHTML);
}

// Session Info
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

// Logout
$("#logout-button").click(function() {
    window.location.replace("logout");
});

// Load movie
let movieId = getParameterByName('id');
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-movie?id=" + movieId,
    success: handleResult
});
