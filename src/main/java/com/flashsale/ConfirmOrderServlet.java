
package com.flashsale;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/confirm")
public class ConfirmOrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null ||
            !"customer".equals(session.getAttribute("userRole"))) {
            response.sendRedirect("login.html");
            return;
        }

        int customerId = (Integer) session.getAttribute("userId");

        try {
            int reservationId = Integer.parseInt(
                request.getParameter("reservationId"));

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                try {
                    // Find the product belonging to this reservation.
                    int productId;

                    String findSql =
                        "SELECT product_id FROM reservations "
                        + "WHERE reservation_id=? AND customer_id=?";

                    try (PreparedStatement ps =
                            con.prepareStatement(findSql)) {

                        ps.setInt(1, reservationId);
                        ps.setInt(2, customerId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new Exception("Reservation not found.");
                            }
                            productId = rs.getInt("product_id");
                        }
                    }

                    // Lock product first to maintain consistent lock ordering.
                    BigDecimal price;
                    int stock;

                    String productSql =
                        "SELECT price, stock FROM products "
                        + "WHERE product_id=? FOR UPDATE";

                    try (PreparedStatement ps =
                            con.prepareStatement(productSql)) {

                        ps.setInt(1, productId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new Exception("Product not found.");
                            }

                            price = rs.getBigDecimal("price");
                            stock = rs.getInt("stock");
                        }
                    }

                    int quantity;

                    // Lock and validate the reservation.
                    String reservationSql =
                        "SELECT quantity FROM reservations "
                        + "WHERE reservation_id=? AND customer_id=? "
                        + "AND status='active' AND expires_at > NOW() "
                        + "FOR UPDATE";

                    try (PreparedStatement ps =
                            con.prepareStatement(reservationSql)) {

                        ps.setInt(1, reservationId);
                        ps.setInt(2, customerId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                con.rollback();

                                response.getWriter().println(
                                    "<h2>Reservation expired or already used!</h2>");
                                response.getWriter().println(
                                    "<a href='products'>Browse Products</a>");
                                return;
                            }

                            quantity = rs.getInt("quantity");
                        }
                    }

                    if (stock < quantity) {
                        throw new Exception("Insufficient stock.");
                    }

                    // Reduce physical stock.
                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "UPDATE products SET stock=stock-? "
                                + "WHERE product_id=?")) {

                        ps.setInt(1, quantity);
                        ps.setInt(2, productId);
                        ps.executeUpdate();
                    }

                    // Mark reservation as confirmed.
                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "UPDATE reservations SET status='confirmed' "
                                + "WHERE reservation_id=?")) {

                        ps.setInt(1, reservationId);
                        ps.executeUpdate();
                    }

                    BigDecimal total =
                        price.multiply(BigDecimal.valueOf(quantity));

                    // Create order.
                    try (PreparedStatement ps =
                            con.prepareStatement(
                                "INSERT INTO orders "
                                + "(reservation_id, customer_id, total_amount) "
                                + "VALUES (?, ?, ?)")) {

                        ps.setInt(1, reservationId);
                        ps.setInt(2, customerId);
                        ps.setBigDecimal(3, total);
                        ps.executeUpdate();
                    }

                    con.commit();

                    response.getWriter().println(
                        "<h1>Order Confirmed Successfully!</h1>");
                    response.getWriter().println(
                        "<p>Total Amount: Rs. " + total + "</p>");
                    response.getWriter().println(
                        "<a href='products'>Continue Shopping</a>");

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid reservation ID.");
        } catch (Exception e) {
            throw new ServletException("Order confirmation failed", e);
        }
    }
}
