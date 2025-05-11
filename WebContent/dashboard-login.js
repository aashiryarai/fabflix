const dashboardLoginForm = $("#dashboard_login_form");

/**
 * Handle the data returned by DashboardLoginServlet
 * @param resultDataJson  parsed JSON object
 */
function handleDashboardLoginResult(resultDataJson) {
    console.log("handle dashboard login response", resultDataJson);

    if (resultDataJson.status === "success") {
        window.location.replace("dashboard.html");
    } else {
        $("#dashboard_login_error_message").text(resultDataJson.message);
    }
}

function submitDashboardLoginForm(event) {
    event.preventDefault();

    $.ajax("_dashboard", {
        method: "POST",
        data: dashboardLoginForm.serialize(),
        dataType: "json",
        success: handleDashboardLoginResult,
        error: (xhr) => {
            $("#dashboard_login_error_message").text("Wrong email or password. Try again.");
        }
    });
}

dashboardLoginForm.submit(submitDashboardLoginForm);
