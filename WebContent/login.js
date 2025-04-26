const login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataJson  parsed JSON object
 */
function handleLoginResult(resultDataJson) {
    console.log("handle login response", resultDataJson);

    if (resultDataJson.status === "success") {
        window.location.replace("index.html");
    } else {
        $("#login_error_message").text(resultDataJson.message);
    }
}

function submitLoginForm(event) {
    event.preventDefault();

    $.ajax("api/login", {
        method: "POST",
        data: login_form.serialize(),
        dataType: "json",            // tell jQuery to parse JSON for us
        success: handleLoginResult,
        error: (xhr) => {
            $("#login_error_message").text("Wrong email or password. Try again.");
        }
    });
}

login_form.submit(submitLoginForm);
