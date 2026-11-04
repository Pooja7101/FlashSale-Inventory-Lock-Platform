
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

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"admin".equals(session.getAttribute("userRole"))) {

            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            PrintWriter out = response.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>FlashSale Admin Dashboard</title>");
            out.println("<link rel='stylesheet' href='style.css'>");

            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;background:#f3f6fa;margin:0;color:#243447}");
            out.println("header{background:#245b78;color:white;padding:20px;text-align:center}");
            out.println(".container{max-width:1100px;margin:25px auto;padding:15px}");
            out.println(".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:15px}");
            out.println(".card{background:white;padding:20px;border-radius:10px;box-shadow:0 2px 8px #ddd}");
            out.println(".card h3{margin-top:0;color:#245b78}");
            out.println(".number{font-size:26px;font-weight:bold}");
            out.println(".section{background:white;padding:20px;margin-top:22px;border-radius:10px;overflow-x:auto}");
            out.println("table{width:100%;border-collapse:collapse;min-width:500px}");
            out.println("th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left}");
            out.println("th{background:#eaf1f6}");
            out.println("a{color:#245b78}");
            out.println(".logout{color:white}");
            out.println("</style>");

            out.println("</head>");
            out.println("<body>");

            out.println("<header>");
            out.println("<h1>FlashSale Admin Dashboard</h1>");
            out.println("<p>Welcome, " +
                    escape(String.valueOf(session.getAttribute("userName"))) +
                    "</p>");
            out.println("<a class='logout' href='" +
                    request.getContextPath() + "/logout'>Logout</a>");
            out.println("</header>");

            out.println("<div class='container'>");

            // Dashboard statistics.
            int customers = getCount(con,
                    "SELECT COUNT(*) FROM users WHERE role='customer'");

            int vendors = getCount(con,
                    "SELECT COUNT(*) FROM users WHERE role='vendor'");

            int products = getCount(con,
                    "SELECT COUNT(*) FROM products");

            int stock = getCount(con,
                    "SELECT COALESCE(SUM(stock),0) FROM products");

            int orders = getCount(con,
                    "SELECT COUNT(*) FROM orders");

            int activeReservations = getCount(con,
                    "SELECT COUNT(*) FROM reservations " +
                    "WHERE status='active' AND expires_at > NOW()");

            int expiredReservations = getCount(con,
                    "SELECT COUNT(*) FROM reservations " +
                    "WHERE status='expired' OR " +
                    "(status='active' AND expires_at <= NOW())");

            String sales = getSales(con);

            out.println("<div class='cards'>");

            card(out, "Customers", customers);
            card(out, "Vendors", vendors);
            card(out, "Products", products);
            card(out, "Total Stock", stock);
            card(out, "Orders", orders);
            card(out, "Active Reservations", activeReservations);
            card(out, "Expired Reservations", expiredReservations);

            out.println("<div class='card'>");
            out.println("<h3>Total Sales</h3>");
            out.println("<div class='number'>₹" + sales + "</div>");
            out.println("</div>");

            out.println("</div>");

            // Recent users.
            out.println("<div class='section'>");
            out.println("<h2>Recent Users</h2>");

            out.println("<table>");
            out.println("<tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th></tr>");

            String usersSql =
                    "SELECT user_id, name, email, role " +
                    "FROM users ORDER BY user_id DESC LIMIT 10";

            try (PreparedStatement ps = con.prepareStatement(usersSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("user_id") + "</td>");
                    out.println("<td>" + escape(rs.getString("name")) + "</td>");
                    out.println("<td>" + escape(rs.getString("email")) + "</td>");
                    out.println("<td>" + escape(rs.getString("role")) + "</td>");
                    out.println("</tr>");
                }
            }

            out.println("</table>");
            out.println("</div>");

            // Recent products.
            out.println("<div class='section'>");
            out.println("<h2>Recent Products</h2>");

            out.println("<table>");
            out.println("<tr><th>ID</th><th>Product</th><th>Vendor ID</th><th>Price</th><th>Stock</th></tr>");

            String productsSql =
                    "SELECT product_id, product_name, vendor_id, price, stock " +
                    "FROM products ORDER BY product_id DESC LIMIT 10";

            try (PreparedStatement ps = con.prepareStatement(productsSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("product_id") + "</td>");
                    out.println("<td>" + escape(rs.getString("product_name")) + "</td>");
                    out.println("<td>" + rs.getInt("vendor_id") + "</td>");
                    out.println("<td>₹" + rs.getBigDecimal("price") + "</td>");
                    out.println("<td>" + rs.getInt("stock") + "</td>");
                    out.println("</tr>");
                }
            }

            out.println("</table>");
            out.println("</div>");

            // Recent orders.
            out.println("<div class='section'>");
            out.println("<h2>Recent Orders</h2>");

            out.println("<table>");
            out.println("<tr><th>Order ID</th><th>Customer ID</th><th>Reservation ID</th><th>Amount</th><th>Date</th></tr>");

            String ordersSql =
                    "SELECT order_id, customer_id, reservation_id, " +
                    "total_amount, order_date FROM orders " +
                    "ORDER BY order_id DESC LIMIT 10";

            try (PreparedStatement ps = con.prepareStatement(ordersSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("order_id") + "</td>");
                    out.println("<td>" + rs.getInt("customer_id") + "</td>");
                    out.println("<td>" + rs.getInt("reservation_id") + "</td>");
                    out.println("<td>₹" + rs.getBigDecimal("total_amount") + "</td>");
                    out.println("<td>" + rs.getTimestamp("order_date") + "</td>");
                    out.println("</tr>");
                }
            }

            out.println("</table>");
            out.println("</div>");

            // Recent reservations.
            out.println("<div class='section'>");
            out.println("<h2>Recent Reservations</h2>");

            out.println("<table>");
            out.println("<tr><th>Reservation ID</th><th>Customer ID</th><th>Product ID</th><th>Quantity</th><th>Status</th><th>Expiry</th></tr>");

            String reservationsSql =
                    "SELECT reservation_id, customer_id, product_id, " +
                    "quantity, status, expires_at FROM reservations " +
                    "ORDER BY reservation_id DESC LIMIT 10";

            try (PreparedStatement ps = con.prepareStatement(reservationsSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("reservation_id") + "</td>");
                    out.println("<td>" + rs.getInt("customer_id") + "</td>");
                    out.println("<td>" + rs.getInt("product_id") + "</td>");
                    out.println("<td>" + rs.getInt("quantity") + "</td>");
                    out.println("<td>" + escape(rs.getString("status")) + "</td>");
                    out.println("<td>" + rs.getTimestamp("expires_at") + "</td>");
                    out.println("</tr>");
                }
            }

            out.println("</table>");
            out.println("</div>");

            out.println("</div>");
            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                    "Unable to load admin dashboard. Please check the server."
            );
        }
    }

    private int getCount(Connection con, String sql) throws Exception {

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    private String getSales(Connection con) throws Exception {

        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getBigDecimal(1).toPlainString();
            }
        }

        return "0.00";
    }

    private void card(PrintWriter out, String title, int value) {

        out.println("<div class='card'>");
        out.println("<h3>" + title + "</h3>");
        out.println("<div class='number'>" + value + "</div>");
        out.println("</div>");
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
