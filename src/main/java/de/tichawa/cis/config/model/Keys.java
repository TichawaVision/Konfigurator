/*
 * This file is generated by jOOQ.
 */
package de.tichawa.cis.config.model;


import de.tichawa.cis.config.model.tables.*;
import de.tichawa.cis.config.model.tables.records.*;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in 
 * tivicc.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<PriceRecord> KEY_PRICE_FERIX_KEY_UNIQUE = Internal.createUniqueKey(Price.PRICE, DSL.name("KEY_price_ferix_key_UNIQUE"), new TableField[] { Price.PRICE.FERIX_KEY }, true);
    public static final UniqueKey<PriceRecord> KEY_PRICE_PRIMARY = Internal.createUniqueKey(Price.PRICE, DSL.name("KEY_price_PRIMARY"), new TableField[] { Price.PRICE.ART_NO }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<ElectronicRecord, PriceRecord> FK_ELECTRONIC_ART_NO = Internal.createForeignKey(Electronic.ELECTRONIC, DSL.name("fk_electronic_art_no"), new TableField[] { Electronic.ELECTRONIC.ART_NO }, Keys.KEY_PRICE_PRIMARY, new TableField[] { Price.PRICE.ART_NO }, true);
    public static final ForeignKey<MechanicRecord, PriceRecord> FK_MECHANIC_ART_NO = Internal.createForeignKey(Mechanic.MECHANIC, DSL.name("fk_mechanic_art_no"), new TableField[] { Mechanic.MECHANIC.ART_NO }, Keys.KEY_PRICE_PRIMARY, new TableField[] { Price.PRICE.ART_NO }, true);
    public static final ForeignKey<EquipmentRecord, PriceRecord> FK_EQUIPMENT_ART_NO = Internal.createForeignKey(Equipment.EQUIPMENT, DSL.name("fk_equipment_art_no"), new TableField[] { Equipment.EQUIPMENT.ART_NO }, Keys.KEY_PRICE_PRIMARY, new TableField[] { Price.PRICE.ART_NO }, true);
}
