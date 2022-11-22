package de.tichawa.cis.config;

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
import lombok.Getter;

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

    protected C CIS_DATA;
    protected LDSTD LDSTD_DATA;

    public MaskController() {
        this.resolutions = setupResolutions();
        this.LDSTD_DATA = new LDSTD();
    }

    @Override
    public abstract void initialize(URL url, ResourceBundle rb);

    public abstract List<CIS.Resolution> setupResolutions();

    @FXML
    @SuppressWarnings("unused")
    public void handleCalculation(ActionEvent a) {
        //for external light sources
        if (!(CIS_DATA instanceof LDSTD) && LDSTD_DATA.getLedLines() > 0) {
            try {
                LDSTD_DATA.calculate();
            } catch (CISException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(e.getMessage());
                alert.show();
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/Calculation.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle("LDSTD Calculation" + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"));
                InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
                if (icon != null) {
                    stage.getIcons().add(new Image(icon));
                }
                stage.centerOnScreen();
                stage.show();
                stage.setX(stage.getX() - 20);
                stage.setY(stage.getY() - 20);

                CalculationController controller = loader.getController();
                controller.passData(LDSTD_DATA);
            } catch (IOException ignored) {
            }
        }

        // for internal light sources
        try {
            CIS_DATA.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/Calculation.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(CIS_DATA.cisName + " Calculation");
            InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
            stage.centerOnScreen();
            stage.show();
            CalculationController controller = loader.getController();
            controller.passData(CIS_DATA);
        } catch (IOException ignored) {
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void handlePartList(ActionEvent a) {
        try {
            CIS_DATA.calculate();
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
                List<Entry<PriceRecord, Integer>> electList = new ArrayList<>(CIS_DATA.getElectConfig().entrySet());
                List<Entry<PriceRecord, Integer>> mechaList = new ArrayList<>(CIS_DATA.getMechaConfig().entrySet());
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
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("File saved."));
                alert.show();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the save attempt.Please close the target file and try again."));
                alert.show();
            }
        }
    }

    @FXML
    public void handleDataSheet(ActionEvent a) {
        // for external light sources
        if (!(CIS_DATA instanceof LDSTD) && LDSTD_DATA.getLedLines() > 0) {
            try {
                LDSTD_DATA.calculate();
            } catch (CISException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(e.getMessage());
                alert.show();
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/DataSheet.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle("LDSTD Datasheet");
                InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
                if (icon != null) {
                    stage.getIcons().add(new Image(icon));
                }
                stage.centerOnScreen();
                stage.show();
                stage.setX(stage.getX() - 20);
                stage.setY(stage.getY() - 20);

                DataSheetController controller = loader.getController();
                controller.passData(LDSTD_DATA);

                if (a.getSource().equals(OEMMode)) {
                    controller.setEditable();
                    InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/OEM_Profile.jpg");
                    if (profile != null) {
                        controller.getProfilePic().setImage(new Image(profile));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        //internal light sources
        try {
            CIS_DATA.calculate();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/DataSheet.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(CIS_DATA.cisName + " Datasheet");
            InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
            stage.centerOnScreen();
            stage.show();

            DataSheetController controller = loader.getController();
            controller.passData(CIS_DATA);

            if (a.getSource().equals(OEMMode)) {
                controller.setEditable();
                InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/OEM_Profile.jpg");
                if (profile != null) {
                    controller.getProfilePic().setImage(new Image(profile));
                }
            }
        } catch (IOException ignored) {
        }
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
        CIS_DATA.getDatabase().ifPresent(context ->
        {
            context.select(EQUIPMENT.asterisk(), PRICE.FERIX_KEY)
                    .from(EQUIPMENT.join(PRICE).on(EQUIPMENT.ART_NO.eq(PRICE.ART_NO))).stream()
                    .filter(record -> record.get(EQUIPMENT.SELECT_CODE) == null
                            || Arrays.stream(record.get(EQUIPMENT.SELECT_CODE).split("&"))
                            .allMatch(pred -> pred.length() == 0
                                    || (pred.startsWith("!") != CIS_DATA.getTiViKey().contains(pred.replace("!", "")))
                                    || (!(CIS_DATA instanceof LDSTD) && LDSTD_DATA.getLedLines() > 0 && LDSTD_DATA.getTiViKey().contains(pred.replace("!", "")))))
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
}
