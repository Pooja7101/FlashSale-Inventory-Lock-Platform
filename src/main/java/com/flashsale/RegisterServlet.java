
package com.flashsale;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        if (name == null || email == null || password == null ||
                role == null || name.isBlank() || email.isBlank() ||
                password.isBlank()) {

            response.getWriter().println("Please fill all fields.");
            return;
        }

        if (!role.equals("customer") && !role.equals("vendor")) {
            response.getWriter().println("Invalid role.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(
                password, BCrypt.gensalt()
        );

        String sql = "INSERT INTO users (name, email, password, role) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.setString(4, role);

            ps.executeUpdate();

            response.setContentType("text/html");
            response.getWriter().println(
                    "<h2>Registration Successful!</h2>"
            );

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {

            response.getWriter().println(
                    "<h2>Email already registered!</h2>"
            );

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Registration failed.");
            e.printStackTrace();
        }
    }
}
