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
import java.util.stream.*;

/**
 * class to collect all utility functions that don't really belong to another class.
 * Handles language selection for the GUI
 */
public class Util {

    private static final Properties DB_PROPERTIES;
    private static final BasicDataSource DATA_SOURCE;

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

    @Getter
    private static Locale locale = Locale.getDefault(); // start with default language

    /**
     * combines two lists into a single list of pairs.
     * Returns a list of {@link Pair} objects with the i-th element of both given lists.
     * If the lists have different sizes the resulting list has the smaller size.
     */
    public static <A, B> List<Pair<A, B>> zip(List<A> as, List<B> bs) {
        return IntStream.range(0, Math.min(as.size(), bs.size()))
                .mapToObj(i -> new Pair<>(as.get(i), bs.get(i)))
                .collect(Collectors.toList());
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
     * switches languages (DE <-> EN)
     */
    public static void switchLanguage() {
        if (locale == Locale.GERMANY)
            locale = Locale.US;
        else
            locale = Locale.GERMANY;
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
            throw new IllegalStateException("fxml file could not be loaded: " + fxmlRelativeUrl);
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
}