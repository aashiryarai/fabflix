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
    console.log("handleMovieResult", resultData);
    const movieTableBodyElement = $("#star_table_body");
    movieTableBodyElement.empty();


    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = `
           <tr>
               <td><a href="single-movie.html?id=${resultData[i]["movie_id"]}">${resultData[i]["movie_title"]}</a></td>
               <td>${resultData[i]["movie_year"]}</td>
               <td>${resultData[i]["movie_director"]}</td>
               <td>${resultData[i]["movie_genres"]}</td>
               <td>${resultData[i]["movie_stars"]}</td>
               <td>
                   ${resultData[i]["movie_rating"]}
                   <button class="btn btn-sm btn-success ml-2 add-to-cart"
                       data-id="${resultData[i]["movie_id"]}"
                       data-title="${resultData[i]["movie_title"]}">
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


    if (params.searchType === "fulltext") {
        $("#search-type-label").text("Using Full-Text Prefix Search");
    } else {
        $("#search-type-label").text("");
    }


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


    const title = $("#title").val().trim();
    const year = $("#year").val().trim();
    const director = $("#director").val().trim();
    const star = $("#star").val().trim();


    currentParams = {
        title,
        year,
        director,
        star
    };


    if (!title && !year && !director && !star) {
        alert("Please provide at least one search field.");
        return;
    }


    // Enable fulltext search only when ONLY title is provided
    if (title && !year && !director && !star) {
        currentParams.searchType = "fulltext";
    } else {
        delete currentParams.searchType;
    }


    currentPage = 1;
    sessionStorage.setItem("origin_page", "index.html");


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


// === AUTOCOMPLETE LOGIC ===
$(document).ready(function() {
    loadSessionState();


    const origin = sessionStorage.getItem("origin_page");


    if (origin && origin.includes("index.html") && Object.keys(currentParams).length > 0) {
        fetchMovies();


        $("#title").val(currentParams.title || "");
        $("#year").val(currentParams.year || "");
        $("#director").val(currentParams.director || "");
        $("#star").val(currentParams.star || "");


        $("#sort-by").val(sortBy);
        $("#sort-order").val(sortOrder);
        $("#page-size").val(pageSize.toString());
    }


    $("#search-form").submit(performSearch);


    let autocompleteCache = {};


    $('#title').autocomplete({
        minChars: 3,
        deferRequestBy: 300,
        lookup: function (query, doneCallback) {
            console.log("Autocomplete search initiated for query:", query);


            const normalizedQuery = query.toLowerCase();


            if (autocompleteCache[normalizedQuery]) {
                console.log("Using cached result for:", normalizedQuery);
                console.log("Cached suggestions:", autocompleteCache[normalizedQuery]);
                doneCallback({ suggestions: autocompleteCache[normalizedQuery] });
                return;
            }


            $.ajax({
                url: "api/autocomplete",
                method: "GET",
                dataType: "json",
                data: { query: normalizedQuery },
                success: function (res) {
                    autocompleteCache[normalizedQuery] = res.suggestions;
                    console.log("Fetched from server:", res.suggestions);
                    doneCallback(res);
                },
                error: function (xhr) {
                    console.error("Autocomplete error:", xhr.responseText);
                }
            });
        },
        onSelect: function (suggestion) {
            console.log("Selected suggestion:", suggestion);
            $('#title').val(suggestion.value);  // Ensure title field reflects selected value
            goToSingleMovie(suggestion.data);   // Redirect to Single Movie Page
        },
        formatResult: function (suggestion, currentValue) {
            const pattern = new RegExp("(" + currentValue + ")", "gi");
            return suggestion.value.replace(pattern, "<strong>$1</strong>");
        }
    });




    $('#title').keydown(function (event) {
        // Prevent form submit if autocomplete is active
        if (event.key === "Enter") {
            if ($('.autocomplete-suggestion.autocomplete-selected').length === 0) {
                console.log("Enter pressed without selecting suggestion. Performing search.");
                event.preventDefault();
                $("#search-form").submit();
            }
        }
    });


});

