/*
 * This file is generated by jOOQ.
 */
package de.tichawa.cis.config.model.tables;


import de.tichawa.cis.config.model.DefaultSchema;
import de.tichawa.cis.config.model.Keys;
import de.tichawa.cis.config.model.tables.records.MechanicRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row7;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Mechanic extends TableImpl<MechanicRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>mechanic</code>
     */
    public static final Mechanic MECHANIC = new Mechanic();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MechanicRecord> getRecordType() {
        return MechanicRecord.class;
    }

    /**
     * The column <code>mechanic.cis_type</code>.
     */
    public final TableField<MechanicRecord, String> CIS_TYPE = createField(DSL.name("cis_type"), SQLDataType.VARCHAR(10).nullable(false), this, "");

    /**
     * The column <code>mechanic.cis_length</code>.
     */
    public final TableField<MechanicRecord, Integer> CIS_LENGTH = createField(DSL.name("cis_length"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>mechanic.lights</code>.
     */
    public final TableField<MechanicRecord, String> LIGHTS = createField(DSL.name("lights"), SQLDataType.VARCHAR(10), this, "");

    /**
     * The column <code>mechanic.select_code</code>.
     */
    public final TableField<MechanicRecord, String> SELECT_CODE = createField(DSL.name("select_code"), SQLDataType.VARCHAR(45), this, "");

    /**
     * The column <code>mechanic.art_no</code>.
     */
    public final TableField<MechanicRecord, Integer> ART_NO = createField(DSL.name("art_no"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>mechanic.amount</code>.
     */
    public final TableField<MechanicRecord, String> AMOUNT = createField(DSL.name("amount"), SQLDataType.VARCHAR(45).nullable(false), this, "");

    /**
     * The column <code>mechanic.next_size_art_no</code>.
     */
    public final TableField<MechanicRecord, Integer> NEXT_SIZE_ART_NO = createField(DSL.name("next_size_art_no"), SQLDataType.INTEGER, this, "");

    private Mechanic(Name alias, Table<MechanicRecord> aliased) {
        this(alias, aliased, null);
    }

    private Mechanic(Name alias, Table<MechanicRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>mechanic</code> table reference
     */
    public Mechanic(String alias) {
        this(DSL.name(alias), MECHANIC);
    }

    /**
     * Create an aliased <code>mechanic</code> table reference
     */
    public Mechanic(Name alias) {
        this(alias, MECHANIC);
    }

    /**
     * Create a <code>mechanic</code> table reference
     */
    public Mechanic() {
        this(DSL.name("mechanic"), null);
    }

    public <O extends Record> Mechanic(Table<O> child, ForeignKey<O, MechanicRecord> key) {
        super(child, key, MECHANIC);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public List<ForeignKey<MechanicRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<MechanicRecord, ?>>asList(Keys.FK_MECHANIC_PRICE_1, Keys.FK_MECHANIC_PRICE_2);
    }

    private transient Price _fkMechanicPrice_1;
    private transient Price _fkMechanicPrice_2;

    public Price fkMechanicPrice_1() {
        if (_fkMechanicPrice_1 == null)
            _fkMechanicPrice_1 = new Price(this, Keys.FK_MECHANIC_PRICE_1);

        return _fkMechanicPrice_1;
    }

    public Price fkMechanicPrice_2() {
        if (_fkMechanicPrice_2 == null)
            _fkMechanicPrice_2 = new Price(this, Keys.FK_MECHANIC_PRICE_2);

        return _fkMechanicPrice_2;
    }

    @Override
    public Mechanic as(String alias) {
        return new Mechanic(DSL.name(alias), this);
    }

    @Override
    public Mechanic as(Name alias) {
        return new Mechanic(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Mechanic rename(String name) {
        return new Mechanic(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Mechanic rename(Name name) {
        return new Mechanic(name, null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<String, Integer, String, String, Integer, String, Integer> fieldsRow() {
        return (Row7) super.fieldsRow();
    }
}
