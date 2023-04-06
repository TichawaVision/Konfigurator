package de.tichawa.cis.config.mxcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.controller.DataSheetController;
import de.tichawa.cis.config.ldstd.LDSTD;
import de.tichawa.cis.config.model.tables.records.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.*;

// Funktionen der Maske
public class MaskController extends de.tichawa.cis.config.controller.MaskController<MXCIS> {
    public MaskController() {
        CIS_DATA = new MXCIS();
        LDSTD LDSTD_DATA = new LDSTD();
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return Arrays.asList(
                new CIS.Resolution(600, 600, false, 0.5, 0.0423),
                new CIS.Resolution(400, 400, false, 0.5, 0.0635),
                new CIS.Resolution(300, 300, false, 1.0, 0.0847),
                new CIS.Resolution(200, 200, false, 1.0, 0.125),
                new CIS.Resolution(150, 300, false, 1.5, 0.167),
                new CIS.Resolution(100, 300, false, 2.0, 0.25),
                new CIS.Resolution(75, 300, false, 3.0, 0.339),
                new CIS.Resolution(50, 300, false, 4.0, 0.5),
                new CIS.Resolution(25, 300, false, 6.0, 1.0));
    }

    @Override
    // Initialisiert die graphische Oberfläche
    public void initialize(URL url, ResourceBundle rb) {
        CIS_DATA.setPhaseCount(1);
        CIS_DATA.getLightColors().add(CIS.LightColor.RED);
        CIS_DATA.setDiffuseLightSources(1);
        CIS_DATA.setCoaxLightSources(0);
        CIS_DATA.setSelectedResolution(getResolutions().get(0));
        CIS_DATA.setScanWidth(520);
        CIS_DATA.setExternalTrigger(false);
        CIS_DATA.setCooling(CIS.Cooling.FAIR);

        phasesChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (newValue.equals("RGB") && CIS_DATA.getLedLines() < 2) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("RGB only usable with Two Sided or One Sided plus Coax");
                alert.show();
                phasesChoiceBox.getSelectionModel().select(oldValue);
                return;
            }

            switch (newValue) {
                case "Monochrome": {
                    CIS_DATA.setPhaseCount(1);
                    LDSTD_DATA.setPhaseCount(1);
                    break;
                }
                case "RGB": {
                    CIS_DATA.setPhaseCount(4);
                    LDSTD_DATA.setPhaseCount(3);
                    break;
                }
            }

            if (newValue.equals("RGB")) {
                internalLightColorChoiceBox.getSelectionModel().selectFirst();
                externalLightColorChoiceBox.getSelectionModel().selectFirst();
            }
            internalLightColorChoiceBox.setDisable(CIS_DATA.getPhaseCount() == 4 || CIS_DATA.getLedLines() == 0);
            externalLightColorChoiceBox.setDisable(newValue.equals("RGB") || externalLightSourceChoiceBox.getSelectionModel().getSelectedIndex() == 0);

            SensorChipRecord sensorChip = CIS_DATA.getSensorChip(CIS_DATA.getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
            SensorBoardRecord sensorBoard = CIS_DATA.getSensorBoard(CIS_DATA.getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown ADC board"));
            AdcBoardRecord adcBoard = CIS_DATA.getADC("MODU_ADC(SLOW)").orElseThrow(() -> new CISException("Unknown ADC board."));
            double maxLR = Math.round(1000.0 * sensorBoard.getLines() / (CIS_DATA.getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
            maxLineRateLabel.setText(maxLR + " kHz");
            selectedLineRateSlider.setMax(maxLR * 1000);
            selectedLineRateSlider.setValue(maxLR * 1000);
            selectedLineRateSlider.setMajorTickUnit(selectedLineRateSlider.getMax() / 20);
            selectedLineRateSlider.setMinorTickCount(0);
            selectedLineRateSlider.setShowTickMarks(true);
            selectedLineRateSlider.setSnapToTicks(true);
            selectedLineRateSlider.setShowTickLabels(true);
            selectedLineRateSlider.setLabelFormatter(new StringConverter<Double>() {
                @Override
                public String toString(Double n) {
                    if (CIS_DATA.getPhaseCount() == 4) {
                        if (n == maxLR * 1000) {
                            return "K4";
                        } else {
                            return "";
                        }
                    } else {
                        if ((CIS_DATA.getSelectedResolution().getBoardResolution() != 600 && n == maxLR * 1000 / 4) || (CIS_DATA.getSelectedResolution().getBoardResolution() == 600 && n == maxLR * 1000 / 5)) {
                            return "K1";
                        } else if (n == maxLR * 1000 / 2) {
                            return "K2";
                        } else if (n == maxLR * 1000) {
                            return "K3";
                        } else {
                            return "";
                        }
                    }
                }

                @Override
                public Double fromString(String s) {
                    return 0.0;
                }
            });

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);
        });
        resolutionChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedResolution(getResolutions().get(resolutionChoiceBox.getSelectionModel().getSelectedIndex()));

            maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
            selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
            selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());
            if (CIS_DATA.getSelectedResolution().getActualResolution() != 600) {
                selectedLineRateSlider.setMajorTickUnit(selectedLineRateSlider.getMax() / 4);
                selectedLineRateSlider.setMinorTickCount(3);
            } else {
                selectedLineRateSlider.setMajorTickUnit(selectedLineRateSlider.getMax() / 20);
                selectedLineRateSlider.setMinorTickCount(0);
            }
            selectedLineRateSlider.setShowTickMarks(true);
            selectedLineRateSlider.setSnapToTicks(true);
            selectedLineRateSlider.setShowTickLabels(true);
            selectedLineRateSlider.setLabelFormatter(new StringConverter<Double>() {
                @Override
                public String toString(Double n) {
                    if (CIS_DATA.getPhaseCount() == 4) {
                        if (n == CIS_DATA.getMaxLineRate()) {
                            return "K4";
                        } else {
                            return "";
                        }
                    } else {
                        if (n == CIS_DATA.getMaxLineRate() / 4 || (CIS_DATA.getSelectedResolution().getBoardResolution() == 600 && n == CIS_DATA.getMaxLineRate() / 5)) {
                            return "K1";
                        } else if (n == CIS_DATA.getMaxLineRate() / 2) {
                            return "K2";
                        } else if (n == CIS_DATA.getMaxLineRate()) {
                            return "K3";
                        } else {
                            return "";
                        }
                    }
                }

                @Override
                public Double fromString(String s) {
                    return 0.0;
                }
            });

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            pixelSizeLabel.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
            defectSizeLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        scanWidthChoiceBox.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

            if (internalLightSourceChoiceBox.getSelectionModel().getSelectedItem() != null && internalLightSourceChoiceBox.getSelectionModel().getSelectedItem().contains("Coax") && sw > 1820) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("Coax Selected.\nPlease reduce the scanwidth");
                alert.show();
                scanWidthChoiceBox.getSelectionModel().select(oldValue);
                return;
            }
            if (sw > 2080) {
                externalLightSourceChoiceBox.getSelectionModel().selectFirst();
            }
            externalLightSourceChoiceBox.setDisable(sw > 2080);
            CIS_DATA.setScanWidth(sw);
            LDSTD_DATA.setScanWidth(CIS_DATA.getScanWidth());
        });

        //Wird jedes Mal ausgeführt, wenn sich der Wert des "Selected line rate" Sliders ändert
        selectedLineRateSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            if (newValue.intValue() < 100) {
                newValue = 100;
            }

            CIS_DATA.setSelectedLineRate(newValue.intValue());
            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            // Maskenfelder updaten
            currentLineRateLabel.setText(newValue.intValue() / 1000.0 + " kHz");

            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });


        internalLightSourceChoiceBox.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            if (newValue.contains("Coax") && CIS_DATA.getScanWidth() > 1820) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("Coax Selected.\nPlease reduce the scanwidth");
                alert.show();
                internalLightSourceChoiceBox.getSelectionModel().select(oldValue);
                return;
            }

            if (CIS_DATA.getPhaseCount() == 4 && !newValue.equals("One sided plus Coax") && !newValue.equals("Two sided")) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("RGB not available with the selected Lightsource");
                alert.show();
                internalLightSourceChoiceBox.getSelectionModel().select(oldValue);
                return;
            }

            if (newValue.contains("Coax")) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("Coax selected.\nPlease check line rate.");
                alert.show();
            }

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
                    CIS_DATA.setDiffuseLightSources(0);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
                case 3:
                    CIS_DATA.setDiffuseLightSources(2);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 4:
                    CIS_DATA.setDiffuseLightSources(1);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
            }

        });

        internalLightColorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
                .ifPresent(CIS_DATA::setLightColor));

        externalLightSourceChoiceBox.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            externalLightColorChoiceBox.setDisable(CIS_DATA.getPhaseCount() == 4 || externalLightSourceChoiceBox.getSelectionModel().getSelectedIndex() == 0);
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

        externalLightColorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
                .ifPresent(LDSTD_DATA::setLightColor));

        interfaceChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setGigeInterface(interfaceChoiceBox.getSelectionModel().getSelectedIndex() == 1));
        coolingChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
                .findByDescription(newValue.split("\\(")[0].trim())
                .ifPresent(CIS_DATA::setCooling));
        externalTriggerCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setExternalTrigger(newValue));

        // Standardwerte setzen - Listener werden das erste Mal aufgerufen
        phasesChoiceBox.getSelectionModel().selectFirst();
        resolutionChoiceBox.getSelectionModel().selectFirst();
        scanWidthChoiceBox.getSelectionModel().selectFirst();
        internalLightSourceChoiceBox.getSelectionModel().select(1);
        externalLightSourceChoiceBox.getSelectionModel().selectFirst();
        internalLightColorChoiceBox.getSelectionModel().select(1);
        externalLightColorChoiceBox.getSelectionModel().select(1);
        interfaceChoiceBox.getSelectionModel().selectFirst();
        coolingChoiceBox.getSelectionModel().select(1);
        externalTriggerCheckbox.setSelected(false);
    }

    @Override
    protected DataSheetController getNewDatasheetController() {
        return new de.tichawa.cis.config.mxcis.DataSheetController();
    }
}
