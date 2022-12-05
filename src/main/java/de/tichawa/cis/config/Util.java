package de.tichawa.cis.config;

import javafx.util.Pair;
import lombok.Getter;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.*;
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
        } catch (IOException ignored) {
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
     */
    public static String getString(String key) {
        return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
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
}