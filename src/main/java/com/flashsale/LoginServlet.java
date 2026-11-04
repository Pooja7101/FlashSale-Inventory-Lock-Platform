
package com.flashsale;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null ||
                email.trim().isEmpty() || password.isEmpty()) {

            response.sendRedirect("login.html");
            return;
        }

        String sql =
                "SELECT user_id, name, email, password, role " +
                "FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new Exception("Database connection failed.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, email.trim());

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        String hashedPassword = rs.getString("password");

                        if (BCrypt.checkpw(password, hashedPassword)) {

                            HttpSession session = request.getSession(true);

                            session.setAttribute("userId", rs.getInt("user_id"));
                            session.setAttribute("userName", rs.getString("name"));
                            session.setAttribute("userRole", rs.getString("role"));

                            session.setMaxInactiveInterval(30 * 60);

                            String role = rs.getString("role");

                            if ("admin".equals(role)) {
                                response.sendRedirect("admin/dashboard");
                            } else if ("vendor".equals(role)) {
                                response.sendRedirect("vendor/products");
                            } else {
                                response.sendRedirect("dashboard.html");
                            }

                            return;
                        }
                    }
                }
            }

            response.sendRedirect("login.html");

        } catch (Exception e) {

            e.printStackTrace();

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println(
                    "<h3>Login failed due to a server error. Please try again.</h3>"
            );
        }
    }
}
