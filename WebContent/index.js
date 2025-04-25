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

        let stars = resultData[i]["movie_stars"].split(', ').map(s =>
            `<a href="single-star.html?name=${encodeURIComponent(s)}">${s}</a>`).join(", ");
        rowHTML += `<th>${stars}</th>`;

        rowHTML += `<th>${resultData[i]["movie_rating"]}<button class="btn btn-sm btn-success ml-2 add-to-cart" 
                        data-id="${resultData[i]["movie_id"]}" 
                        data-title="${resultData[i]["movie_title"]}">
                    Add to Cart
                </button>
                </th>`;
        rowHTML += "</tr>";

        starTableBodyElement.append(rowHTML);
    }
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


function performSearch(e) {
    e.preventDefault();
    const params = {
        title: $("#title").val(),
        year: $("#year").val(),
        director: $("#director").val(),
        star: $("#star").val()
    };

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

$.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movies",
    success: handleMovieResult
});

fetch('api/genres')
    .then(res => res.json())
    .then(genres => {
        const container = document.getElementById("genre-links");
        genres.forEach(g => {
            const link = document.createElement("a");
            link.href = `browse-genre.html?genre=${encodeURIComponent(g)}`;
            link.className = "badge badge-info mx-1 my-1";
            link.innerText = g;
            container.appendChild(link);
        });
    })
    .catch(err => console.error("Genre fetch failed:", err));