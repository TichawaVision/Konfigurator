package de.tichawa.cis.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

/**
 * class to collect all utility functions that don't really belong to another class.
 * Handles language selection for the GUI
 */
public class Util {

    private static final BasicDataSource DATA_SOURCE;
    private static final Properties DB_PROPERTIES;
    @Getter
    private static Locale locale = Locale.getDefault(); // start with default language

    static {
        // setup database connection
        DB_PROPERTIES = new Properties();
        DATA_SOURCE = new BasicDataSource();
        String DATABASE_FOLDER = "./Database/"; // in Database subfolder of folder where the jar is
        try {
            DB_PROPERTIES.loadFromXML(new FileInputStream(DATABASE_FOLDER + "connection.xml"));

            switch (DB_PROPERTIES.getProperty("dbType")) {
                case "SQLite":
                    DATA_SOURCE.setUrl("jdbc:sqlite:" + DATABASE_FOLDER + DB_PROPERTIES.getProperty("dbFile"));
                    break;
                case "MariaDB":
                    DATA_SOURCE.setUrl("jdbc:mariadb://" + DB_PROPERTIES.getProperty("dbHost") + ":" + DB_PROPERTIES.getProperty("dbPort")
                            + "/" + DB_PROPERTIES.getProperty("dbName"));
                    DATA_SOURCE.setUsername(DB_PROPERTIES.getProperty("dbUser"));
                    DATA_SOURCE.setPassword(DB_PROPERTIES.getProperty("dbPwd"));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            System.err.println("exception while reading database connection: " + e);
        }
    }

    /**
     * Creates a new stage with the given title by loading the given fxml file
     *
     * @param fxmlRelativeUrl the relative url (to de.tichawa.cis.config) to the fxml file
     * @param title           the title of the created stage
     * @return the {@link Stage} object that was created
     */
    public static Stage createNewStage(String fxmlRelativeUrl, String title) {
        return createNewStageWithLoader(fxmlRelativeUrl, title).getKey();
    }

    /**
     * Creates a new stage with the given title by loading the given fxml file while setting the given controller
     *
     * @param fxmlRelativeUrl the relative url (to de.tichawa.cis.config) to the fxml file
     * @param title           the title of the created stage
     * @param controller      the controller object to be set for the {@link FXMLLoader}. Will not be set if null is given.
     * @return the {@link Stage} object that was created
     */
    public static Stage createNewStage(String fxmlRelativeUrl, String title, Object controller) {
        return createNewStageWithLoader(fxmlRelativeUrl, title, controller).getKey();
    }

    /**
     * Creates a new stage with the given title by loading the given fxml file
     *
     * @param fxmlRelativeUrl the relative url (to de.tichawa.cis.config) to the fxml file
     * @param title           the title of the created stage
     * @param controller      the controller object to be set for the {@link FXMLLoader}. Will not be set if null is given.
     * @return a pair of the {@link Stage} object that was created and the loader
     */
    public static Pair<Stage, FXMLLoader> createNewStageWithLoader(String fxmlRelativeUrl, String title, Object controller) {
        URL fxml = Util.class.getResource("/de/tichawa/cis/config/" + fxmlRelativeUrl);
        if (fxml == null)
            throw new IllegalStateException("fxml file not found: " + fxmlRelativeUrl);
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(fxml);
            if (controller != null)
                loader.setController(controller);
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
            stage.centerOnScreen();
            // load image
            InputStream icon = Util.class.getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
            return new Pair<>(stage, loader);
        } catch (IOException e) {
            throw new IllegalStateException("fxml file could not be loaded: " + fxmlRelativeUrl + " - " + e);
        }
    }

    /**
     * Creates a new stage with the given title by loading the given fxml file
     *
     * @param fxmlRelativeUrl the relative url (to de.tichawa.cis.config) to the fxml file
     * @param title           the title of the created stage
     * @return a pair of the {@link Stage} object that was created and the loader
     */
    public static Pair<Stage, FXMLLoader> createNewStageWithLoader(String fxmlRelativeUrl, String title) {
        return createNewStageWithLoader(fxmlRelativeUrl, title, null);
    }

    /**
     * returns the database connection
     *
     * @return an optional of a {@link DSLContext} database connection object
     */
    public static Optional<DSLContext> getDatabase() {
        try {
            SQLDialect dialect;
            switch (DB_PROPERTIES.getProperty("dbType")) {
                case "SQLite":
                    dialect = SQLDialect.SQLITE;
                    break;
                case "MariaDB":
                    dialect = SQLDialect.MARIADB;
                    break;
                default:
                    dialect = SQLDialect.MYSQL;
            }
            DSLContext context = DSL.using(DATA_SOURCE, dialect);

            return Optional.of(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Converts the given number to string via {@link String#format(Locale, String, Object...)}
     *
     * @param number   the number to format
     * @param decimals the number of output decimals
     * @return a {@link String} object representing the number in {@link Locale#US}
     */
    public static String getNumberAsOutputString(double number, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", number);
    }

    /**
     * returns the value depending on the currently selected language for the given key.
     *
     * @param key       the key in the resource bundle
     * @param arguments the arguments for {@link MessageFormat#format(String, Object...)} method that is used internally to format the value String for the given key
     */
    public static String getString(String key, Object... arguments) {
        return MessageFormat.format(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key), arguments);
    }

    /**
     * Checks whether the given String is a double number
     *
     * @param s the String to test
     * @return true if s represents a double number i.e.
     * - s is not null and
     * - s optionally matches a sign
     * - followed by at least one digit
     * - optionally followed by decimal point or comma
     * - optionally followed by an arbitrary amount of digits
     * or false otherwise
     */
    public static boolean isDouble(String s) {
        return s != null && s.matches("[-+]?\\d+[.,]?\\d*");
    }

    /**
     * Checks whether the given String is an integer number
     *
     * @param s the String to test
     * @return true if s represents an integer number i.e.
     * - s is not null and
     * - s optionally matches a sign
     * - followed by at least one digit
     * or false otherwise
     */
    public static boolean isInteger(String s) {
        return s != null && s.matches("[-+]?\\d+");
    }

    /**
     * Rounds the given number value by the given digits
     *
     * @param value  the number value to round
     * @param digits the number of digits after the decimal point for rounding
     * @return the rounded number
     */
    public static double round(double value, int digits) {
        return Math.round(Math.pow(10.0, digits) * value) / Math.pow(10.0, digits);
    }

    /**
     * switches languages (DE <-> EN)
     */
    public static void switchLanguage() {
        if (locale == Locale.GERMANY)
            locale = Locale.US;
        else
            locale = Locale.GERMANY;
    }
}