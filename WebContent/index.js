function handleMovieResult(resultData) {
    let starTableBodyElement = jQuery("#star_table_body");
    starTableBodyElement.empty(); // Clear previous results

    //console.log("Result data received:", resultData);

    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "<tr>";
        rowHTML += `<th><a href="single-movie.html?id=${resultData[i]["movie_id"]}">${resultData[i]["movie_title"]}</a></th>`;
        rowHTML += `<th>${resultData[i]["movie_year"]}</th>`;
        rowHTML += `<th>${resultData[i]["movie_director"]}</th>`;

        // Render genres as hyperlinks
        let genres = resultData[i]["movie_genres"].split(', ').map(g =>
            `<a href="browse-genre.html?genre=${encodeURIComponent(g)}">${g}</a>`).join(", ");
        rowHTML += `<th>${genres}</th>`;

        // Render stars as hyperlinks
        let stars = resultData[i]["movie_stars"].split(', ').map(s =>
            `<a href="single-star.html?name=${encodeURIComponent(s)}">${s}</a>`).join(", ");
        rowHTML += `<th>${stars}</th>`;

        rowHTML += `<th>${resultData[i]["movie_rating"]}</th>`;
        rowHTML += "</tr>";

        starTableBodyElement.append(rowHTML);
    }
}

function performSearch(e) {
    e.preventDefault();
    const params = {
        title: $("#title").val(),
        year: $("#year").val(),
        director: $("#director").val(),
        star: $("#star").val()
    };

    // Skip empty search
    if (!params.title && !params.year && !params.director && !params.star) {
        alert("Please provide at least one search field.");
        return;
    }

    $.ajax({
        url: "api/movies",
        method: "GET",
        data: params,
        dataType: "json",
        success: handleMovieResult,
        error: function(xhr) {
            console.log("Search failed:", xhr.responseText);
        }
    });
}

// Initial movie load (top 20)
$.ajax({
    url: "api/index",
    method: "GET",
    dataType: "json",
    success: function(data) {
        $("#user-info").text("Signed in as: " + data.username);
    },
    error: function() {
        window.location.replace("login.html");
    }
});

$("#logout-button").click(() => window.location.replace("logout"));
$("#search-form").submit(performSearch);

// Initial load top 20 movies (no filter)
$.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movies",
    success: handleMovieResult
});
