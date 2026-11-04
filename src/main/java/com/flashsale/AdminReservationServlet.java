
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

@WebServlet("/admin/reservations")
public class AdminReservationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            !"admin".equals(session.getAttribute("userRole"))) {

            response.sendRedirect(
                request.getContextPath() + "/login.html"
            );
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        String filter = request.getParameter("status");

        if (filter == null || filter.isBlank()) {
            filter = "all";
        }

        if (!filter.equals("all") &&
            !filter.equals("active") &&
            !filter.equals("expired") &&
            !filter.equals("confirmed") &&
            !filter.equals("cancelled")) {

            filter = "all";
        }

        PrintWriter out = response.getWriter();

        out.println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">

                <title>Reservation Management</title>

                <link rel="stylesheet" href="../style.css">

                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: #f3f6fa;
                        margin: 0;
                        color: #263445;
                    }

                    header {
                        background: #205b78;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }

                    .container {
                        max-width: 1200px;
                        margin: 30px auto;
                        padding: 25px;
                        background: white;
                        border-radius: 10px;
                        overflow-x: auto;
                    }

                    h2 {
                        color: #205b78;
                    }

                    .filters {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        margin: 20px 0;
                    }

                    .filters a, .back {
                        display: inline-block;
                        padding: 10px 16px;
                        background: #205b78;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                    }

                    .filters a:hover, .back:hover {
                        background: #123d53;
                    }

                    table {
                        width: 100%;
                        min-width: 950px;
                        border-collapse: collapse;
                    }

                    th, td {
                        padding: 12px;
                        border: 1px solid #ddd;
                        text-align: left;
                    }

                    th {
                        background: #205b78;
                        color: white;
                    }

                    tr:nth-child(even) {
                        background: #f2f6f9;
                    }

                    .empty {
                        text-align: center;
                        padding: 20px;
                    }

                    .active {
                        color: #087b38;
                        font-weight: bold;
                    }

                    .expired {
                        color: #c0392b;
                        font-weight: bold;
                    }

                    .confirmed {
                        color: #1769aa;
                        font-weight: bold;
                    }

                    .cancelled {
                        color: #777;
                        font-weight: bold;
                    }
                </style>
            </head>

            <body>

            <header>
                <h1>FlashSale Admin Panel</h1>
                <p>Reservation Management</p>
            </header>

            <div class="container">

                <h2>All Reservations</h2>

                <div class="filters">
                    <a href="reservations">All</a>
                    <a href="reservations?status=active">Active</a>
                    <a href="reservations?status=expired">Expired</a>
                    <a href="reservations?status=confirmed">Confirmed</a>
                    <a href="reservations?status=cancelled">Cancelled</a>
                </div>

                <p>Selected filter:
            """);

        out.println("<strong>" + escape(filter.toUpperCase()) +
                    "</strong></p>");

        out.println("""
                <table>
                    <tr>
                        <th>Reservation ID</th>
                        <th>Customer</th>
                        <th>Email</th>
                        <th>Product</th>
                        <th>Quantity</th>
                        <th>Reservation Date</th>
                        <th>Expiry Date</th>
                        <th>Status</th>
                        <th>Order ID</th>
                    </tr>
            """);

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            // Mark reservations past their expiry time as expired.
            String expirySQL =
                "UPDATE reservations " +
                "SET status = 'expired' " +
                "WHERE status = 'active' AND expires_at <= NOW()";

            try (PreparedStatement ps =
                     con.prepareStatement(expirySQL)) {
                ps.executeUpdate();
            }

            String sql =
                "SELECT r.reservation_id, u.name, u.email, " +
                "p.product_name, r.quantity, r.created_at, " +
                "r.expires_at, r.status, o.order_id " +
                "FROM reservations r " +
                "JOIN users u ON r.customer_id = u.user_id " +
                "JOIN products p ON r.product_id = p.product_id " +
                "LEFT JOIN orders o ON r.reservation_id = o.reservation_id ";

            if (!filter.equals("all")) {
                sql += "WHERE r.status = ? ";
            }

            sql += "ORDER BY r.created_at DESC";

            try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

                if (!filter.equals("all")) {
                    ps.setString(1, filter);
                }

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        String status = rs.getString("status");

                        out.println("<tr>");

                        out.println("<td>" +
                            rs.getInt("reservation_id") + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("name")) + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("email")) + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("product_name")) + "</td>");

                        out.println("<td>" +
                            rs.getInt("quantity") + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("created_at")) + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("expires_at")) + "</td>");

                        out.println("<td class='" + escape(status) + "'>" +
                            escape(status.toUpperCase()) + "</td>");

                        int orderId = rs.getInt("order_id");

                        if (rs.wasNull()) {
                            out.println("<td>--</td>");
                        } else {
                            out.println("<td>" + orderId + "</td>");
                        }

                        out.println("</tr>");
                    }

                    if (!found) {
                        out.println("""
                            <tr>
                                <td colspan="9" class="empty">
                                    No reservations found.
                                </td>
                            </tr>
                            """);
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            out.println("""
                <tr>
                    <td colspan="9" class="empty">
                        Unable to load reservations.
                        Check the server console.
                    </td>
                </tr>
                """);
        }

        out.println("""
                </table>

                <br>

                <a class="back" href="dashboard">
                    Back to Admin Dashboard
                </a>

                <a href="reservations">Manage Reservations</a>
                
                <a class="back" href="../logout">
                    Logout
                </a>

            </div>
            </body>
            </html>
            """);
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
