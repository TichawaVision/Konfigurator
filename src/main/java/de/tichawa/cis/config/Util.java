package de.tichawa.cis.config;

import javafx.util.Pair;
import lombok.Getter;

import java.util.*;
import java.util.stream.*;

/**
 * class to collect all utility functions that don't really belong to another class
 */
public class Util {
    @Getter
    private static Locale locale = Locale.getDefault();

    public static <A, B> List<Pair<A, B>> zip(List<A> as, List<B> bs) {
        return IntStream.range(0, Math.min(as.size(), bs.size()))
                .mapToObj(i -> new Pair<>(as.get(i), bs.get(i)))
                .collect(Collectors.toList());
    }

    // Laden von statischen Texten aus dem Bundle (de/en)
    public static String getString(String key) {
        return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
    }

    public static void switchLanguage() {
        if (getLocale().toString().equals("de_DE")) {
            locale = new Locale("en", "US");
        } else if (getLocale().toString().equals("en_US")) {
            locale = new Locale("de", "DE");
        }
    }
}