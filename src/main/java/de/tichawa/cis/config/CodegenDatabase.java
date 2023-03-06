package de.tichawa.cis.config;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

/**
 * Utility class to codegen the jooq database classes
 */
public class CodegenDatabase {
    private static final String DATABASE_FOLDER = "./Database/";
    private static final String DATABASE_FILENAME = "tivicc.sqlite";

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration()
                .withJdbc(new Jdbc()
                        .withDriver("org.sqlite.JDBC")
                        .withUrl("jdbc:sqlite:" + DATABASE_FOLDER + DATABASE_FILENAME)
                ).withGenerator(new Generator()
                        .withDatabase(new org.jooq.meta.jaxb.Database()
                                .withName("org.jooq.meta.sqlite.SQLiteDatabase")
                                .withIncludes(".*"))
                        .withGenerate(new Generate())
                        .withTarget(new Target()
                                .withPackageName("de.tichawa.cis.config.model")
                                .withDirectory("U:\\SW\\PC\\Java\\Konfigurator\\src\\main\\java")));
        GenerationTool.generate(configuration);
    }
}
