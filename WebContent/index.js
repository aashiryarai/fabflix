let currentPage = 1;
let pageSize = 10;
let sortBy = "title";
let sortOrder = "asc";
let currentParams = {};  // Search or browse params (title/year/etc.)

function saveSessionState() {
    sessionStorage.setItem("currentPage", currentPage);
    sessionStorage.setItem("pageSize", pageSize);
    sessionStorage.setItem("sortBy", sortBy);
    sessionStorage.setItem("sortOrder", sortOrder);
    sessionStorage.setItem("currentParams", JSON.stringify(currentParams));
}

function loadSessionState() {
    if (sessionStorage.getItem("currentPage")) {
        currentPage = parseInt(sessionStorage.getItem("currentPage"));
        pageSize = parseInt(sessionStorage.getItem("pageSize"));
        sortBy = sessionStorage.getItem("sortBy");
        sortOrder = sessionStorage.getItem("sortOrder");
        currentParams = JSON.parse(sessionStorage.getItem("currentParams"));
    }
}

function handleMovieResult(resultData) {
    let starTableBodyElement = jQuery("#star_table_body");
    starTableBodyElement.empty();

    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "<tr>";
        rowHTML += `<td><a href="single-movie.html?id=${resultData[i]["movie_id"]}">${resultData[i]["movie_title"]}</a></td>`;
        rowHTML += `<td>${resultData[i]["movie_year"]}</td>`;
        rowHTML += `<td>${resultData[i]["movie_director"]}</td>`;
        rowHTML += `<td>${resultData[i]["movie_genres"]}</td>`;
        rowHTML += `<td>${resultData[i]["movie_stars"]}</td>`;
        rowHTML += `<td>${resultData[i]["movie_rating"]}</td>`;
        rowHTML += "</tr>";

        starTableBodyElement.append(rowHTML);
    }

    // Disable Previous button if on page 1
    if (currentPage === 1) {
        $("#prev-button").prop("disabled", true);
    } else {
        $("#prev-button").prop("disabled", false);
    }
}

function fetchMovies() {
    const params = {
        ...currentParams,
        sortBy,
        sortOrder,
        page: currentPage,
        pageSize
    };

    saveSessionState();

    $.ajax({
        url: "api/movies",
        method: "GET",
        data: params,
        dataType: "json",
        success: handleMovieResult,
        error: function(xhr) {
            console.log("Fetch failed:", xhr.responseText);
        }
    });
}

function performSearch(e) {
    e.preventDefault();

    currentParams = {
        title: $("#title").val(),
        year: $("#year").val(),
        director: $("#director").val(),
        star: $("#star").val()
    };

    if (!currentParams.title && !currentParams.year && !currentParams.director && !currentParams.star) {
        alert("Please provide at least one search field.");
        return;
    }

    currentPage = 1;
    fetchMovies();
}

// Sorting and pagination controls
$("#sort-by").change(function() {
    sortBy = $(this).val();
    currentPage = 1;
    fetchMovies();
});

$("#sort-order").change(function() {
    sortOrder = $(this).val();
    currentPage = 1;
    fetchMovies();
});

$("#page-size").change(function() {
    pageSize = parseInt($(this).val());
    currentPage = 1;
    fetchMovies();
});

$("#prev-button").click(function() {
    if (currentPage > 1) {
        currentPage--;
        fetchMovies();
    }
});

$("#next-button").click(function() {
    currentPage++;
    fetchMovies();
});

// Authentication check
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
$("#login-button").click(() => window.location.replace("login.html"));

$(document).ready(function () {
    loadSessionState();

    if (Object.keys(currentParams).length > 0) {
        fetchMovies();  // Only fetch if params exist from session
    }

    $("#search-form").submit(performSearch);
});

