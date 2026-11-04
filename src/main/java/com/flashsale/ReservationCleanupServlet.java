
package com.flashsale;

import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ReservationCleanupServlet
        implements ServletContextListener {

    private java.util.concurrent.ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        scheduler = java.util.concurrent.Executors
                .newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {

            String sql = "UPDATE reservations "
                       + "SET status='expired' "
                       + "WHERE status='active' "
                       + "AND expires_at <= NOW()";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                int count = ps.executeUpdate();

                if (count > 0) {
                    System.out.println(
                        count + " reservations expired.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
