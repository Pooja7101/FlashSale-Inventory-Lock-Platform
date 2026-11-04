
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

@WebServlet("/vendor/products")
public class VendorProductServlet extends HttpServlet {

    private Integer getVendorId(HttpServletRequest request,
                                HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"vendor".equals(session.getAttribute("userRole")) ||
                !(session.getAttribute("userId") instanceof Integer)) {

            response.sendRedirect(request.getContextPath() + "/login.html");
            return null;
        }

        return (Integer) session.getAttribute("userId");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        Integer vendorId = getVendorId(request, response);

        if (vendorId == null) {
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
            out.println("<title>Vendor Inventory</title>");
            out.println("<link rel='stylesheet' href='../style.css'>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>FlashSale - Vendor Dashboard</h1>");
            out.println("<h2>Manage Your Products</h2>");

            out.println("<p>Welcome, " +
                    escape(String.valueOf(
                            request.getSession(false).getAttribute("userName")
                    )) +
                    "</p>");

            out.println("<h3>Add New Product</h3>");

            out.println("<form method='post' action='" +
                    request.getContextPath() + "/vendor/products'>");

            out.println("<input type='hidden' name='action' value='add'>");

            out.println("Product Name: <input type='text' name='productName' required><br><br>");

            out.println("Description: <textarea name='description'></textarea><br><br>");

            out.println("Price: <input type='number' name='price' min='0.01' step='0.01' required><br><br>");

            out.println("Stock: <input type='number' name='stock' min='0' required><br><br>");

            out.println("<button type='submit'>Add Product</button>");
            out.println("</form>");

            out.println("<hr>");
            out.println("<h3>Your Products</h3>");

            String sql =
                    "SELECT product_id, product_name, description, price, stock " +
                    "FROM products WHERE vendor_id = ? ORDER BY product_id DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, vendorId);

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        int productId = rs.getInt("product_id");
                        String productName = rs.getString("product_name");
                        String description = rs.getString("description");
                        BigDecimal price = rs.getBigDecimal("price");
                        int stock = rs.getInt("stock");

                        out.println("<div style='border:1px solid #ccc;padding:15px;margin:15px 0;'>");

                        out.println("<h3>" + escape(productName) + "</h3>");
                        out.println("<p>" + escape(description) + "</p>");
                        out.println("<p>Product ID: " + productId + "</p>");

                        out.println("<form method='post' action='" +
                                request.getContextPath() + "/vendor/products'>");

                        out.println("<input type='hidden' name='action' value='update'>");
                        out.println("<input type='hidden' name='productId' value='" +
                                productId + "'>");

                        out.println("Price: <input type='number' name='price' min='0.01' step='0.01' value='" +
                                price + "' required><br><br>");

                        out.println("Stock: <input type='number' name='stock' min='0' value='" +
                                stock + "' required><br><br>");

                        out.println("<button type='submit'>Update Product</button>");
                        out.println("</form>");

                        out.println("<br>");

                        out.println("<form method='post' action='" +
                                request.getContextPath() + "/vendor/products' " +
                                "onsubmit=\"return confirm('Are you sure you want to delete this product?');\">");

                        out.println("<input type='hidden' name='action' value='delete'>");

                        out.println("<input type='hidden' name='productId' value='" +
                                productId + "'>");

                        out.println("<button type='submit'>Delete Product</button>");
                        out.println("</form>");

                        out.println("</div>");
                    }

                    if (!found) {
                        out.println("<p>You have not added any products yet.</p>");
                    }
                }
            }

            out.println("<br><a href='" +
                    request.getContextPath() + "/logout'>Logout</a>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                    "Unable to load vendor products. Please try again."
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        Integer vendorId = getVendorId(request, response);

        if (vendorId == null) {
            return;
        }

        String action = request.getParameter("action");

        if (action == null) {
            showMessage(response, "Invalid request.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            switch (action) {

                case "add":
                    addProduct(request, con, vendorId);
                    showMessage(response, "Product added successfully!");
                    break;

                case "update":
                    updateProduct(request, response, con, vendorId);
                    break;

                case "delete":
                    deleteProduct(request, response, con, vendorId);
                    break;

                default:
                    showMessage(response, "Invalid action.");
            }

        } catch (IllegalArgumentException e) {

            showMessage(response, e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(response, "Operation failed. Please try again.");
        }
    }

    private void addProduct(HttpServletRequest request,
                            Connection con,
                            int vendorId) throws Exception {

        String name = request.getParameter("productName");
        String description = request.getParameter("description");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }

        BigDecimal price = new BigDecimal(request.getParameter("price"));
        int stock = Integer.parseInt(request.getParameter("stock"));

        if (price.compareTo(BigDecimal.ZERO) <= 0 || stock < 0) {
            throw new IllegalArgumentException(
                    "Enter a valid price and stock."
            );
        }

        String sql =
                "INSERT INTO products " +
                "(vendor_id, product_name, description, price, stock) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, vendorId);
            ps.setString(2, name.trim());
            ps.setString(3, description == null ? "" : description.trim());
            ps.setBigDecimal(4, price);
            ps.setInt(5, stock);

            ps.executeUpdate();
        }
    }

    private void updateProduct(HttpServletRequest request,
                               HttpServletResponse response,
                               Connection con,
                               int vendorId) throws Exception {

        int productId = Integer.parseInt(
                request.getParameter("productId")
        );

        BigDecimal price = new BigDecimal(
                request.getParameter("price")
        );

        int newStock = Integer.parseInt(
                request.getParameter("stock")
        );

        if (productId <= 0 ||
                price.compareTo(BigDecimal.ZERO) <= 0 ||
                newStock < 0) {

            throw new IllegalArgumentException(
                    "Enter valid product details."
            );
        }

        con.setAutoCommit(false);

        try {

            String lockSql =
                    "SELECT product_id FROM products " +
                    "WHERE product_id = ? AND vendor_id = ? FOR UPDATE";

            try (PreparedStatement ps = con.prepareStatement(lockSql)) {

                ps.setInt(1, productId);
                ps.setInt(2, vendorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Product not found or you do not own this product."
                        );
                    }
                }
            }

            String expireSql =
                    "UPDATE reservations SET status = 'expired' " +
                    "WHERE product_id = ? AND status = 'active' " +
                    "AND expires_at <= NOW()";

            try (PreparedStatement ps = con.prepareStatement(expireSql)) {

                ps.setInt(1, productId);
                ps.executeUpdate();
            }

            int reserved = 0;

            String reservedSql =
                    "SELECT COALESCE(SUM(quantity), 0) AS total " +
                    "FROM reservations WHERE product_id = ? " +
                    "AND status = 'active' AND expires_at > NOW()";

            try (PreparedStatement ps = con.prepareStatement(reservedSql)) {

                ps.setInt(1, productId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        reserved = rs.getInt("total");
                    }
                }
            }

            if (newStock < reserved) {
                throw new IllegalArgumentException(
                        "Stock cannot be less than currently reserved quantity: " +
                                reserved
                );
            }

            String updateSql =
                    "UPDATE products SET price = ?, stock = ? " +
                    "WHERE product_id = ? AND vendor_id = ?";

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                ps.setBigDecimal(1, price);
                ps.setInt(2, newStock);
                ps.setInt(3, productId);
                ps.setInt(4, vendorId);

                ps.executeUpdate();
            }

            con.commit();

        } catch (Exception e) {

            con.rollback();
            throw e;

        } finally {

            con.setAutoCommit(true);
        }

        showMessage(response, "Product updated successfully!");
    }

    private void deleteProduct(HttpServletRequest request,
                               HttpServletResponse response,
                               Connection con,
                               int vendorId) throws Exception {

        int productId = Integer.parseInt(
                request.getParameter("productId")
        );

        if (productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID.");
        }

        con.setAutoCommit(false);

        try {

            String lockSql =
                    "SELECT product_id FROM products " +
                    "WHERE product_id = ? AND vendor_id = ? FOR UPDATE";

            try (PreparedStatement ps = con.prepareStatement(lockSql)) {

                ps.setInt(1, productId);
                ps.setInt(2, vendorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Product not found or you do not own this product."
                        );
                    }
                }
            }

            String historySql =
                    "SELECT COUNT(*) AS total FROM reservations " +
                    "WHERE product_id = ?";

            try (PreparedStatement ps = con.prepareStatement(historySql)) {

                ps.setInt(1, productId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next() && rs.getInt("total") > 0) {
                        throw new IllegalArgumentException(
                                "This product has reservation history and cannot be deleted."
                        );
                    }
                }
            }

            String deleteSql =
                    "DELETE FROM products WHERE product_id = ? AND vendor_id = ?";

            try (PreparedStatement ps = con.prepareStatement(deleteSql)) {

                ps.setInt(1, productId);
                ps.setInt(2, vendorId);

                if (ps.executeUpdate() == 0) {
                    throw new IllegalArgumentException(
                            "Product could not be deleted."
                    );
                }
            }

            con.commit();

        } catch (Exception e) {

            con.rollback();
            throw e;

        } finally {

            con.setAutoCommit(true);
        }

        showMessage(response, "Product deleted successfully!");
    }

    private void showMessage(HttpServletResponse response,
                             String message) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Vendor Inventory</title></head>");
        out.println("<body>");

        out.println("<h2>" + escape(message) + "</h2>");

        out.println("<a href='" +
                getServletContext().getContextPath() +
                "/vendor/products'>Back to Vendor Dashboard</a>");

        out.println("</body>");
        out.println("</html>");
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
