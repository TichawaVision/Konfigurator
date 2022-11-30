package de.tichawa.cis.config;

import javafx.util.Pair;
import lombok.Getter;

import java.util.*;
import java.util.stream.*;

/**
 * class to collect all utility functions that don't really belong to another class.
 * Handles language selection for the GUI
 */
public class Util {
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
        if (getLocale().toString().equals("de_DE")) {
            locale = new Locale("en", "US");
        } else if (getLocale().toString().equals("en_US")) {
            locale = new Locale("de", "DE");
        }
    }
}