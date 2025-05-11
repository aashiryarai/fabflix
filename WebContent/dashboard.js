$(document).ready(() => {
    $.ajax({
        url: "api/session",
        method: "GET",
        dataType: "json",
        success: (response) => {
            if (!response.isEmployee) {
                window.location.replace("dashboard-login.html");
            }
        },
        error: () => {
            window.location.replace("dashboard-login.html");
        }
    });
});

// ADD STAR HANDLING
$("#add-star-form").submit((event) => {
    event.preventDefault();

    $.ajax("api/add-star", {
        method: "POST",
        data: $("#add-star-form").serialize(),
        dataType: "json",
        success: (response) => {

            $("#add-star-result").text(response.message || "Star added.");
        },
        error: (xhr) => {
            $("#add-star-result").text("Failed to add star. Check server.");
        }
    });
});

// LOAD METADATA
$("#load-metadata").click(() => {
    $.ajax("api/metadata", {
        method: "GET",
        dataType: "json",
        success: (response) => {
            const tables = response.tables;
            let html = "";
            tables.forEach((table) => {
                html += `<h3>${table.name}</h3><ul>`;
                table.columns.forEach((col) => {
                    html += `<li>${col.name} (${col.type})</li>`;
                });
                html += `</ul>`;
            });
            $("#metadata-result").html(html);
        },
        error: () => {
            $("#metadata-result").html("Failed to load metadata.");
        }
    });
});

// ADD MOVIE HANDLING
$("#add-movie-form").submit((event) => {
    event.preventDefault();

    $.ajax("api/add-movie", {
        method: "POST",
        data: $("#add-movie-form").serialize(),
        dataType: "json",
        success: (response) => {
            $("#add-movie-result").text(response.message || "Movie added.");
        },
        error: (xhr) => {
            $("#add-movie-result").text("Failed to add movie. Check server.");
        }
    });
});
