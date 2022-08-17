package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.CIS;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

public class MaskController extends de.tichawa.cis.config.MaskController<VUCIS>
{

  @FXML
  public ChoiceBox<String> LightPreset;
  @FXML
  public ChoiceBox<String> LensType;
  @FXML
  private ChoiceBox<String> BrightFieldLeft;
  @FXML
  private ChoiceBox<String> Coax;
  @FXML
  private ChoiceBox<String> BrightFieldRight;
  @FXML
  private ChoiceBox<String> DarkFieldLeft;
  @FXML
  private ChoiceBox<String> DarkFieldRight;

  public MaskController()
  {
    CIS_DATA = new VUCIS();
  }

  @Override
  public List<CIS.Resolution> setupResolutions()
  {
    return Arrays.asList(
            new CIS.Resolution(1000,1000,true,0,0.025),
            new CIS.Resolution(1000,1000,false,0,0.025),
            new CIS.Resolution(500,1000,false,0,0.05),
            new CIS.Resolution(250,1000,false,0,0.1),
            new CIS.Resolution(125,1000,false,0,0.02),
            new CIS.Resolution(100,1000,false,0,0.25),
            new CIS.Resolution(50,1000,false,0,0.5),
            new CIS.Resolution(25,1000,false,0,1.0));
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    CIS_DATA.setSelectedResolution(getResolutions().get(0));
    CIS_DATA.setScanWidth(1200);
    CIS_DATA.setExternalTrigger(false);
    CIS_DATA.setPhaseCount(1);
    CIS_DATA.setCooling(CIS.Cooling.LICO);

    Color.valueProperty().addListener((observable, oldValue, newValue) ->
    {
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
        if((CIS_DATA.getPhaseCount() == 3) && (!CIS_DATA.getLights().equals("CCCCC")) && (!CIS_DATA.getLights().equals("DCCCD")) &&
                (!CIS_DATA.getLights().equals("DDDDD")))
        {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Three phases available only with RGB or RGB8");
          alert.show();
          Color.getSelectionModel().select(oldValue);
          return;
        }

        if((CIS_DATA.getPhaseCount() == 3 && CIS_DATA.getScanWidth() > 1300) || (CIS_DATA.getPhaseCount() == 4 && CIS_DATA.getScanWidth() > 1040) ||
        (CIS_DATA.getPhaseCount() == 5 && CIS_DATA.getScanWidth() > 780) || (CIS_DATA.getPhaseCount() == 6 && CIS_DATA.getScanWidth() > 520))
        {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Not available with the selected scan width.Please reduce it.");
          alert.show();
          Color.getSelectionModel().select(oldValue);
          return;
        }
        if(CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING) && !(CIS_DATA.getPhaseCount() == 4 || CIS_DATA.getPhaseCount() == 1))
        {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Shape from shading not available with the selected color.");
          alert.show();
          Color.getSelectionModel().select(oldValue);
          return;
        }
        MaxLineRate.setText(CIS_DATA.getMaxLineRate()/ 1000.0 + " kHz");
        SelLineRate.setMax(CIS_DATA.getMaxLineRate());
        SelLineRate.setValue(CIS_DATA.getMaxLineRate());

        CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

    });

    LightPreset.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      Optional<VUCIS.LightPreset> lightPreset = VUCIS.LightPreset.findByDescription(newValue);
      lightPreset.ifPresent(CIS_DATA::setLightPreset);

      boolean disable = CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.CLOUDY_DAY);
      DarkFieldLeft.setDisable(disable);
      DarkFieldRight.setDisable(disable);

      boolean dis = CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING);
      BrightFieldLeft.setDisable(dis);
      Coax.setDisable(dis);
      DarkFieldLeft.setDisable(dis);
      DarkFieldRight.setDisable(dis);

      if(CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING))
      {
        if (CIS_DATA.getScanWidth() > 1040 || CIS_DATA.getScanWidth() < 520) {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Shape from Shading not available with the selected scan width");
          alert.show();
          LightPreset.getSelectionModel().select(oldValue);
          return;
        }
        if (!(CIS_DATA.getLensType().equals(VUCIS.LensType.TC54)) && !(CIS_DATA.getLensType().equals(VUCIS.LensType.TC54L))) {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Shape from Shading only available with TC54 with long DOF or TC54");
          alert.show();
          LightPreset.getSelectionModel().select(oldValue);
          return;
        }
        if (!(CIS_DATA.getPhaseCount() == 1) && !(CIS_DATA.getPhaseCount() == 4)) {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Shape from Shading not available with the selected color");
          alert.show();
          LightPreset.getSelectionModel().select(oldValue);
          return;
        }
        if (!(CIS_DATA.getRightBrightField().equals(CIS.LightColor.RED)) && (!CIS_DATA.getRightBrightField().equals(CIS.LightColor.WHITE))) {
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setHeaderText("Shape from Shading not available with the selected lightcolor");
          alert.show();
          LightPreset.getSelectionModel().select(oldValue);
        }
      }
    });
    BrightFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> {

      VUCIS.LightColor.findByDescription(newValue)
              .ifPresent(CIS_DATA::setLeftBrightField);
      if(CIS_DATA.getPhaseCount() == 3 && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB)
              && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB8)){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Three phases available only with RGB or RGB8");
        alert.show();
        BrightFieldLeft.getSelectionModel().select(oldValue);
      }
    });
    Coax.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      VUCIS.LightColor.findByDescription(newValue)
              .ifPresent(CIS_DATA::setCoaxLight);
      if(!(CIS_DATA.getCoaxLight().equals(CIS.LightColor.NONE)) && CIS_DATA.getScanWidth() > 1040)
      {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Coax Selected.\nPlease reduce the scanwidth");
        alert.show();
        Coax.getSelectionModel().select(oldValue);
      }
      if(CIS_DATA.getPhaseCount() == 3 && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB)
              && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB8)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Three phases available only with RGB or RGB8");
        alert.show();
        Coax.getSelectionModel().select(oldValue);
      }
    });
    BrightFieldRight.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      VUCIS.LightColor.findByDescription(newValue)
              .ifPresent(CIS_DATA::setRightBrightField);
      if((CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING))
              && (!CIS_DATA.getRightBrightField().equals(CIS.LightColor.RED) && !CIS_DATA.getRightBrightField().equals(CIS.LightColor.WHITE)))
      {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Shape from Shading only\nwith Red or White ");
        alert.show();
        BrightFieldRight.getSelectionModel().select(oldValue);
      }
      if(CIS_DATA.getPhaseCount() == 3 && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB)
              && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB8)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Three phases available only with RGB or RGB8");
        alert.show();
        BrightFieldRight.getSelectionModel().select(oldValue);
      }
    });
    DarkFieldLeft.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      VUCIS.LightColor.findByDescription(newValue)
              .ifPresent(CIS_DATA::setLeftDarkField);
      if(CIS_DATA.getPhaseCount() == 3 && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB)
              && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB8)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Three phases available only with RGB or RGB8");
        alert.show();
        DarkFieldLeft.getSelectionModel().select(oldValue);
      }
    });
    DarkFieldRight.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      VUCIS.LightColor.findByDescription(newValue)
              .ifPresent(CIS_DATA::setRightDarkField);

      if(CIS_DATA.getPhaseCount() == 3 && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB)
              && !CIS_DATA.getLeftBrightField().equals(CIS.LightColor.RGB8)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Three phases available only with RGB or RGB8");
        alert.show();
        DarkFieldRight.getSelectionModel().select(oldValue);
      }
    });
    LensType.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      Optional<VUCIS.LensType> lensType = VUCIS.LensType.findByDescription(newValue);
      lensType.ifPresent(CIS_DATA::setLensType);
      if(CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING) && !CIS_DATA.getLensType().equals(VUCIS.LensType.TC54L)
              && !CIS_DATA.getLensType().equals(VUCIS.LensType.TC54)){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Shape from Shading only available with TC54 with long DOF or TC54");
        alert.show();
        LensType.getSelectionModel().select(oldValue);
        return;
      }
    });

    Resolution.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      CIS_DATA.setSelectedResolution(getResolutions().get(Resolution.getSelectionModel().getSelectedIndex()));

      MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
      SelLineRate.setMax(CIS_DATA.getMaxLineRate());
      SelLineRate.setValue(CIS_DATA.getMaxLineRate());

      CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

      PixelSize.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
      DefectSize.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
      Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
      Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
    });
    ScanWidth.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      CIS_DATA.setScanWidth(Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim()));


      int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(' ')).trim());

      if((sw > 1300 && CIS_DATA.getPhaseCount() == 3) || (sw > 1040 && CIS_DATA.getPhaseCount() == 4) ||
              (sw > 780 && CIS_DATA.getPhaseCount() == 5)  || (sw > 520 && CIS_DATA.getPhaseCount() == 6))
      {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("The selected scan width is not available");
        alert.show();
        ScanWidth.getSelectionModel().select(oldValue);
        return;
      }

      if(!(CIS_DATA.getCoaxLight().equals(CIS.LightColor.NONE)) && CIS_DATA.getScanWidth() > 1040){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Coax Selected.\nPlease reduce the scanwidth");
        alert.show();
        ScanWidth.getSelectionModel().select(oldValue);
      }
    });

    SelLineRate.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      CIS_DATA.setSelectedLineRate(newValue.intValue());

      CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

      Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
      Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");

      if(CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.SHAPE_FROM_SHADING) && (CIS_DATA.getScanWidth() > 1040 || CIS_DATA.getScanWidth() < 520)){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Shape from Shading not available with the selected scan width");
        alert.show();
        ScanWidth.getSelectionModel().select((Integer) oldValue);
        return;
      }
    });

    Interface.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setGigeInterface(Interface.getSelectionModel().getSelectedIndex() == 1));
    Cooling.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
            .findByDescription(newValue.split("\\(")[0].trim())
            .ifPresent(CIS_DATA::setCooling));

    Color.getSelectionModel().selectFirst();
    LightPreset.getSelectionModel().selectFirst();
    BrightFieldLeft.getSelectionModel().select(1);
    Coax.getSelectionModel().selectFirst();
    BrightFieldRight.getSelectionModel().select(1);
    DarkFieldLeft.getSelectionModel().selectFirst();
    DarkFieldRight.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectLast();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    LensType.getSelectionModel().selectFirst();
  }
}
