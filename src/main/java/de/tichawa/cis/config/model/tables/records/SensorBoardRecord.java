/*
 * This file is generated by jOOQ.
 */
package de.tichawa.cis.config.model.tables.records;


import de.tichawa.cis.config.model.tables.SensorBoard;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorBoardRecord extends UpdatableRecordImpl<SensorBoardRecord> implements Record9<String, Integer, Integer, Integer, Integer, Integer, Double, Byte, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>sensor_board.name</code>.
     */
    public void setName(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>sensor_board.name</code>.
     */
    public String getName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>sensor_board.chips</code>.
     */
    public void setChips(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>sensor_board.chips</code>.
     */
    public Integer getChips() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>sensor_board.orientation</code>.
     */
    public void setOrientation(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>sensor_board.orientation</code>.
     */
    public Integer getOrientation() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>sensor_board.lines</code>.
     */
    public void setLines(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>sensor_board.lines</code>.
     */
    public Integer getLines() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>sensor_board.line_spacing_odd_even</code>.
     */
    public void setLineSpacingOddEven(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>sensor_board.line_spacing_odd_even</code>.
     */
    public Integer getLineSpacingOddEven() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>sensor_board.line_spacing_one_two</code>.
     */
    public void setLineSpacingOneTwo(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>sensor_board.line_spacing_one_two</code>.
     */
    public Integer getLineSpacingOneTwo() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>sensor_board.length</code>.
     */
    public void setLength(Double value) {
        set(6, value);
    }

    /**
     * Getter for <code>sensor_board.length</code>.
     */
    public Double getLength() {
        return (Double) get(6);
    }

    /**
     * Setter for <code>sensor_board.stagged</code>.
     */
    public void setStagged(Byte value) {
        set(7, value);
    }

    /**
     * Getter for <code>sensor_board.stagged</code>.
     */
    public Byte getStagged() {
        return (Byte) get(7);
    }

    /**
     * Setter for <code>sensor_board.overlap</code>.
     */
    public void setOverlap(Integer value) {
        set(8, value);
    }

    /**
     * Getter for <code>sensor_board.overlap</code>.
     */
    public Integer getOverlap() {
        return (Integer) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<String, Integer, Integer, Integer, Integer, Integer, Double, Byte, Integer> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<String, Integer, Integer, Integer, Integer, Integer, Double, Byte, Integer> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return SensorBoard.SENSOR_BOARD.NAME;
    }

    @Override
    public Field<Integer> field2() {
        return SensorBoard.SENSOR_BOARD.CHIPS;
    }

    @Override
    public Field<Integer> field3() {
        return SensorBoard.SENSOR_BOARD.ORIENTATION;
    }

    @Override
    public Field<Integer> field4() {
        return SensorBoard.SENSOR_BOARD.LINES;
    }

    @Override
    public Field<Integer> field5() {
        return SensorBoard.SENSOR_BOARD.LINE_SPACING_ODD_EVEN;
    }

    @Override
    public Field<Integer> field6() {
        return SensorBoard.SENSOR_BOARD.LINE_SPACING_ONE_TWO;
    }

    @Override
    public Field<Double> field7() {
        return SensorBoard.SENSOR_BOARD.LENGTH;
    }

    @Override
    public Field<Byte> field8() {
        return SensorBoard.SENSOR_BOARD.STAGGED;
    }

    @Override
    public Field<Integer> field9() {
        return SensorBoard.SENSOR_BOARD.OVERLAP;
    }

    @Override
    public String component1() {
        return getName();
    }

    @Override
    public Integer component2() {
        return getChips();
    }

    @Override
    public Integer component3() {
        return getOrientation();
    }

    @Override
    public Integer component4() {
        return getLines();
    }

    @Override
    public Integer component5() {
        return getLineSpacingOddEven();
    }

    @Override
    public Integer component6() {
        return getLineSpacingOneTwo();
    }

    @Override
    public Double component7() {
        return getLength();
    }

    @Override
    public Byte component8() {
        return getStagged();
    }

    @Override
    public Integer component9() {
        return getOverlap();
    }

    @Override
    public String value1() {
        return getName();
    }

    @Override
    public Integer value2() {
        return getChips();
    }

    @Override
    public Integer value3() {
        return getOrientation();
    }

    @Override
    public Integer value4() {
        return getLines();
    }

    @Override
    public Integer value5() {
        return getLineSpacingOddEven();
    }

    @Override
    public Integer value6() {
        return getLineSpacingOneTwo();
    }

    @Override
    public Double value7() {
        return getLength();
    }

    @Override
    public Byte value8() {
        return getStagged();
    }

    @Override
    public Integer value9() {
        return getOverlap();
    }

    @Override
    public SensorBoardRecord value1(String value) {
        setName(value);
        return this;
    }

    @Override
    public SensorBoardRecord value2(Integer value) {
        setChips(value);
        return this;
    }

    @Override
    public SensorBoardRecord value3(Integer value) {
        setOrientation(value);
        return this;
    }

    @Override
    public SensorBoardRecord value4(Integer value) {
        setLines(value);
        return this;
    }

    @Override
    public SensorBoardRecord value5(Integer value) {
        setLineSpacingOddEven(value);
        return this;
    }

    @Override
    public SensorBoardRecord value6(Integer value) {
        setLineSpacingOneTwo(value);
        return this;
    }

    @Override
    public SensorBoardRecord value7(Double value) {
        setLength(value);
        return this;
    }

    @Override
    public SensorBoardRecord value8(Byte value) {
        setStagged(value);
        return this;
    }

    @Override
    public SensorBoardRecord value9(Integer value) {
        setOverlap(value);
        return this;
    }

    @Override
    public SensorBoardRecord values(String value1, Integer value2, Integer value3, Integer value4, Integer value5, Integer value6, Double value7, Byte value8, Integer value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SensorBoardRecord
     */
    public SensorBoardRecord() {
        super(SensorBoard.SENSOR_BOARD);
    }

    /**
     * Create a detached, initialised SensorBoardRecord
     */
    public SensorBoardRecord(String name, Integer chips, Integer orientation, Integer lines, Integer lineSpacingOddEven, Integer lineSpacingOneTwo, Double length, Byte stagged, Integer overlap) {
        super(SensorBoard.SENSOR_BOARD);

        setName(name);
        setChips(chips);
        setOrientation(orientation);
        setLines(lines);
        setLineSpacingOddEven(lineSpacingOddEven);
        setLineSpacingOneTwo(lineSpacingOneTwo);
        setLength(length);
        setStagged(stagged);
        setOverlap(overlap);
    }
}
