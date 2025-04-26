let currentPage = 1;
let pageSize = 10;
let sortBy = "title";
let sortOrder = "asc";
let currentParams = {}; // Search conditions: title, year, director, star

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
    const movieTableBodyElement = $("#star_table_body");
    movieTableBodyElement.empty();

        rowHTML += `<th>${resultData[i]["movie_rating"]}<button class="btn btn-sm btn-success ml-2 add-to-cart" 
                        data-id="${resultData[i]["movie_id"]}" 
                        data-title="${resultData[i]["movie_title"]}">
                    Add to Cart
                </button>
                </th>`;
        rowHTML += "</tr>";


    for (const movie of resultData) {
        const rowHTML = `
            <tr>
                <td><a href="#" onclick="goToSingleMovie('${movie.movie_id}')">${movie.movie_title}</a></td>
                <td>${movie.movie_year}</td>
                <td>${movie.movie_director}</td>
                <td>${movie.movie_genres || ""}</td>
                <td>${movie.movie_stars || ""}</td>
                <td>${movie.movie_rating}</td>
            </tr>
        `;
        movieTableBodyElement.append(rowHTML);
    }

    $("#prev-button").prop("disabled", currentPage === 1);
}
$(document).on('click', '.add-to-cart', function () {
    const movieId = $(this).data("id");
    const title = $(this).data("title");

    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            action: "add",
            movieId: movieId,
            title: title
        },
        success: () => alert(`${title} added to cart!`),
        error: () => alert("Failed to add to cart.")
    });
});


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
            console.error("Fetch failed:", xhr.responseText);
        }
    });
}

function performSearch(event) {
    event.preventDefault();

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

    sessionStorage.setItem("origin_page", "index.html"); // 🔥 Save that the user is coming from search

    fetchMovies();
}


function goToSingleMovie(movieId) {
    saveSessionState();
    window.location.href = `single-movie.html?id=${movieId}`;
}

// Event Listeners
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

$("#checkout-button").click(() => window.location.href = "shopping-cart.html");

$("#search-form").submit(performSearch);

// Main
$(document).ready(function() {
    loadSessionState();

    // Only auto-fetch if origin_page was set (coming back from single-movie/star), NOT from browse pages
    const origin = sessionStorage.getItem("origin_page");

    if (origin && origin.includes("index.html") && Object.keys(currentParams).length > 0) {
        fetchMovies();

        // Repopulate search fields
        $("#title").val(currentParams.title || "");
        $("#year").val(currentParams.year || "");
        $("#director").val(currentParams.director || "");
        $("#star").val(currentParams.star || "");

        // Repopulate dropdowns
        $("#sort-by").val(sortBy);
        $("#sort-order").val(sortOrder);
        $("#page-size").val(pageSize.toString());
    }

    $("#search-form").submit(performSearch);
});
