$(document).ready(() => {
    // Display total
    $.get("api/cart", (items) => {
        const total = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
        $("#cart-summary").html(`<h4>Total: $${total.toFixed(2)}</h4>`);
    });

    $("#payment-form").submit((e) => {
        e.preventDefault();
        const formData = $("#payment-form").serialize();

        $.post("api/payment", formData)
            .done(() => window.location.href = "confirmation.html")
            .fail(() => alert("Payment failed. Please check your card info."));
    });
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