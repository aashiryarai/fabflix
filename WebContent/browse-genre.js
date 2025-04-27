// browse-genre.js

let currentPage = 1;
let pageSize = 10;
let sortBy = "title";
let sortOrder = "asc";
let currentParams = {};

function saveSessionState() {
    sessionStorage.setItem("currentPage", currentPage);
    sessionStorage.setItem("pageSize", pageSize);
    sessionStorage.setItem("sortBy", sortBy);
    sessionStorage.setItem("sortOrder", sortOrder);
    sessionStorage.setItem("currentParams", JSON.stringify(currentParams));
    sessionStorage.setItem("origin_page", "browse-genre.html");
}

function loadSessionState() {
    if (sessionStorage.getItem("currentPage")) {
        currentPage    = parseInt(sessionStorage.getItem("currentPage"));
        pageSize       = parseInt(sessionStorage.getItem("pageSize"));
        sortBy         = sessionStorage.getItem("sortBy");
        sortOrder      = sessionStorage.getItem("sortOrder");
        currentParams  = JSON.parse(sessionStorage.getItem("currentParams"));
    }
}

function getParameterByName(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}

function goToSingleMovie(movieId) {
    saveSessionState();
    window.location.href = `single-movie.html?id=${movieId}`;
}

function goToSingleStar(starId) {
    saveSessionState();
    window.location.href = `single-star.html?id=${starId}`;
}

function handleMovieResult(resultData) {
    const movieTableBodyElement = $("#movie_table_body");
    movieTableBodyElement.empty();

    // If no results but not on the first page, go back a page
    if (resultData.length === 0 && currentPage > 1) {
        currentPage--;
        fetchMovies();
        return;
    }

    for (const movie of resultData) {
        const genreLinks = (movie.movie_genres || "")
            .split(", ")
            .map(genre =>
                `<a href="browse-genre.html?genre=${encodeURIComponent(genre)}">${genre}</a>`
            ).join(", ");

        const rowHTML = `
            <tr>
                <td>
                  <a href="#" onclick="goToSingleMovie('${movie.movie_id}')">
                    ${movie.movie_title}
                  </a>
                </td>
                <td>${movie.movie_year}</td>
                <td>${movie.movie_director}</td>
                <td>${genreLinks}</td>
                <td>${movie.movie_stars || ""}</td>
                <td>
                  ${movie.movie_rating}
                  <button
                    class="btn btn-sm btn-success ml-2 add-to-cart"
                    data-id="${movie.movie_id}"
                    data-title="${movie.movie_title}">
                    Add to Cart
                  </button>
                </td>
            </tr>
        `;
        movieTableBodyElement.append(rowHTML);
    }

    $("#prev-button").prop("disabled", currentPage === 1);
}

$(document).on('click', '.add-to-cart', function () {
    const movieId = $(this).data("id");
    const title   = $(this).data("title");

    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            action: "add",
            movieId: movieId,
            title: title
        },
        success: () => alert(`${title} added to cart!`),
        error:   () => alert("Failed to add to cart.")
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
        url: "api/browse-genre",
        method: "GET",
        data: params,
        dataType: "json",
        success: handleMovieResult,
        error:   xhr => console.error("Fetch failed:", xhr.responseText)
    });
}

function performGenreBrowse() {
    currentParams  = { genre: getParameterByName('genre') || "" };
    currentPage    = 1;
    fetchMovies();
}

$(document).ready(() => {
    // Authentication check
    $.ajax({
        url: "api/index",
        method: "GET",
        dataType: "json",
        success: data => {
            $("#user-info").text("Signed in as: " + data.username);
        },
        error: () => window.location.replace("login.html")
    });

    $("#logout-button").click(() => window.location.replace("logout"));
    $("#checkout-button").click(() => window.location.href = "shopping-cart.html");
    $("#login-button").click(() => window.location.replace("login.html"));

    $("#sort-by").change(function() {
        sortBy      = $(this).val();
        currentPage = 1;
        fetchMovies();
    });

    $("#sort-order").change(function() {
        sortOrder   = $(this).val();
        currentPage = 1;
        fetchMovies();
    });

    $("#page-size").change(function() {
        pageSize    = parseInt($(this).val());
        currentPage = 1;
        fetchMovies();
    });

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

    // On load: either resume session or start fresh by genre URL
    const genreFromURL = getParameterByName('genre');
    if (genreFromURL) {
        currentParams = { genre: genreFromURL };
        currentPage   = 1;
        fetchMovies();
    } else {
        loadSessionState();
        fetchMovies();
    }

    $("#sort-by").val(sortBy);
    $("#sort-order").val(sortOrder);
    $("#page-size").val(pageSize.toString());
});
