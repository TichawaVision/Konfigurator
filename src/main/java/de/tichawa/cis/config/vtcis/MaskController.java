package de.tichawa.cis.config.vtcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.ldstd.LDSTD;
import de.tichawa.cis.config.serial.SerialController;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.net.URL;
import java.util.*;

public class MaskController extends de.tichawa.cis.config.controller.MaskController<VTCIS> {

    @FXML
    private ChoiceBox<String> cameraLinkModeChoiceBox;

    public MaskController() {
        CIS_DATA = new VTCIS();
        LDSTD_DATA = new LDSTD();
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return Arrays.asList(
                new CIS.Resolution(1200, 1200, true, 0.25, 0.02115),
                new CIS.Resolution(1200, 1200, false, 0.5, 0.02115),
                new CIS.Resolution(600, 600, false, 1.0, 0.0423),
                new CIS.Resolution(400, 1200, false, 1.0, 0.0635),
                new CIS.Resolution(300, 300, false, 1.5, 0.0847),
                new CIS.Resolution(200, 600, false, 2.0, 0.125),
                new CIS.Resolution(150, 300, false, 3.0, 0.167),
                new CIS.Resolution(100, 300, false, 4.0, 0.25),
                new CIS.Resolution(75, 300, false, 6.0, 0.339),
                new CIS.Resolution(50, 300, false, 8.0, 0.5),
                new CIS.Resolution(25, 300, false, 10.0, 1.0));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        CIS_DATA.setPhaseCount(1);
        CIS_DATA.setDiffuseLightSources(1);
        CIS_DATA.setCoaxLightSources(0);
        CIS_DATA.setLightColor(CIS.LightColor.RED);
        CIS_DATA.setSelectedResolution(getResolutions().get(0));
        CIS_DATA.setScanWidth(1040);
        CIS_DATA.setExternalTrigger(false);
        CIS_DATA.setCooling(CIS.Cooling.LICO);

        phasesChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (newValue.contains("Three phases (RGB)") &&
                    CIS_DATA.getScanWidth() > 1300) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("RGB not available with the selected scan width.");
                alert.show();
                phasesChoiceBox.getSelectionModel().select(oldValue);
                return;
            }

            switch (newValue) {
                case "One phase (Monochrome)": {
                    CIS_DATA.setPhaseCount(1);
                    break;
                }
                case "Two phases": {
                    CIS_DATA.setPhaseCount(2);
                    break;
                }
                case "Three phases (RGB)": {
                    CIS_DATA.setPhaseCount(3);
                    break;
                }
                case "Four phases": {
                    CIS_DATA.setPhaseCount(4);
                    break;
                }
                case "Five phases": {
                    CIS_DATA.setPhaseCount(5);
                    break;
                }
                case "Six phases": {
                    CIS_DATA.setPhaseCount(6);
                    break;
                }
            }
            if ((CIS_DATA.getPhaseCount() == 4 && CIS_DATA.getScanWidth() > 1040) || (CIS_DATA.getPhaseCount() == 5 && CIS_DATA.getScanWidth() > 780) ||
                    (CIS_DATA.getPhaseCount() == 6 && CIS_DATA.getScanWidth() > 520)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not available with the selected scan width.Please reduce it.");
                alert.show();
                phasesChoiceBox.getSelectionModel().select(oldValue);
                return;
            }
            if (newValue.equals("Three phases (RGB)")) {
                internalLightColorChoiceBox.getSelectionModel().selectFirst();
                externalLightColorChoiceBox.getSelectionModel().selectFirst();
            }
            internalLightColorChoiceBox.setDisable(newValue.equals("Three phases (RGB)") || CIS_DATA.getLedLines() == 0);
            externalLightColorChoiceBox.setDisable(newValue.equals("Three phases (RGB)") || externalLightSourceChoiceBox.getSelectionModel().getSelectedIndex() == 0);

            maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000.0 + " kHz");
            selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
            selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            LDSTD_DATA.setPhaseCount(CIS_DATA.getPhaseCount());
        });
        resolutionChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedResolution(getResolutions().get(resolutionChoiceBox.getSelectionModel().getSelectedIndex()));

            maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000.0 + " kHz");
            selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
            selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            pixelSizeLabel.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
            defectSizeLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        scanWidthChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(' ')).trim());

            if ((sw > 1300 && CIS_DATA.getPhaseCount() == 3) || (sw > 1040 && CIS_DATA.getPhaseCount() == 4) ||
                    (sw > 780 && CIS_DATA.getPhaseCount() == 5) || (sw > 520 && CIS_DATA.getPhaseCount() == 6)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("The selected scan width is not available");
                alert.show();
                scanWidthChoiceBox.getSelectionModel().select(oldValue);
                return;
            }
            if (sw >= 1300 && internalLightSourceChoiceBox.getSelectionModel().getSelectedItem().contains("Coax")) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("The selected scan width is not available");
                alert.show();
                scanWidthChoiceBox.getSelectionModel().select(oldValue);
                return;
            }
            CIS_DATA.setScanWidth(sw);
            LDSTD_DATA.setScanWidth(CIS_DATA.getScanWidth());
        });
        selectedLineRateSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedLineRate(newValue.intValue());

            currentLineRateLabel.setText(newValue.intValue() / 1000.0 + " kHz");

            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        internalLightSourceChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            switch (internalLightSourceChoiceBox.getSelectionModel().getSelectedIndex()) {
                case 0:
                    CIS_DATA.setDiffuseLightSources(0);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 1:
                    CIS_DATA.setDiffuseLightSources(1);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 2:
                    CIS_DATA.setDiffuseLightSources(2);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 3:
                    CIS_DATA.setDiffuseLightSources(2);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
                case 4:
                    CIS_DATA.setDiffuseLightSources(0);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
            }
            if (newValue.contains("Coax") && CIS_DATA.getScanWidth() >= 1300) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Coax not available with the selected scan width");
                alert.show();
                internalLightSourceChoiceBox.getSelectionModel().select(oldValue);
                return;
            }
            internalLightColorChoiceBox.setDisable(CIS_DATA.getPhaseCount() == 3 || CIS_DATA.getLedLines() == 0);
        });
        internalLightColorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
                .ifPresent(CIS_DATA::setLightColor));
        externalLightSourceChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (CIS_DATA.getPhaseCount() == 3 && CIS_DATA.getLedLines() == 0 && newValue.equals("None")) {
                externalLightSourceChoiceBox.getSelectionModel().select(oldValue);
                return;
            }

            externalLightColorChoiceBox.setDisable(CIS_DATA.getPhaseCount() == 3 || externalLightSourceChoiceBox.getSelectionModel().getSelectedIndex() == 0);
            switch (externalLightSourceChoiceBox.getSelectionModel().getSelectedIndex()) {
                case 0:
                    LDSTD_DATA.setDiffuseLightSources(0);
                    LDSTD_DATA.setCoaxLightSources(0);
                    break;
                case 1:
                    LDSTD_DATA.setDiffuseLightSources(1);
                    LDSTD_DATA.setCoaxLightSources(0);
                    break;
            }
        });
        externalLightColorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS.LightColor.findByDescription(newValue)
                        .ifPresent(LDSTD_DATA::setLightColor));
        interfaceChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setGigeInterface(interfaceChoiceBox.getSelectionModel().getSelectedIndex() == 1));
        coolingChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
                .findByDescription(newValue.split("\\(")[0].trim())
                .ifPresent(CIS_DATA::setCooling));
        externalTriggerCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setExternalTrigger(newValue));
        cameraLinkModeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCLMode(newValue));

        phasesChoiceBox.getSelectionModel().selectFirst();
        resolutionChoiceBox.getSelectionModel().selectFirst();
        scanWidthChoiceBox.getSelectionModel().selectLast();
        internalLightSourceChoiceBox.getSelectionModel().select(1);
        externalLightSourceChoiceBox.getSelectionModel().selectFirst();
        internalLightColorChoiceBox.getSelectionModel().select(1);
        externalLightColorChoiceBox.getSelectionModel().select(1);
        interfaceChoiceBox.getSelectionModel().selectFirst();
        coolingChoiceBox.getSelectionModel().select(1);
        externalTriggerCheckbox.setSelected(false);
        cameraLinkModeChoiceBox.getSelectionModel().selectLast();
    }

    /**
     * handles the serial button press by opening the serial settings window
     */
    @FXML
    private void handleSerial() {
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("Serial.fxml", "Serial Interface parameters");
        ((SerialController) stageWithLoader.getValue().getController()).initialize(CIS_DATA);
        stageWithLoader.getKey().show();
    }
}
