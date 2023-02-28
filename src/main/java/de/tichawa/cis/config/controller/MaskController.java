package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.ldstd.LDSTD;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Pair;
import lombok.Getter;
import org.jooq.Record;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.*;

import static de.tichawa.cis.config.model.Tables.*;

public abstract class MaskController<C extends CIS> implements Initializable {
    @Getter
    private final List<CIS.Resolution> resolutions;
    @FXML
    protected ComboBox<String> colorComboBox;
    @FXML
    protected ComboBox<String> resolutionComboBox;
    @FXML
    protected ComboBox<String> scanWidthComboBox;
    @FXML
    protected Label pixelSizeLabel;
    @FXML
    protected Label defectSizeLabel;
    @FXML
    protected Label maxLineRateLabel;
    @FXML
    protected Label currentLineRateLabel;
    @FXML
    protected Slider selectedLineRateSlider;
    @FXML
    protected Label speedmmsLabel;
    @FXML
    protected Label speedmsLabel;
    @FXML
    protected Label speedmminLabel;
    @FXML
    protected Label speedipsLabel;
    @FXML
    protected ComboBox<String> internalLightSourceComboBox;
    @FXML
    protected ComboBox<String> internalLightColorComboBox;
    @FXML
    protected ComboBox<String> externalLightSourceComboBox;
    @FXML
    protected ComboBox<String> externalLightColorComboBox;
    @FXML
    protected ComboBox<String> interfaceComboBox;
    @FXML
    protected ComboBox<String> coolingComboBox;
    @FXML
    protected CheckBox externalTriggerCheckbox;
    @FXML
    protected Label infotextLabel;
    @FXML
    protected Button oemModeButton;

    protected C CIS_DATA; // the actual cis
    protected LDSTD LDSTD_DATA; // the external lighting if there is one

    public MaskController() {
        this.resolutions = setupResolutions();
        this.LDSTD_DATA = new LDSTD();
    }

    @Override
    public abstract void initialize(URL url, ResourceBundle rb);

    public abstract List<CIS.Resolution> setupResolutions();

    /**
     * handles the calculation button press by showing the calculation window for the cis and the LDSTD if there is external lighting
     */
    @FXML
    public void handleCalculation() {
        //for external light sources
        if (hasExternalLighting())
            handleSingleCalculation(LDSTD_DATA, "LDSTD Calculation" + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"), 20);
        // for internal light sources
        handleSingleCalculation(CIS_DATA, CIS_DATA.cisName + " Calculation", 0);
    }

    /**
     * calls {@link CIS#calculate()} and shows an alert if it produces an error
     *
     * @param cis the cis where calculate gets called
     * @return the result of {@link CIS#calculate()} or null if it produced an error
     */
    private static CIS.CISCalculation calculateOrShowAlert(CIS cis) {
        try {
            return cis.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return null;
        }
    }

    /**
     * handles a single calculation for a cis (there may be 2 if there is external lighting)
     *
     * @param cis        the cis to calculate
     * @param stageTitle the title of the stage
     * @param offset     the x- and y-offset of the stage (so that the user can see that there are multiple stages)
     */
    private static void handleSingleCalculation(CIS cis, String stageTitle, int offset) {
        if (calculateOrShowAlert(cis) == null)
            return;

        // - create stage
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("Calculation.fxml", stageTitle);
        Stage stage = stageWithLoader.getKey();
        stage.show();
        if (offset != 0) {
            stage.setX(stage.getX() - offset);
            stage.setY(stage.getY() - offset);
        }
        ((CalculationController) stageWithLoader.getValue().getController()).passData(cis);
    }

    /**
     * determines whether this CIS has external lighting i.e. whether the {@link #LDSTD_DATA} has lights but the CIS is not a {@link LDSTD} object
     *
     * @return whether the CIS has external lighting
     */
    private boolean hasExternalLighting() {
        return !(CIS_DATA instanceof LDSTD) && LDSTD_DATA.getLedLines() > 0;
    }

    /**
     * handles the datasheet stage creation for a single CIS.
     *
     * @param cis        the cis for the datasheet
     * @param stageTitle the title of the datasheet stage
     * @param offset     the x- and y-offset of the stage (so that the user can see that there are multiple stages)
     * @param controller the datasheet controller for the given CIS
     * @param isOEMMode  whether the datasheet should be editable
     */
    private static void handleSingleDatasheet(CIS cis, String stageTitle, int offset, DataSheetController controller, boolean isOEMMode) {
        if (calculateOrShowAlert(cis) == null)
            return;

        // show stage
        Stage stage = Util.createNewStage("DataSheet.fxml", stageTitle, controller);
        stage.show();
        if (offset != 0) {
            stage.setX(stage.getX() - offset);
            stage.setY(stage.getY() - offset);
        }
        controller.passData(cis);

        // possibly make stage editable
        if (isOEMMode) {
            controller.setEditable();
            InputStream profile = MaskController.class.getResourceAsStream("/de/tichawa/cis/config/OEM_Profile.jpg");
            if (profile != null) {
                controller.getProfileImageView().setImage(new Image(profile));
            }
        }
    }

    /**
     * handles the datasheet button press by showing the datasheet window for the CIS and LDSTD if there is additional external lighting
     *
     * @param a the button press action event
     */
    @FXML
    public void handleDataSheet(ActionEvent a) {
        boolean isOemMode = a.getSource().equals(oemModeButton);
        // for external light sources
        if (hasExternalLighting())
            handleSingleDatasheet(LDSTD_DATA, "LDSTD Datasheet", 20, new DataSheetController(), isOemMode);

        //internal light sources
        handleSingleDatasheet(CIS_DATA, CIS_DATA.cisName + " Datasheet", 0, getNewDatasheetController(), isOemMode);
    }

    /**
     * Handles the part list button press.
     * Asks the user for an output CSV file and creates a part list there that can be used to import into Ferix via the device part list import.
     *
     * @param a the action event of the button press
     */
    @FXML
    @SuppressWarnings("unused")
    public void handlePartList(ActionEvent a) {
        // determine items to write
        CIS.CISCalculation calculation = calculateOrShowAlert(CIS_DATA);
        if (calculation == null)
            return;
        // sort lists by ferix key (could also add both configs to one list if desired)
        List<Entry<PriceRecord, Integer>> electronicList = new ArrayList<>(calculation.electConfig.entrySet());
        List<Entry<PriceRecord, Integer>> mechanicList = new ArrayList<>(calculation.mechaConfig.entrySet());
        electronicList.sort(Comparator.comparing(e -> e.getKey().getFerixKey()));
        mechanicList.sort(Comparator.comparing(m -> m.getKey().getFerixKey()));

        electronicList.addAll(mechanicList); // electronicList now contains all items

        FileChooser f = new FileChooser();
        f.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        f.setInitialFileName(CIS_DATA.getTiViKey() + "_partList.csv");
        File file = f.showSaveDialog(null);

        if (file != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charset.forName("Cp1252"))) {// ANSI for import in Ferix since it does not support UTF-8
                // write file
                //createPartlistForAltiumFerixImport(writer, electronicList); // old version
                createPartListForDevicePartlistFerixImport(writer, electronicList);

                // show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(Util.getString("File saved."));
                alert.show();
            } catch (IOException e) {
                // show error alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(Util.getString("A fatal error occurred during the save attempt.Please close the target file and try again."));
                alert.show();
            }
        }
    }

    /**
     * Creates the part list that can be used for Ferix device part list import.
     * <p>
     * The resulting csv file consists of 10 columns where most are ignored during the import (the Ferix import function was implemented for something else before).
     * The first column will set the position number inside the part list in Ferix. This method just counts it up.
     * The fifth value is used for the quantity.
     * The 9th, 10th and 2nd values can be used to set the article descriptions 1, 2 and 3 respectively. This method only sets description 1.
     *
     * @param writer   the bufferd writer already initialized with the output file
     * @param itemList a list of items to write as a part list
     * @throws IOException if an error occurs while writing the csv file
     */
    private static void createPartListForDevicePartlistFerixImport(BufferedWriter writer,
                                                                   List<Entry<PriceRecord, Integer>> itemList) throws IOException {
        String csvSeparator = ";";
        // no headline -> this import does not expect one

        // write content
        int position = 0;
        for (Entry<PriceRecord, Integer> entry : itemList) {
            Stream<String> cells = Stream.of(String.valueOf(++position), "", "", "", String.valueOf(entry.getValue()), "", "", "", entry.getKey().getFerixKey(), "");
            String line = cells.collect(Collectors.joining(csvSeparator));
            writer.write(line);
            writer.newLine();
        }
        // make sure it is written
        writer.flush();
    }

    /**
     * Creates the part list that can be used for Ferix ALTIUM import.
     * This was the old implementation and is not used currently.
     * The resulting csv file consists of 3 columns: the quantity, the ferix key and a column containing a single space
     *
     * @param writer   the buffered writer with the file already set
     * @param itemList a list of entries with the price record as key and the quantity as value
     * @throws IOException if an error occurs while writing the csv file
     */
    private static void createPartlistForAltiumFerixImport(BufferedWriter writer,
                                                           List<Entry<PriceRecord, Integer>> itemList) throws IOException {
        // write headline
        writer.write("Quantity,TiViKey");
        writer.newLine();
        writer.write("");
        writer.newLine();
        // write content
        for (Entry<PriceRecord, Integer> entry : itemList) {
            writer.write("\"" + entry.getValue() + "\",\"" + entry.getKey().getFerixKey() + "\",\" \"");
            writer.newLine();
        }
        // make sure it is written
        writer.flush();
    }

    /**
     * Returns a new controller object for the datasheet.
     * Returns a {@link DataSheetController} unless overwritten by subclass.
     *
     * @return a new {@link DataSheetController} object to be used as controller for the datasheet
     */
    protected DataSheetController getNewDatasheetController() {
        return new DataSheetController();
    }

    @FXML
    public void handleOEMMode(ActionEvent a) {
        handleDataSheet(a);
    }

    @FXML
    @SuppressWarnings("unused")
    public void handleEquipment(ActionEvent a) {
        Stage printStage = new Stage();
        printStage.setTitle("Additional equipment list");
        InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
        if (icon != null) {
            printStage.getIcons().add(new Image(icon));
        }
        GridPane printPane = new GridPane();
        printPane.getStylesheets().add("/de/tichawa/cis/config/style.css");
        printPane.getStyleClass().add("white");
        ColumnConstraints c = new ColumnConstraints();
        c.setHgrow(Priority.ALWAYS);
        printPane.getColumnConstraints().addAll(c, c);
        Scene printScene = new Scene(printPane);

        SimpleBooleanProperty pale = new SimpleBooleanProperty(false);
        Util.getDatabase().ifPresent(context -> {
            context.select(EQUIPMENT.asterisk(), PRICE.FERIX_KEY)
                    .from(EQUIPMENT.join(PRICE).on(EQUIPMENT.ART_NO.eq(PRICE.ART_NO))).stream()
                    .filter(this::isValidEquipmentRecord)
                    .map(record -> {
                        Label[] labels = new Label[2];

                        for (int i = 0; i < labels.length; i++) {
                            labels[i] = new Label();
                            Label l = labels[i];
                            l.setAlignment(Pos.CENTER_LEFT);
                            l.setMaxWidth(1000);
                            l.getStyleClass().add("bolder");
                            if (pale.get()) {
                                l.getStyleClass().add("pale-blue");
                            } else {
                                l.getStyleClass().add("white");
                            }
                        }

                        labels[0].setText(record.get(EQUIPMENT.ART_NO).toString());
                        labels[1].setText(record.get(PRICE.FERIX_KEY));

                        pale.set(!pale.get());
                        return labels;
                    }).forEach(labels -> printPane.addRow(printPane.getChildren().size() / 2, labels));
            printStage.setScene(printScene);
            printStage.show();
        });
    }

    /**
     * returns whether the CIS requires the next size housing.
     * Default is false unless overwritten by subclass
     */
    protected boolean requiresNextSizeHousing() {
        return false;
    }

    /**
     * returns the given string as a previous size or unchanged if it's not an int string
     */
    protected String getPreviousSizeHousingString(String sizeString) {
        try {
            return String.valueOf(Integer.parseInt(sizeString) - CIS.BASE_LENGTH);
        } catch (NumberFormatException e) { // not a number -> return given String
            return sizeString;
        }
    }

    /**
     * returns whether the given equipment record is valid
     *
     * @param record the equipment record that contains the select_code of the equipment table
     */
    private boolean isValidEquipmentRecord(Record record) {
        return record.get(EQUIPMENT.SELECT_CODE) == null // no select code -> it is valid
                || Arrays.stream(record.get(EQUIPMENT.SELECT_CODE).split("&")).allMatch(pred -> // all predicates must match
                pred.length() == 0  // predicate matches if it is empty
                        || (pred.startsWith("!") != CIS_DATA.getTiViKey().contains(requiresNextSizeHousing() // check if we might need next size
                        ? (getPreviousSizeHousingString(pred.replace("!", "")).equals("0") // previous size would be 0 -> don't take this (and replace with something not in tivi key like °
                        ? "°" : getPreviousSizeHousingString(pred.replace("!", ""))) // check with prev size to get next size
                        : pred.replace("!", ""))) // check if pred part of tivi key
                        || (!(CIS_DATA instanceof LDSTD) && LDSTD_DATA.getLedLines() > 0 && LDSTD_DATA.getTiViKey().contains(pred.replace("!", ""))));
    }
}
