package de.tichawa.cis.config.ldstd;

import java.net.*;
import java.util.*;
import javafx.beans.value.*;

public class MaskController extends de.tichawa.cis.config.MaskController
{
  public MaskController()
  {
    CIS_DATA = new LDSTD();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    CIS_DATA.setSpec("Color", 1);
    CIS_DATA.setSpec("Internal Light Source", 1);
    CIS_DATA.setSpec("Internal Light Color", 0);
    CIS_DATA.setSpec("Scan Width", 12);
    CIS_DATA.setSpec("sw_cp", 1040);
    CIS_DATA.setSpec("sw_index", 6);
    CIS_DATA.setSpec("External Trigger", 0);
    CIS_DATA.setSpec("LEDLines", 1);

    Color.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
            -> 
            {
              if(newValue.equals("RGB") && CIS_DATA.getSpec("Internal Light Source") == 0)
              {
                Color.getSelectionModel().select(oldValue);
                return;
              }

              switch(newValue)
              {
                case "Monochrome":
                {
                  CIS_DATA.setSpec("Color", 1);
                  break;
                }
                case "RGB":
                {
                  CIS_DATA.setSpec("Color", 3);
                  break;
                }
              }

              InternalLightColor.setDisable(newValue.equals("RGB") || CIS_DATA.getSpec("LEDLines") == 0);
              ExternalLightColor.setDisable(newValue.equals("RGB") || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);
    });
    ScanWidth.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
            -> 
            {
              int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

              CIS_DATA.setSpec("Scan Width", ScanWidth.getSelectionModel().getSelectedIndex());
              CIS_DATA.setSpec("sw_cp", sw);
              CIS_DATA.setSpec("sw_index", ScanWidth.getItems().size() - (ScanWidth.getSelectionModel().getSelectedIndex() + 1));
    });
    InternalLightSource.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
            -> 
            {
              if(CIS_DATA.getSpec("Color") == 3 && newValue.equals("None") && CIS_DATA.getSpec("External Light Source") == 0)
              {
                InternalLightSource.getSelectionModel().select(oldValue);
                return;
              }

              CIS_DATA.setSpec("Internal Light Source", InternalLightSource.getSelectionModel().getSelectedIndex());

              switch(newValue)
              {
                case "None":
                {
                  CIS_DATA.setSpec("LEDLines", 0);
                  break;
                }
                case "One sided":
                {
                  CIS_DATA.setSpec("LEDLines", 1);
                  break;
                }
                case "Two sided":
                {
                  CIS_DATA.setSpec("LEDLines", 2);
                  break;
                }
                case "Coax":
                {
                  CIS_DATA.setSpec("LEDLines", 1);
                  break;
                }
                case "Two sided plus Coax":
                {
                  CIS_DATA.setSpec("LEDLines", 3);
                  break;
                }
              }

              InternalLightColor.setDisable(CIS_DATA.getSpec("Color") == 3 || CIS_DATA.getSpec("LEDLines") == 0);
    });
    InternalLightColor.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
            -> 
            {
              CIS_DATA.setSpec("Internal Light Color", InternalLightColor.getSelectionModel().getSelectedIndex());
    });

    Color.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectFirst();
    InternalLightSource.getSelectionModel().select(1);
    InternalLightColor.getSelectionModel().selectFirst();
  }
}
