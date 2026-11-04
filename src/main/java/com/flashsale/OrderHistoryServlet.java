
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

@WebServlet("/orders")
public class OrderHistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Only logged-in customers can view their orders.
        if (session == null ||
            !"customer".equals(session.getAttribute("userRole"))) {

            response.sendRedirect(
                request.getContextPath() + "/login.html"
            );
            return;
        }

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            response.sendRedirect(
                request.getContextPath() + "/login.html"
            );
            return;
        }

        int customerId = ((Number) userIdObject).intValue();

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">
                <title>My Order History - FlashSale</title>

                <link rel="stylesheet"
                      href="style.css">

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
                        max-width: 1100px;
                        margin: 35px auto;
                        padding: 25px;
                        background: white;
                        border-radius: 10px;
                        overflow-x: auto;
                    }

                    h2 {
                        color: #205b78;
                    }

                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                        min-width: 650px;
                    }

                    th, td {
                        padding: 13px;
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
                        padding: 25px;
                    }

                    .back {
                        display: inline-block;
                        background: #205b78;
                        color: white;
                        padding: 11px 18px;
                        text-decoration: none;
                        border-radius: 5px;
                        margin-top: 20px;
                    }

                    .back:hover {
                        background: #123d53;
                    }

                    .error {
                        color: #c0392b;
                        padding: 15px;
                    }
                </style>
            </head>

            <body>

                <header>
                    <h1>FlashSale</h1>
                    <p>My Order History</p>
                </header>

                <div class="container">
                    <h2>My Confirmed Orders</h2>
            """);

        String sql = """
            SELECT
                o.order_id,
                o.total_amount,
                o.order_date,
                r.quantity,
                p.product_name
            FROM orders o
            JOIN reservations r
                ON o.reservation_id = r.reservation_id
            JOIN products p
                ON r.product_id = p.product_id
            WHERE o.customer_id = ?
            ORDER BY o.order_date DESC
            """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {

                    out.println("""
                        <table>
                            <tr>
                                <th>Order ID</th>
                                <th>Product Name</th>
                                <th>Quantity</th>
                                <th>Total Amount</th>
                                <th>Order Date</th>
                            </tr>
                        """);

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        out.println("<tr>");

                        out.println("<td>" +
                            rs.getInt("order_id") + "</td>");

                        out.println("<td>" +
                            escape(rs.getString("product_name")) +
                            "</td>");

                        out.println("<td>" +
                            rs.getInt("quantity") + "</td>");

                        out.println("<td>₹" +
                            rs.getBigDecimal("total_amount") +
                            "</td>");

                        out.println("<td>" +
                            escape(rs.getString("order_date")) +
                            "</td>");

                        out.println("</tr>");
                    }

                    if (!found) {
                        out.println("""
                            <tr>
                                <td colspan="5" class="empty">
                                    You have not placed any confirmed
                                    orders yet.
                                </td>
                            </tr>
                            """);
                    }

                    out.println("</table>");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            out.println("""
                <p class="error">
                    Unable to load your order history.
                    Please try again later.
                </p>
                """);
        }

        out.println("""
                    <a class="back" href="dashboard.html">
                        Back to Dashboard
                    </a>

                    <a class="back" href="logout">
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
