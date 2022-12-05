package de.tichawa.cis.config;

import de.tichawa.cis.config.model.Tables;
import de.tichawa.cis.config.model.tables.Price;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import de.tichawa.util.Tuple;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LauncherController implements Initializable {
    protected CIS CIS_DATA;
    @FXML
    private ComboBox<String> selectCIS;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    @FXML
    @SuppressWarnings("unused")
    private void handleContinue(ActionEvent a) {
        try {
            URL mask = getClass().getResource("/de/tichawa/cis/config/" + selectCIS.getSelectionModel().getSelectedItem().toLowerCase() + "/Mask.fxml");
            if (mask != null) {
                Parent root = FXMLLoader.load(mask);
                Scene scene = new Scene(root);
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle(selectCIS.getSelectionModel().getSelectedItem() + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"));
                InputStream icon = getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png");
                if (icon != null) {
                    stage.getIcons().add(new Image(icon));
                }
                stage.centerOnScreen();
                stage.show();
            }
        } catch (IOException ignored) {
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleUpdate(ActionEvent a) {
        try {
            Desktop.getDesktop().open(Launcher.ferixHome.resolve("Priceexport.lnk").toFile());

            if (new Alert(AlertType.CONFIRMATION, "Please wait until FERIX has finished the export to " + Launcher.ferixHome.resolve("Export/art.csv") + ", then press OK to continue.",
                    ButtonType.OK, ButtonType.CANCEL).showAndWait()
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {
                Map<Integer, Tuple<Double, Boolean>> sourcePrices = Files.readAllLines(Launcher.ferixHome.resolve("Export/art.csv")).stream()
                        .map(line -> line.split("\t"))
                        .filter(line -> CIS.isInteger(line[0]) && CIS.isDouble(line[1]))
                        .map(line -> new Tuple<>(Integer.parseInt(line[0]), new Tuple<>(Double.parseDouble(line[1]) / CIS.decodeQuantity(line[2]), line.length < 4 || line[3].equals("A"))))
                        .filter(t -> t.getV().getU() > 0)
                        .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));

                Util.getDatabase()
                        .ifPresent(database ->
                        {
                            List<Double> oldPrices = database.selectFrom(Tables.PRICE)
                                    .where(Tables.PRICE.ART_NO.in(sourcePrices.keySet()))
                                    .orderBy(Tables.PRICE.ART_NO.asc())
                                    .fetch(Tables.PRICE.PRICE_);

                            database.batchUpdate(sourcePrices.entrySet().stream()
                                            .map(e ->
                                            {
                                                PriceRecord record = new PriceRecord();
                                                record.setArtNo(e.getKey());
                                                record.setPrice(e.getValue().getU());
                                                record.changed(Tables.PRICE.ART_NO, false);
                                                return record;
                                            }).collect(Collectors.toList()))
                                    .execute();

                            List<Double> newPrices = database.selectFrom(Tables.PRICE)
                                    .where(Tables.PRICE.ART_NO.in(sourcePrices.keySet()))
                                    .orderBy(Tables.PRICE.ART_NO.asc())
                                    .fetch(Tables.PRICE.PRICE_);

                            int updateCount = 0;
                            for (int x = 0; x < newPrices.size(); x++) {
                                if (!newPrices.get(x).equals(oldPrices.get(x))) {
                                    updateCount++;
                                }
                            }

                            new Alert(Alert.AlertType.CONFIRMATION, "Update successful. " + updateCount + " entries updated.")
                                    .showAndWait();

                            Set<Integer> components = database.selectFrom(Price.PRICE)
                                    .fetchSet(Price.PRICE.ART_NO);

                            List<String> inactiveComponents = sourcePrices.entrySet().stream()
                                    .filter(e -> !e.getValue().getV())
                                    .map(Map.Entry::getKey)
                                    .filter(components::contains)
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                            if (!inactiveComponents.isEmpty()) {
                                Alert inactiveAlert = new Alert(AlertType.WARNING);
                                inactiveAlert.setHeaderText("Some components are inactive now. Please consider replacing the components with these IDs:");
                                inactiveAlert.setContentText(String.join("\n", inactiveComponents));
                                inactiveAlert.showAndWait();
                            }
                        });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
