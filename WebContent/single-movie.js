// single-movie.js

// 1. Utility to grab URL params
function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// 2. Render the movie info and inject the Add-to-Cart button inside the table row
function handleResult(resultData) {
    console.log("handleResult: populating movie info");

    const movieInfoElement      = $("#movie_info");
    const movieTableBodyElement = $("#movie_table_body");
    const movie                 = resultData[0]; // Only ONE movie object

    // clear any previous
    movieInfoElement.empty();
    movieTableBodyElement.empty();

    // append the basic info
    movieInfoElement.append(
        `<p><b>Title:</b> ${movie.movie_title}</p>` +
        `<p><b>Year:</b> ${movie.movie_year}</p>` +
        `<p><b>Director:</b> ${movie.movie_director}</p>` +
        `<p><b>Rating:</b> ${movie.movie_rating}</p>`
    );

    // build the table row with a properly formed button
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

// 3. Session check & nav buttons
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

// 4. Fetch the movie from the backend
const movieId = getParameterByName('id');
$.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-movie?id=${movieId}`,
    success: handleResult
});

// 5. Delegate click on Add-to-Cart
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
