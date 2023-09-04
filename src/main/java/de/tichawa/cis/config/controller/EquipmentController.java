package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.ldstd.LDSTD;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.*;
import javafx.scene.control.*;
import lombok.AllArgsConstructor;
import org.jooq.Record;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static de.tichawa.cis.config.model.Tables.*;

/**
 * Controller for Equipment.fxml to handle additional equipment for the CIS
 */
public class EquipmentController implements Initializable {

    private CIS cisData;
    @FXML
    private TableView<EquipmentEntry> equipmentTableView;
    private CIS ldstdData;
    private String neededCameraLinkCable;
    private String neededPowerCable;
    private String neededPowerSupply;
    private String neededTriggerCable;

    /**
     * returns the given string as a previous size or unchanged if it's not an int string
     */
    private static String getPreviousSizeHousingString(String sizeString) {
        try {
            return String.valueOf(Integer.parseInt(sizeString) - CIS.BASE_LENGTH);
        } catch (NumberFormatException e) { // not a number -> return given String
            return sizeString;
        }
    }

    /**
     * Initializes the controller by setting up the list view
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        equipmentTableView.getColumns().clear();
        TableColumn<EquipmentEntry, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(e -> new ReadOnlyObjectWrapper<>(e.getValue().description));
        TableColumn<EquipmentEntry, String> ferixKeyColumn = new TableColumn<>("Ferix Key");
        ferixKeyColumn.setCellValueFactory(e -> new ReadOnlyObjectWrapper<>(e.getValue().ferixKey));
        TableColumn<EquipmentEntry, String> articleNumberFerixOldColumn = new TableColumn<>("FerixAlt");
        articleNumberFerixOldColumn.setCellValueFactory(e -> new ReadOnlyObjectWrapper<>(String.valueOf(e.getValue().articleNumberFerixOld)));
        TableColumn<EquipmentEntry, String> articleNumberFerixNewColumn = new TableColumn<>("FerixNeu");
        articleNumberFerixNewColumn.setCellValueFactory(e -> new ReadOnlyObjectWrapper<>(e.getValue().articleNumberFerixNew == 0 ? "-" : String.valueOf(e.getValue().articleNumberFerixNew))); // 0 stands for null value
        equipmentTableView.getColumns().addAll(descriptionColumn, ferixKeyColumn, articleNumberFerixOldColumn, articleNumberFerixNewColumn);
        equipmentTableView.getSortOrder().add(ferixKeyColumn);
    }

    /**
     * returns whether the given equipment record is valid
     *
     * @param record the equipment record that contains the select_code of the equipment table
     */
    private boolean isValidEquipmentRecord(Record record) {
        return record.get(EQUIPMENT.SELECT_CODE) == null // no select code -> it is valid
                || Arrays.stream(record.get(EQUIPMENT.SELECT_CODE).split("\\|")).map(String::trim).anyMatch(option -> // any option must match
                Arrays.stream(option.split("&")).map(String::trim).allMatch(this::isValidPredicate));
    }

    /**
     * Returns whether the given positive (= does not start with "!") predicate is valid.
     * An empty predicate is always valid.
     * If the TiVi key contains the predicate, it is valid.
     * If there is external lighting and the {@link LDSTD}'s TiVi Key contains the predicate, it is valid.
     * If the predicate equals any of the user selected configuration, it is valid.
     * Otherwise, it is not valid.
     *
     * @param predicate the predicate to check
     * @return true if the predicate could be matched or false otherwise
     */
    private boolean isValidPositivePredicate(String predicate) {
        return predicate == null || predicate.length() == 0 || //predicate matches if it is empty
                cisData.getTiViKey().contains( // predicate also matches if it is part of the TiVi key
                        cisData.requiresNextSizeHousing() ? // check if we might need next size housing
                                // yes, need next size -> the predicate must match the previous size -> if 0 skip this entry and replace with something not in the TiVi key (like °)
                                (getPreviousSizeHousingString(predicate).equals("0") ?
                                        "°" : getPreviousSizeHousingString(predicate)) :
                                predicate // no, no next size needed -> just use the predicate without changes
                ) ||
                // predicate also matches if there is external lighting (ldstd has lights) and the TiVi key of the ldstd contains the predicate
                (!(cisData instanceof LDSTD) && ldstdData.getLedLines() > 0 && ldstdData.getTiViKey().contains(predicate)) ||
                predicate.equals(neededPowerSupply) || // predicate also matches if it is the selected power supply by the user
                predicate.equals(neededPowerCable) || // predicate also matches if it is the selected power cable by the user
                predicate.equals(neededCameraLinkCable) || // predicate also matches if it is the selected camera cable by the user
                predicate.equals(neededTriggerCable); // predicate also matches if it is the selected trigger cable by the user
    }

    /**
     * Returns whether the given predicate is valid.
     * An empty predicate is always valid. If the predicate starts with an "!" it is negated. Uses {@link #isValidPositivePredicate(String)} to check remaining conditions.
     *
     * @param predicate the predicate to check
     * @return true if the predicate could be matched or false otherwise
     */
    private boolean isValidPredicate(String predicate) {
        return predicate == null
                || (predicate.startsWith("!") ? !isValidPositivePredicate(predicate.replace("!", "")) : isValidPositivePredicate(predicate));
    }

    /**
     * Initializes the attributes with the given data (that is chosen via the {@link EquipmentSelectionController})
     *
     * @param cisData               the CIS that needs additional equipment
     * @param ldstdData             the {@link LDSTD} that comes with the CIS
     * @param neededPowerSupply     the select code of the chosen power supply
     * @param neededPowerCable      the select code of the chosen power cable
     * @param neededCameraLinkCable the select code of the camera link cable
     * @param neededTriggerCable    the select code of the trigger cable
     */
    public void passData(CIS cisData, CIS ldstdData, String neededPowerSupply, String neededPowerCable, String neededCameraLinkCable, String neededTriggerCable) {

        this.cisData = cisData;
        this.ldstdData = ldstdData;
        this.neededPowerSupply = neededPowerSupply;
        this.neededPowerCable = neededPowerCable;
        this.neededCameraLinkCable = neededCameraLinkCable;
        this.neededTriggerCable = neededTriggerCable;

        equipmentTableView.getItems().clear();

        Util.getDatabase().ifPresent(context -> equipmentTableView.getItems().addAll(
                context.select(EQUIPMENT.asterisk(), PRICE.FERIX_KEY)
                        .from(EQUIPMENT.join(PRICE).on(EQUIPMENT.ART_NO_FERIX_OLD.eq(PRICE.ART_NO))).stream()
                        .filter(this::isValidEquipmentRecord)
                        .map(record -> new EquipmentEntry(
                                record.get(EQUIPMENT.ART_NO_FERIX_OLD),
                                record.get(EQUIPMENT.ART_NO_FERIX_NEW) == null ? 0 : record.get(EQUIPMENT.ART_NO_FERIX_NEW),
                                record.get(PRICE.FERIX_KEY),
                                record.get(EQUIPMENT.DESCRIPTION)))
                        .collect(Collectors.toSet())));
        equipmentTableView.sort();
    }

    /**
     * POJO for equipment entries. This is used as type parameter by the {@link #equipmentTableView}.
     */
    @AllArgsConstructor
    public static class EquipmentEntry {
        public final int articleNumberFerixNew;
        public final int articleNumberFerixOld;
        public final String description;
        public final String ferixKey;
    }
}
