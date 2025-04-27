function getParameterByName(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}


function goBackToPrevious() {
    const origin = sessionStorage.getItem("origin_page");
    if (origin) {
        window.location.href = origin;
    } else {
        window.location.href = "index.html"; // fallback if no session found
    }
}

function handleResult(resultData) {
    console.log("handleResult: populating movie info");

    const movieInfoElement      = $("#movie_info");
    const movieTableBodyElement = $("#movie_table_body");
    const movie                 = resultData[0]; // Only ONE movie object

    movieInfoElement.empty();
    movieTableBodyElement.empty();

    movieInfoElement.append(
        `<p><b>Title:</b> ${movie.movie_title}</p>` +
        `<p><b>Year:</b> ${movie.movie_year}</p>` +
        `<p><b>Director:</b> ${movie.movie_director}</p>` +
        `<p><b>Rating:</b> ${movie.movie_rating}</p>`
    );

    const rowHTML = `
      <tr>
        <th>${movie.movie_title}</th>
        <th>${movie.movie_year}</th>
        <th>${movie.movie_director}</th>
        <th>${movie.movie_genres}</th>
        <th>${movie.movie_stars}</th>
        <th>
          ${movie.movie_rating}
          <button
            class="btn btn-success add-to-cart"
            data-id="${movie.movie_id}"
            data-title="${movie.movie_title}"
            style="margin-top: 10px;">
            Add to Cart
          </button>
        </th>
      </tr>
    `;

    movieTableBodyElement.append(rowHTML);
}

$.ajax({
    url: "api/index",
    method: "GET",
    dataType: "json",
    success(data) {
        $("#user-info").text("Signed in as: " + data.username);
    },
    error() {
        window.location.replace("login.html");
    }
});

$("#logout-button").click(() => window.location.replace("logout"));
$("#checkout-button").click(() => window.location.href = "shopping-cart.html");
$("#back-button").click(() => goBackToPrevious()); // ⭐ New back button handler

const movieId = getParameterByName('id');
$.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-movie?id=${movieId}`,
    success: handleResult
});

$(document).on('click', '.add-to-cart', function () {
    const movieId = $(this).data("id");
    const title   = $(this).data("title");

    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            action:  "add",
            movieId: movieId,
            title:   title
        },
        success: () => alert(`${title} added to cart!`),
        error:   () => alert("Failed to add to cart.")
    });
});
