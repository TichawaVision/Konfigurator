package de.tichawa.cis.config;

import javafx.fxml.*;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

// Datasheet für alle CIS
public class DataSheetController implements Initializable
{
  protected CIS CIS_DATA;

  @FXML
  private ScrollPane Scroller;
  @FXML
  private GridPane Grid;
  @FXML
  private TextArea Header;
  @FXML
  private TextArea Specs;
  @FXML
  private TextArea CLConfig;
  @FXML
  private ImageView ProductPic;
  @FXML
  private ImageView ProfilePic;
  @FXML
  private Menu File;
  @FXML
  private MenuItem Print;
  @FXML
  private Menu Lang;
  @FXML
  private MenuItem SwitchLang;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {

  }

  public void passData(CIS data)
  {
    this.CIS_DATA = data;

    load();
  }

  public TextArea getHeader()
  {
    return Header;
  }

  public TextArea getSpecs()
  {
    return Specs;
  }

  public TextArea getCLConfig()
  {
    return CLConfig;
  }

  private void load()
  {
    try
    {
      Lang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("lang"));
      SwitchLang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("switchlang"));

      File.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("File"));
      Print.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("Print"));

      String[] dataSheetText = CIS_DATA.createPrntOut().split("\n\t\n");
      String key = CIS_DATA.getTiViKey();

      Header.setText(dataSheetText[0]);
      Header.setEditable(false);
      Specs.setText(dataSheetText[1]);
      Specs.setEditable(false);
      Specs.setMinHeight((dataSheetText[1].length() - dataSheetText[1].replace("\n", "").length()) * 20);
      CLConfig.setText(dataSheetText[2]);
      CLConfig.setEditable(false);
      CLConfig.setMinHeight((dataSheetText[2].length() - dataSheetText[2].replace("\n", "").length()) * 20);
      Scroller.setStyle("-fx-background-color: #FFFFFF;");
      Grid.setStyle("-fx-background-color: #FFFFFF;");

      InputStream product = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Product.jpg");
      if(product != null)
      {
        ProductPic.setImage(new Image(product));
      }

      if(key.contains("MXCIS"))
      {
        String append = "";

        if(key.split("_")[5].endsWith("C"))
        {
          append += "_coax";
        }

        if(key.split("_")[5].startsWith("2"))
        {
          append += "_L2";
        }
        else
        {
          append += "_L1";
        }

        InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile" + append + ".jpg");
        if(profile != null)
        {
          ProfilePic.setImage(new Image(profile));
        }
      }
      else if(key.split("_")[4].endsWith("C") && getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg") != null)
      {
        InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg");
        if(profile != null)
        {
          ProfilePic.setImage(new Image(profile));
        }
      }
      else
      {
        InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile.jpg");
        if(profile != null)
        {
          ProfilePic.setImage(new Image(profile));
        }
      }
    }
    catch(CISException e)
    {
      ((Stage) Header.getScene().getWindow()).close();

      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setHeaderText(e.getMessage());
      alert.show();
    }
    catch(NullPointerException e)
    {
      e.printStackTrace();
    }
  }

  public void print()
  {
    PrinterJob p = PrinterJob.createPrinterJob();
    Pane printable = Grid;

    if(p.showPrintDialog(null))
    {
      p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(p.getJobSettings().getPageLayout().getPaper(), PageOrientation.PORTRAIT, 0.0, 0.0, 0.0, 0.0));
      double scaleX = p.getJobSettings().getPageLayout().getPrintableHeight() / printable.getHeight();
      double scaleY = p.getJobSettings().getPageLayout().getPrintableWidth() / printable.getWidth();

      printable.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));

      if(p.printPage(printable))
      {
        printable.getTransforms().clear();
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("printsuccess"));
        alert.show();
        p.endJob();
      }
      else
      {
        printable.getTransforms().clear();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings."));
        alert.show();
      }
    }
  }

  public void switchLang()
  {
    if(CIS_DATA.getLocale().toString().equals("de_DE"))
    {
      CIS.setLocale(new Locale("en", "US"));
    }
    else if(CIS_DATA.getLocale().toString().equals("en_US"))
    {
      CIS.setLocale(new Locale("de", "DE"));
    }

    load();
  }

  public ImageView getProfilePic()
  {
    return ProfilePic;
  }
}
