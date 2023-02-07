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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;

import static de.tichawa.cis.config.model.Tables.*;

public abstract class MaskController<C extends CIS> implements Initializable {
    @Getter
    private final List<CIS.Resolution> resolutions;
    @FXML
    protected ComboBox<String> Color;
    @FXML
    protected ComboBox<String> Resolution;
    @FXML
    protected ComboBox<String> ScanWidth;
    @FXML
    protected Label PixelSize;
    @FXML
    protected Label DefectSize;
    @FXML
    protected Label MaxLineRate;
    @FXML
    protected Label CurrLineRate;
    @FXML
    protected Slider SelLineRate;
    @FXML
    protected Label Speedmms;
    @FXML
    protected Label Speedms;
    @FXML
    protected Label Speedmmin;
    @FXML
    protected Label Speedips;
    @FXML
    protected ComboBox<String> InternalLightSource;
    @FXML
    protected ComboBox<String> InternalLightColor;
    @FXML
    protected ComboBox<String> ExternalLightSource;
    @FXML
    protected ComboBox<String> ExternalLightColor;
    @FXML
    protected ComboBox<String> Interface;
    @FXML
    protected ComboBox<String> Cooling;
    @FXML
    protected CheckBox Trigger;
    @FXML
    protected Label Infotext;
    @FXML
    protected Button Calculation;
    @FXML
    protected Button PartList;
    @FXML
    protected Button DataSheet;
    @FXML
    protected Button OEMMode;
    @FXML
    protected Button Equip;

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
     * handles a single calculation for a cis (there may be 2 if there is external lighting)
     *
     * @param cis        the cis to calculate
     * @param stageTitle the title of the stage
     * @param offset     the x- and y-offset of the stage (so that the user can see that there are multiple stages)
     */
    private static void handleSingleCalculation(CIS cis, String stageTitle, int offset) {
        try {
            cis.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return;
        }
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
        try {
            cis.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return;
        }
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
                controller.getProfilePic().setImage(new Image(profile));
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
        boolean isOemMode = a.getSource().equals(OEMMode);
        // for external light sources
        if (hasExternalLighting())
            handleSingleDatasheet(LDSTD_DATA, "LDSTD Datasheet", 20, new DataSheetController(), isOemMode);

        //internal light sources
        handleSingleDatasheet(CIS_DATA, CIS_DATA.cisName + " Datasheet", 0, getNewDatasheetController(), isOemMode);
    }

    @FXML
    @SuppressWarnings("unused")
    public void handlePartList(ActionEvent a) {
        CIS.CISCalculation calculation;
        try {
            calculation = CIS_DATA.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return;
        }

        FileChooser f = new FileChooser();
        f.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        f.setInitialFileName(CIS_DATA.getTiViKey() + "_partList.csv");
        File file = f.showSaveDialog(null);

        if (file != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write("Quantity,TiViKey");
                writer.newLine();
                writer.write("");
                writer.newLine();
                writer.flush();

                // sort lists by ferix key (could also add both configs to one list if desired)
                List<Entry<PriceRecord, Integer>> electList = new ArrayList<>(calculation.electConfig.entrySet());
                List<Entry<PriceRecord, Integer>> mechaList = new ArrayList<>(calculation.mechaConfig.entrySet());
                electList.sort(Comparator.comparing(e -> e.getKey().getFerixKey()));
                mechaList.sort(Comparator.comparing(m -> m.getKey().getFerixKey()));

                for (Entry<PriceRecord, Integer> entry : electList) {
                    writer.write("\"" + entry.getValue() + "\",\"" + entry.getKey().getFerixKey() + "\",\" \"");
                    writer.newLine();
                }

                for (Entry<PriceRecord, Integer> entry : mechaList) {
                    writer.write("\"" + entry.getValue() + "\",\"" + entry.getKey().getFerixKey() + "\",\" \"");
                    writer.newLine();
                }

                writer.flush();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("File saved."));
                alert.show();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("A fatal error occurred during the save attempt.Please close the target file and try again."));
                alert.show();
            }
        }
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
                    .map(record ->
                    {
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
