/*
 * This file is generated by jOOQ.
 */
package de.tichawa.cis.config.model.tables;


import de.tichawa.cis.config.model.DefaultSchema;
import de.tichawa.cis.config.model.Keys;
import de.tichawa.cis.config.model.tables.records.SensorBoardRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorBoard extends TableImpl<SensorBoardRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>sensor_board</code>
     */
    public static final SensorBoard SENSOR_BOARD = new SensorBoard();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SensorBoardRecord> getRecordType() {
        return SensorBoardRecord.class;
    }

    /**
     * The column <code>sensor_board.name</code>.
     */
    public final TableField<SensorBoardRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(45).nullable(false), this, "");

    /**
     * The column <code>sensor_board.chips</code>.
     */
    public final TableField<SensorBoardRecord, Integer> CHIPS = createField(DSL.name("chips"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sensor_board.orientation</code>.
     */
    public final TableField<SensorBoardRecord, Integer> ORIENTATION = createField(DSL.name("orientation"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sensor_board.lines</code>.
     */
    public final TableField<SensorBoardRecord, Integer> LINES = createField(DSL.name("lines"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sensor_board.line_spacing_odd_even</code>.
     */
    public final TableField<SensorBoardRecord, Integer> LINE_SPACING_ODD_EVEN = createField(DSL.name("line_spacing_odd_even"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sensor_board.line_spacing_one_two</code>.
     */
    public final TableField<SensorBoardRecord, Integer> LINE_SPACING_ONE_TWO = createField(DSL.name("line_spacing_one_two"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sensor_board.length</code>.
     */
    public final TableField<SensorBoardRecord, Double> LENGTH = createField(DSL.name("length"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>sensor_board.stagged</code>.
     */
    public final TableField<SensorBoardRecord, Byte> STAGGED = createField(DSL.name("stagged"), SQLDataType.TINYINT.nullable(false), this, "");

    /**
     * The column <code>sensor_board.overlap</code>.
     */
    public final TableField<SensorBoardRecord, Integer> OVERLAP = createField(DSL.name("overlap"), SQLDataType.INTEGER.nullable(false), this, "");

    private SensorBoard(Name alias, Table<SensorBoardRecord> aliased) {
        this(alias, aliased, null);
    }

    private SensorBoard(Name alias, Table<SensorBoardRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>sensor_board</code> table reference
     */
    public SensorBoard(String alias) {
        this(DSL.name(alias), SENSOR_BOARD);
    }

    /**
     * Create an aliased <code>sensor_board</code> table reference
     */
    public SensorBoard(Name alias) {
        this(alias, SENSOR_BOARD);
    }

    /**
     * Create a <code>sensor_board</code> table reference
     */
    public SensorBoard() {
        this(DSL.name("sensor_board"), null);
    }

    public <O extends Record> SensorBoard(Table<O> child, ForeignKey<O, SensorBoardRecord> key) {
        super(child, key, SENSOR_BOARD);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<SensorBoardRecord> getPrimaryKey() {
        return Keys.PK_SENSOR_BOARD;
    }

    @Override
    public List<UniqueKey<SensorBoardRecord>> getKeys() {
        return Arrays.<UniqueKey<SensorBoardRecord>>asList(Keys.PK_SENSOR_BOARD);
    }

    @Override
    public SensorBoard as(String alias) {
        return new SensorBoard(DSL.name(alias), this);
    }

    @Override
    public SensorBoard as(Name alias) {
        return new SensorBoard(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public SensorBoard rename(String name) {
        return new SensorBoard(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SensorBoard rename(Name name) {
        return new SensorBoard(name, null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<String, Integer, Integer, Integer, Integer, Integer, Double, Byte, Integer> fieldsRow() {
        return (Row9) super.fieldsRow();
    }
}
