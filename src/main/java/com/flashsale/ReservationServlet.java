
package com.flashsale;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/reserve")
public class ReservationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"customer".equals(session.getAttribute("userRole"))) {
            response.sendRedirect("login.html");
            return;
        }

        int customerId =
                (Integer) session.getAttribute("userId");

        int productId;
        int quantity;

        try {
            productId = Integer.parseInt(request.getParameter("productId"));
            quantity = Integer.parseInt(request.getParameter("quantity"));

            if (productId <= 0 || quantity <= 0) {
                throw new NumberFormatException();
            }

        } catch (Exception e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product or quantity."
            );
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            con.setAutoCommit(false);

            try {
                int stock;

                // Lock the product row to prevent overselling.
                String productSql =
                        "SELECT stock FROM products WHERE product_id = ? FOR UPDATE";

                try (PreparedStatement ps =
                             con.prepareStatement(productSql)) {

                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            con.rollback();
                            showMessage(response, "Product not found.");
                            return;
                        }

                        stock = rs.getInt("stock");
                    }
                }

                // Expire old reservations for this product.
                String expireSql =
                        "UPDATE reservations SET status = 'expired' " +
                        "WHERE product_id = ? AND status = 'active' " +
                        "AND expires_at <= NOW()";

                try (PreparedStatement ps =
                             con.prepareStatement(expireSql)) {

                    ps.setInt(1, productId);
                    ps.executeUpdate();
                }

                // Calculate currently reserved quantity.
                int reserved = 0;

                String reservedSql =
                        "SELECT COALESCE(SUM(quantity), 0) AS total " +
                        "FROM reservations " +
                        "WHERE product_id = ? " +
                        "AND status = 'active' AND expires_at > NOW()";

                try (PreparedStatement ps =
                             con.prepareStatement(reservedSql)) {

                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            reserved = rs.getInt("total");
                        }
                    }
                }

                int available = stock - reserved;

                if (quantity > available) {
                    con.rollback();
                    showMessage(
                            response,
                            "Insufficient stock. Available quantity: " + available
                    );
                    return;
                }

                // Create a reservation lasting 10 minutes.
                String insertSql =
                        "INSERT INTO reservations " +
                        "(customer_id, product_id, quantity, expires_at, status) " +
                        "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'active')";

                int reservationId;

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     insertSql,
                                     PreparedStatement.RETURN_GENERATED_KEYS
                             )) {

                    ps.setInt(1, customerId);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {

                        if (!rs.next()) {
                            throw new Exception(
                                    "Could not create reservation."
                            );
                        }

                        reservationId = rs.getInt(1);
                    }
                }

                con.commit();

                // Display reservation success with ONE confirmation form.
                showReservationSuccess(response, reservationId);

            } catch (Exception e) {

                con.rollback();
                throw e;

            } finally {

                con.setAutoCommit(true);
            }

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    response,
                    "Reservation failed. Please try again."
            );
        }
    }

    private void showReservationSuccess(
            HttpServletResponse response,
            int reservationId
    ) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Reservation Successful</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h2>Reservation Successful!</h2>");
        out.println("<p>Your product is reserved for 10 minutes.</p>");

        out.println("<h3 id='timer'>10:00</h3>");

        // Only ONE Confirm Order form.
        out.println("<form action='confirm' method='post'>");
        out.println("<input type='hidden' name='reservationId' value='" +
                reservationId + "'>");
        out.println("<button type='submit'>Confirm Order</button>");
        out.println("</form>");

        out.println("<script>");
        out.println("let seconds = 600;");
        out.println("const timer = document.getElementById('timer');");

        out.println("const interval = setInterval(function() {");
        out.println("seconds--;");

        out.println("let minutes = Math.floor(seconds / 60);");
        out.println("let remaining = seconds % 60;");

        out.println("timer.textContent = minutes + ':' + " +
                "String(remaining).padStart(2, '0');");

        out.println("if (seconds <= 0) {");
        out.println("clearInterval(interval);");
        out.println("timer.textContent = 'Reservation Expired';");
        out.println("}");
        out.println("}, 1000);");

        out.println("</script>");

        out.println("<br><a href='products'>Back to Products</a>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showMessage(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>FlashSale</title></head>");
        out.println("<body>");

        out.println("<h2>" + message + "</h2>");
        out.println("<a href='products'>Back to Products</a>");

        out.println("</body>");
        out.println("</html>");
    }
}
