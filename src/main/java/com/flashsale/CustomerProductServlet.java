
package com.flashsale;

import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/products")
public class CustomerProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"customer".equals(session.getAttribute("userRole"))) {
            response.sendRedirect("login.html");
            return;
        }

        String search = request.getParameter("search");

        if (search == null) {
            search = "";
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
            out.println("<title>FlashSale Products</title>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<link rel='stylesheet' href='style.css'>");

            out.println("<style>");
            out.println("body{font-family:Arial;background:#f4f7fb;margin:30px;color:#243447}");
            out.println(".product{background:white;padding:20px;margin:15px 0;border-radius:10px;border:1px solid #ddd}");
            out.println("input,button{padding:9px;margin:5px;border-radius:5px;border:1px solid #bbb}");
            out.println("button{background:#286b8a;color:white;cursor:pointer}");
            out.println(".available{color:green;font-weight:bold}");
            out.println(".unavailable{color:red;font-weight:bold}");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>FlashSale</h1>");
            out.println("<h2>Available Products</h2>");

            out.println("<form method='get' action='products'>");
            out.println("<input type='text' name='search' placeholder='Search products' value='" +
                    escape(search) + "'>");
            out.println("<button type='submit'>Search</button>");
            out.println("<a href='products'>Show All</a>");
            out.println("</form>");

            out.println("<hr>");

            String sql =
                    "SELECT p.product_id, p.product_name, p.description, " +
                    "p.price, p.stock, " +
                    "COALESCE(SUM(CASE WHEN r.status = 'active' " +
                    "AND r.expires_at > NOW() THEN r.quantity ELSE 0 END), 0) " +
                    "AS reserved " +
                    "FROM products p " +
                    "LEFT JOIN reservations r ON p.product_id = r.product_id " +
                    "WHERE p.product_name LIKE ? OR p.description LIKE ? " +
                    "GROUP BY p.product_id, p.product_name, p.description, p.price, p.stock " +
                    "ORDER BY p.product_id DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, "%" + search + "%");
                ps.setString(2, "%" + search + "%");

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        int productId = rs.getInt("product_id");
                        String productName = rs.getString("product_name");
                        String description = rs.getString("description");
                        BigDecimal price = rs.getBigDecimal("price");

                        int stock = rs.getInt("stock");
                        int reserved = rs.getInt("reserved");

                        int available = Math.max(0, stock - reserved);

                        out.println("<div class='product'>");

                        out.println("<h3>" + escape(productName) + "</h3>");
                        out.println("<p>" + escape(description) + "</p>");

                        out.println("<p><b>Price:</b> ₹" + price + "</p>");
                        out.println("<p><b>Total Stock:</b> " + stock + "</p>");
                        out.println("<p><b>Reserved:</b> " + reserved + "</p>");

                        out.println("<p><b>Available Stock:</b> " + available + "</p>");

                        if (available > 0) {

                            out.println("<p class='available'>Available</p>");

                            out.println("<form method='post' action='reserve'>");

                            out.println("<input type='hidden' name='productId' value='" +
                                    productId + "'>");

                            out.println("<label>Quantity:</label>");

                            out.println("<input type='number' name='quantity' min='1' max='" +
                                    available + "' value='1' required>");

                            out.println("<button type='submit'>Reserve Now</button>");

                            out.println("</form>");

                        } else {

                            out.println("<p class='unavailable'>Currently Unavailable</p>");
                        }

                        out.println("</div>");
                    }

                    if (!found) {
                        out.println("<p>No products found.</p>");
                    }
                }
            }

            out.println("<br><a href='dashboard.html'>Back to Dashboard</a>");
            out.println("<br><br><a href='logout'>Logout</a>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                    "Unable to load products. Please try again."
            );
        }
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
