function fetchCart() {
    $.ajax({
        url: "api/cart",
        method: "GET",
        success: renderCart,
        error: () => alert("Failed to load cart.")
    });
}

function renderCart(data) {
    let html = '<table class="table"><thead><tr><th>Title</th><th>Qty</th><th>Price</th><th>Total</th><th>Action</th></tr></thead><tbody>';
    let total = 0;

    data.forEach(item => {
        const itemTotal = item.price * item.quantity;
        total += itemTotal;
        html += `
        <tr>
            <td>${item.title}</td>
            <td>${item.quantity}</td>
            <td>$${item.price.toFixed(2)}</td>
            <td>$${itemTotal.toFixed(2)}</td>
            <td>
                <button class="btn btn-sm btn-info update" data-id="${item.movieId}" data-action="increase">+</button>
                <button class="btn btn-sm btn-warning update" data-id="${item.movieId}" data-action="decrease">-</button>
                <button class="btn btn-sm btn-danger update" data-id="${item.movieId}" data-action="delete">Delete</button>
            </td>
        </tr>`;
    });

    html += '</tbody></table>';
    $("#cart-container").html(html);
    $("#total-price").text("Total: $" + total.toFixed(2));
}

$(document).on("click", ".update", function () {
    const movieId = $(this).data("id");
    const action = $(this).data("action");

    $.post("api/cart", { movieId, action }, fetchCart);
});

fetchCart();

