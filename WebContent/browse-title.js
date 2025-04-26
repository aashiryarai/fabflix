function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// Session variables
let startsWith = getParameterByName("startsWith") || "A";
let currentPage = parseInt(sessionStorage.getItem("browse_currentPage")) || 1;
let pageSize = parseInt(sessionStorage.getItem("browse_pageSize")) || 10;
let sortBy = sessionStorage.getItem("browse_sortBy") || "title";
let sortOrder = sessionStorage.getItem("browse_sortOrder") || "asc";

// Save session state
function saveSession() {
    sessionStorage.setItem("browse_currentPage", currentPage);
    sessionStorage.setItem("browse_pageSize", pageSize);
    sessionStorage.setItem("browse_sortBy", sortBy);
    sessionStorage.setItem("browse_sortOrder", sortOrder);
    sessionStorage.setItem("browse_startsWith", startsWith);
}

function handleResult(resultData) {
    let tableBody = jQuery("#movie_table_body");
    tableBody.empty();

    if (resultData.length === 0 && currentPage > 1) {
        currentPage--;
        fetchMovies(); // rollback a page if no results
        return;
    }

    resultData.forEach(movie => {
        let row = `
            <tr>
                <td><a href="single-movie.html?id=${movie.movie_id}">${movie.movie_title}</a></td>
                <td>${movie.movie_year}</td>
                <td>${movie.movie_director}</td>
                <td>${movie.movie_genres}</td>
                <td>${movie.movie_stars}</td>
                <td>${movie.movie_rating}</td>
            </tr>`;
        tableBody.append(row);
    });

    $("#prev-button").prop("disabled", currentPage === 1);
}

// Fetch movie list with current params
function fetchMovies() {
    saveSession();

    $.ajax({
        dataType: "json",
        method: "GET",
        url: `api/browse-title`,
        data: {
            startsWith,
            sortBy,
            sortOrder,
            page: currentPage,
            pageSize
        },
        success: handleResult,
        error: function (xhr) {
            console.error("Failed to load movies:", xhr.responseText);
        }
    });
}

// Setup pagination buttons (you must include these buttons in the HTML if not already)
$(document).ready(function () {
    // Authentication
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

    $("#prev-button").click(() => {
        if (currentPage > 1) {
            currentPage--;
            fetchMovies();
        }
    });

    $("#next-button").click(() => {
        currentPage++;
        fetchMovies();
    });

    $("#sort-by").change(function () {
        sortBy = $(this).val();
        currentPage = 1;
        fetchMovies();
    });

    $("#sort-order").change(function () {
        sortOrder = $(this).val();
        currentPage = 1;
        fetchMovies();
    });

    $("#page-size").change(function () {
        pageSize = parseInt($(this).val());
        currentPage = 1;
        fetchMovies();
    });

    // Load on first visit
    fetchMovies();
});
