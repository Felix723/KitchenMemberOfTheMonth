package introdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * This is a simple web application for a coffee shop that allows users to register, log in,
 * view products, make purchases, and view purchase history. It uses Javalin as the web server
 * and connects to DuckDB over JDBC for data storage.
 */
public class App {

    // DuckDB connection string for local file-based DB
    private static final String CONNECTION = "jdbc:duckdb:./coffee.db";
    private static final int HIGH_VALUE = 50;
    private static final int MEDIUM_VALUE = 30;
    private static final int STANDARD_VALUE =10;
    private static final int LOW_VALUE = 5;
    private static final int WILD_CARD = 55;

    private record Product(String task_category, String description, int points) {}
    private record Purchase(String userName, int points, String awardTime) {}

    /**
     * Main entry point. Starts the Javalin web server and sets up all routes.
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/");
        }).start(8080);

        app.get("/", ctx -> ctx.redirect("/index.html"));

        app.post("/login", App::handleLogin);
        app.get("/logout", App::handleLogout);
        app.get("/products", App::renderProductsPage);
        app.post("/purchase", App::handlePurchase);
        app.get("/purchases", App::renderPurchasesPage);
        app.get("/my-purchases", App::renderMyPurchasesPage);
        app.post("/register", App::handleRegister);
    }

    // --- SQL Query Methods ---
    /**
     * Checks if a user exists with the given username and password.
     * @param username The username to check
     * @param password The password to check
     * @return true if user exists and password matches, false otherwise
     */
    private static boolean loginUser(String username, String password) {
       try (Connection conn = DriverManager.getConnection(CONNECTION);
           PreparedStatement ps = conn.prepareStatement("select * from user where username = ? and password = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new user into the database.
     * @param username The username for the new user
     * @param email The email for the new user
     * @param password The password for the new user
     * @return true if insertion was successful, false otherwise
     */
    private static boolean insertUser(String username, String email, String password) {
        try (Connection conn = DriverManager.getConnection(CONNECTION);
             PreparedStatement ps = conn.prepareStatement(
                     "insert into user values (?, ?, ?)");) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all products from the database.
     * @return List of Product records
     */
    private static List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(CONNECTION);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select task_category, description, points from product")) {
            while (rs.next()) {
                String productname = rs.getString("task_category");
                String description = rs.getString("description");
                int price = rs.getInt("points");
                products.add(new Product(productname, description, price));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    private static int MapValueToPoints(String value){
        return switch (value.toLowerCase()) {
            case "high_value" -> HIGH_VALUE;
            case "medium_value" -> MEDIUM_VALUE;
            case "standard_value" -> STANDARD_VALUE;
            case "low_value" -> LOW_VALUE;
            case "wild_card" -> WILD_CARD;
            default -> -9999;
        };
    }

    /**
     * Retrieves all purchases for a specific user, ordered by time descending.
     * @param username The username whose purchases to retrieve
     * @return List of Purchase records for the user
     */
    private static int getUserPoints(String username) {
        int points = -1;
        String currentTime = Instant.now().toString();
        System.out.println("current time: " + currentTime);

        String start = currentTime.substring(0, 7);
        System.out.println("start: " + start);

        String startOfMonth = start + "-01 00:01";

        String month = currentTime.substring(5, 7);
        System.out.println("month: " + month);

        int monthInt = Integer.parseInt(month);

        int year = Integer.parseInt(currentTime.substring(0, 4));
        System.out.println("year: " + year);

        YearMonth obj = YearMonth.of(year, monthInt);

        System.out.println("start: " + start);

        int days = obj.lengthOfMonth();

        String endOfMonth = start + "-" + days + " 23:59";

        System.out.println("start: " + startOfMonth + "end: " +endOfMonth);

        try (Connection conn = DriverManager.getConnection(CONNECTION);
             PreparedStatement ps = conn.prepareStatement(
                     "select points from purchase " +
                             "where username = ? AND awardTime > ? AND awardTime < ?");) {
            ps.setString(1, username);
            ps.setString(2, startOfMonth);
            ps.setString(3, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Adding points "  + rs.getInt("points") + " for " + username);
                    points += rs.getInt("points");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    /**
     * Inserts a new purchase for a user and product.
     * @param username The username making the purchase
     * @param value The point value awarded
     * @return true if insertion was successful, false otherwise
     */
    private static boolean insertPoints(String username, String value) {
        int points = MapValueToPoints(value);
        try (Connection conn = DriverManager.getConnection(CONNECTION);
             PreparedStatement ps = conn.prepareStatement(
                     "insert into purchase values (?, ?, ?)");) {
            ps.setString(1, Instant.now().toString());
            ps.setInt(2, points);
            ps.setString(3, username);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all purchases from the database, ordered by time descending.
     * @return List of Purchase records
     */
    private static List<Purchase> getAllPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(CONNECTION);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select username, points, awardTime from purchase order by awardTime desc")) {
            while (rs.next()) {
                String username = rs.getString("username");
                int points = rs.getInt("points");
                String awardTime = rs.getString("awardTime");
                purchases.add(new Purchase(username, points, awardTime));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return purchases;
    }



    // --- Page Rendering Methods ---
    /**
     * Renders the products page as HTML and sends it to the client.
     * @param ctx The Javalin context for the request
     */
    private static void renderProductsPage(Context ctx) {
        String username = ctx.sessionAttribute("username");
        StringBuilder html = new StringBuilder();
        html.append(header("Products", username));
        html.append("<main><h2>Products</h2>");
        html.append("<ul class='product-list'>");
        for (Product product : getProducts()) {
            String task_category = product.task_category();
            String desc = product.description();
            int price = product.points();
            html.append("<li class='product'>")
                .append("<h3>").append(task_category).append(" - DKK ").append(price).append("</h3>")
                .append("<p>").append(desc).append("</p>");
            if (username != null) {
                html.append("<form method='post' action='/purchase'>")
                    .append("<input type='hidden' name='task_category' value='").append(task_category).append("'>")
                    .append("<button type='submit'>Record</button>")
                    .append("</form>");
            } else {
                html.append("<p><em>Login to claim</em></p>");
            }
            html.append("</li>");
        }
        html.append("</ul>");
        html.append("</main>");
        html.append(footer());
        ctx.html(html.toString());
    }

    /**
     * Renders the all purchases page as HTML and sends it to the client.
     * @param ctx The Javalin context for the request
     */
    private static void renderPurchasesPage(Context ctx) {
        String username = ctx.sessionAttribute("username");
        StringBuilder html = new StringBuilder();
        html.append(header("All Purchases", username));
        html.append("<main><h2>All Purchases</h2>");
        html.append("<table class='purchases'><thead><tr><th>User</th><th>Product</th><th>Time</th></tr></thead><tbody>");
        for (Purchase purchase : getAllPurchases()) {
            html.append("<tr>")
                .append("<td>").append(purchase.userName()).append("</td>")
                .append("<td>").append(purchase.points()).append("</td>")
                .append("<td>").append(purchase.awardTime()).append("</td>")
                .append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("</main>");
        html.append(footer());
        ctx.html(html.toString());
    }

    /**
     * Renders the current user's purchases page as HTML and sends it to the client.
     * @param ctx The Javalin context for the request
     */
    private static void renderMyPurchasesPage(Context ctx) {
        String username = ctx.sessionAttribute("username");
        if (username == null) {
            ctx.status(401).result("You must be logged in to view your purchases.");
            return;
        }
        StringBuilder html = new StringBuilder();
        html.append(header("My Purchases", username));
        html.append("<main><h2>My Purchases</h2>");
        html.append("<table class='purchases'><thead><tr><th>Product</th><th>Time</th></tr></thead><tbody>");
        html.append("<b>").append(getUserPoints(username)).append("</b>");
        html.append("</tbody></table>");
        html.append("</main>");
        html.append(footer());
        ctx.html(html.toString());
    }

    // --- Form Handlers ---
    /**
     * Handles login form submission, authenticates user, and sets session.
     * @param ctx The Javalin context for the request
     */
    private static void handleLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        if (username == null || password == null) {
            ctx.status(400).result("Missing username or password");
            return;
        }
        if (loginUser(username, password)) {
            ctx.sessionAttribute("username", username);
            ctx.redirect("/products");
        } else {
            ctx.status(401).result("Invalid username/password");
        }
    }

    /**
     * Handles logout by clearing session and redirecting to index.
     * @param ctx The Javalin context for the request
     */
    private static void handleLogout(Context ctx) {
        ctx.sessionAttribute("username", null);
        ctx.redirect("/index.html");
    }

    /**
     * Handles registration form submission, creates user, and sets session.
     * @param ctx The Javalin context for the request
     */
    private static void handleRegister(Context ctx) {
        String username = ctx.formParam("username");
        String email = ctx.formParam("email");
        String password = ctx.formParam("password");
        if (username == null || email == null || password == null) {
            ctx.status(400).result("Missing username, email, or password");
            return;
        }
        if (insertUser(username, email, password)) {
            ctx.sessionAttribute("username", username);
            ctx.redirect("/products");
        } else {
            ctx.status(500).result("Registration failed");
        }
    }

    /**
     * Handles purchase form submission, inserts purchase, and redirects.
     * @param ctx The Javalin context for the request
     */
    private static void handlePurchase(Context ctx) {
        String username = ctx.sessionAttribute("username");
        if (username == null) {
            ctx.status(401).result("You must be logged in to purchase.");
            return;
        }
        String productname = ctx.formParam("task_category");
        if (productname == null || productname.isBlank()) {
            ctx.status(400).result("No product specified.");
            return;
        }
        if (insertPoints(username, productname)) {
            ctx.redirect("/my-purchases");
        } else {
            ctx.status(500).result("Purchase failed");
        }
    }

    // small HTML header/footer helpers
    /**
     * Generates the HTML header for all pages.
     * @param title The page title
     * @param username The current logged-in username (may be null)
     * @return HTML header string
     */
    private static String header(String title, String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
          .append("<title>").append(title).append("</title>")
          .append("<link rel='stylesheet' href='style.css'>")
          .append("<link rel='icon' type='image/png' href='icon.png'>")
          .append("<header><h1>").append(title).append("</h1><nav>");
        sb.append("<a href='/'>Home</a> | <a href='/products'>Products</a> | <a href='/purchases'>All Purchases</a>");
        if (username == null) {
            sb.append(" | <a href='/login.html'>Login</a> | <a href='/register.html'>Register</a>");
        } else {
            sb.append(" | <a href='/my-purchases'>My Purchases</a> | <a href='/logout'>Logout (").append(username).append(")</a>");
        }
        sb.append("</nav></header>");
        return sb.toString();
    }

    /**
     * Generates the HTML footer for all pages.
     * @return HTML footer string
     */
    private static String footer() {
        return "<footer><p>&copy; Introduction to Database Systems - Coffee Shop</p></footer></body></html>";
    }
}
