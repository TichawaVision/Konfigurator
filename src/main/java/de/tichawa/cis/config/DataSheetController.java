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
public class DataSheetController implements Initializable {
    private static final double LINE_HEIGHT = 17.5;

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
    private TextArea SpecsWarning;
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
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void passData(CIS data) {
        this.CIS_DATA = data.copy();
        load();
    }

    private void load() {
        try {
            Lang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("lang"));
            SwitchLang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("switchlang"));

            File.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("File"));
            Print.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("Print"));


            String[] dataSheetText = CIS_DATA.createPrntOut().split("\n\t\n");
            String key = CIS_DATA.getTiViKey();

            Header.setText(dataSheetText[0]);
            Header.setEditable(false);
            //specs part
            if (dataSheetText[1].contains(CIS.PRINTOUT_WARNING)) { // if the specs have a warning -> extract it and put it in the SpecsWarning TextArea
                String[] split = dataSheetText[1].split(CIS.PRINTOUT_WARNING);
                dataSheetText[1] = split[0]; //take the part before the warning as the spec output
                SpecsWarning.setText(split[1].trim()); //take the part after the warning as warning text
                SpecsWarning.setMinHeight(split[1].trim().split("\n").length * LINE_HEIGHT);
            }
            Specs.setText(dataSheetText[1].trim());
            Specs.setEditable(false);
            Specs.setMinHeight(dataSheetText[1].trim().split("\n").length * LINE_HEIGHT);
            SpecsWarning.setEditable(false);
            SpecsWarning.lookup(".scroll-bar:vertical").setDisable(true);
            CLConfig.setText(dataSheetText[2].trim());
            CLConfig.setEditable(false);
            CLConfig.setMinHeight(dataSheetText[2].trim().split("\n").length * LINE_HEIGHT);
            Scroller.setStyle("-fx-background-color: #FFFFFF;");
            Grid.setStyle("-fx-background-color: #FFFFFF;");

            InputStream product = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Product.jpg");
            if (product != null) {
                ProductPic.setImage(new Image(product));
            }

            if (key.contains("MXCIS")) {
                String append = "";

                if (key.split("_")[5].endsWith("C")) {
                    append += "_coax";
                }

                if (key.split("_")[5].startsWith("2")) {
                    append += "_L2";
                } else {
                    append += "_L1";
                }

                InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile" + append + ".jpg");
                if (profile != null) {
                    ProfilePic.setImage(new Image(profile));
                }
            } else if (key.split("_")[4].endsWith("C") && getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg") != null) {
                InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg");
                if (profile != null) {
                    ProfilePic.setImage(new Image(profile));
                }
            } else {
                InputStream profile = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile.jpg");
                if (profile != null) {
                    ProfilePic.setImage(new Image(profile));
                }
            }
        } catch (CISException e) {
            ((Stage) Header.getScene().getWindow()).close();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void print() { //TODO make this not take a screenshot -> maybe by using labels instead of text areas
        PrinterJob p = PrinterJob.createPrinterJob();
        Pane printable = Grid;

        if (p.showPrintDialog(null)) {
            p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(p.getJobSettings().getPageLayout().getPaper(), PageOrientation.PORTRAIT, 0.0, 0.0, 0.0, 0.0));
            double scaleX = p.getJobSettings().getPageLayout().getPrintableHeight() / printable.getHeight();
            double scaleY = p.getJobSettings().getPageLayout().getPrintableWidth() / printable.getWidth();

            printable.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));

            if (p.printPage(printable)) {
                printable.getTransforms().clear();
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("printsuccess"));
                alert.show();
                p.endJob();
            } else {
                printable.getTransforms().clear();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings."));
                alert.show();
            }
        }
    }

    /**
     * handles the language switch menu item press:
     * shows an alert that progress will be lost in OEM mode and switches language only on confirmation
     */
    public void handleSwitchLang() {
        if (isOEMMode()) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setHeaderText(Util.getString("switch confirmation OEM mode"));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                switchLang();
                setEditable();
            } //else do nothing
        } else // no OEM mode -> just switch language
            switchLang();
    }

    /**
     * actually switches the language by setting the new locale and loading the data in the new language
     */
    private void switchLang() {
        Util.switchLanguage();
        load();
    }

    /**
     * makes the datasheet editable by setting header, specs and camera link config areas to editable
     */
    public void setEditable() {
        Header.setEditable(true);
        Specs.setEditable(true);
        SpecsWarning.setEditable(true);
        CLConfig.setEditable(true);

    }

    /**
     * returns whether the datasheet is in OEM mode by checking whether the header field is editable
     */
    private boolean isOEMMode() {
        return Header.isEditable();
    }

    public ImageView getProfilePic() {
        return ProfilePic;
    }
}
