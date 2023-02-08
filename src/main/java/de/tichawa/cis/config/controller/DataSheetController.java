package de.tichawa.cis.config.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import de.tichawa.cis.config.*;
import javafx.fxml.*;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.*;

import java.io.*;
import java.net.URL;
import java.util.*;

// Datasheet für alle CIS
public class DataSheetController implements Initializable {
    private static final Font FONT_HEADLINE = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font FONT_RED = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.RED);

    private static final double LINE_HEIGHT = 17.5;

    protected CIS CIS_DATA;

    @SuppressWarnings("unused")
    @FXML
    private GridPane gridPane;
    @SuppressWarnings("unused")
    @FXML
    private TextArea headerTextArea;
    @SuppressWarnings("unused")
    @FXML
    private TextArea specsTextArea;
    @SuppressWarnings("unused")
    @FXML
    private TextArea specsWarningTextArea;
    @SuppressWarnings("unused")
    @FXML
    private TextArea clConfigTextArea;
    @SuppressWarnings("unused")
    @FXML
    private ImageView productImageView;
    @SuppressWarnings("unused")
    @FXML
    private ImageView profileImageView;
    @SuppressWarnings("unused")
    @FXML
    private Menu fileMenu;
    @SuppressWarnings("unused")
    @FXML
    private MenuItem printMenuItem;
    @SuppressWarnings("unused")
    @FXML
    private MenuItem saveMenuItem;
    @SuppressWarnings("unused")
    @FXML
    private Menu languageMenu;
    @SuppressWarnings("unused")
    @FXML
    private MenuItem switchLanguageMenuItem;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void passData(CIS data) {
        this.CIS_DATA = data.copy();
        load();
    }

    private void load() {
        try {
            languageMenu.setText(Util.getString("lang"));
            switchLanguageMenuItem.setText(Util.getString("switchlang"));

            fileMenu.setText(Util.getString("File"));
            //Print.setText(Util.getString("Print"));
            saveMenuItem.setText(Util.getString("Save"));

            String[] dataSheetText = CIS_DATA.createPrntOut().split("\n\t\n");
            String key = CIS_DATA.getTiViKey();

            headerTextArea.setText(dataSheetText[0]);
            headerTextArea.setEditable(false);
            //specs part
            if (dataSheetText[1].contains(CIS.PRINTOUT_WARNING)) { // if the specs have a warning -> extract it and put it in the SpecsWarning TextArea
                String[] split = dataSheetText[1].split(CIS.PRINTOUT_WARNING);
                dataSheetText[1] = split[0]; //take the part before the warning as the spec output
                specsWarningTextArea.setText(split[1].trim()); //take the part after the warning as warning text
                specsWarningTextArea.setMinHeight(split[1].trim().split("\n").length * LINE_HEIGHT);
            }
            specsTextArea.setText(dataSheetText[1].trim());
            specsTextArea.setEditable(false);
            specsTextArea.setMinHeight(dataSheetText[1].trim().split("\n").length * LINE_HEIGHT);
            specsWarningTextArea.setEditable(false);
            specsWarningTextArea.lookup(".scroll-bar:vertical").setDisable(true);
            clConfigTextArea.setText(dataSheetText[2].trim());
            clConfigTextArea.setEditable(false);
            clConfigTextArea.setMinHeight(dataSheetText[2].trim().split("\n").length * LINE_HEIGHT);

            InputStream product = getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Product.jpg");
            if (product != null) {
                productImageView.setImage(new Image(product));
            }
            InputStream profile = getClass().getResourceAsStream(getProfileImageUrlString());
            if (profile != null)
                profileImageView.setImage(new Image(profile));
        } catch (CISException e) {
            ((Stage) headerTextArea.getScene().getWindow()).close();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void print() { // old print
        PrinterJob p = PrinterJob.createPrinterJob();
        Pane printable = gridPane;

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
    @SuppressWarnings("unused")
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
        headerTextArea.setEditable(true);
        specsTextArea.setEditable(true);
        specsWarningTextArea.setEditable(true);
        clConfigTextArea.setEditable(true);

    }

    /**
     * returns whether the datasheet is in OEM mode by checking whether the header field is editable
     */
    private boolean isOEMMode() {
        return headerTextArea.isEditable();
    }

    public ImageView getProfileImageView() {
        return profileImageView;
    }

    /**
     * saves the current contents of the datasheet to a pdf file.
     * Shows a file chooser to determine the file location.
     */
    @SuppressWarnings("unused")
    @FXML
    private void save() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
                    new FileChooser.ExtensionFilter("All files", "*"));
            java.io.File file = fileChooser.showSaveDialog(headerTextArea.getScene().getWindow());
            Document document = new Document();
            PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(file));
            addHeader(pdfWriter);
            writeDocument(document);
        } catch (IOException | DocumentException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Saving failed");
            alert.setHeaderText("PDF file could not be written or saved. (" + e.getMessage() + ")");
            alert.showAndWait();
        }
    }

    /**
     * adds the header line to all pages that are written by the given pdf writer.
     * The header line consists of the tivi key, and the current page number out of total pages.
     *
     * @param pdfWriter the pdf writer that is used to write the pages that get the headline
     */
    private void addHeader(PdfWriter pdfWriter) {
        pdfWriter.setPageEvent(new PdfPageEventHelper() {
            private PdfTemplate totalNumberOfPages;

            /**
             * creates the template that is used to set the total number of pages
             * @param writer the pdf writer
             * @param document the pdf document
             */
            @Override
            public void onOpenDocument(PdfWriter writer, Document document) {
                totalNumberOfPages = writer.getDirectContent().createTemplate(30, 12);
            }

            /**
             * creates the headline for each page. Creates a {@link PdfPTable} with 3 columns: the tivi key, the current page number and the total number of pages.
             * The total number of pages is determined by the template {@link #totalNumberOfPages} and set by {@link #onCloseDocument(PdfWriter, Document)}
             * @param writer the pdf writer
             * @param document the pdf document
             */
            @Override
            public void onStartPage(PdfWriter writer, Document document) {
                try {
                    // create header table
                    PdfPTable headerTable = new PdfPTable(3);
                    headerTable.setWidths(new int[]{24, 24, 2});
                    headerTable.getDefaultCell().setFixedHeight(10);
                    headerTable.getDefaultCell().setBorder(Rectangle.BOTTOM);

                    // first cell: tivi key
                    PdfPCell tiviKeyCell = new PdfPCell();
                    tiviKeyCell.setBorder(0);
                    tiviKeyCell.setBorderWidthBottom(1);
                    tiviKeyCell.setPaddingBottom(5);
                    tiviKeyCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    tiviKeyCell.setPhrase(new Phrase(headerTextArea.getText(), FONT_HEADLINE));
                    headerTable.addCell(tiviKeyCell);

                    // second cell: current page number
                    PdfPCell currentPageCell = new PdfPCell();
                    currentPageCell.setBorder(0);
                    currentPageCell.setBorderWidthBottom(1);
                    currentPageCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    currentPageCell.setPhrase(new Phrase(Util.getString("pdfPageOf", writer.getPageNumber()), FONT_NORMAL));
                    headerTable.addCell(currentPageCell);

                    // third cell: total number of pages
                    PdfPCell totalNumberOfPagesCell = new PdfPCell(com.itextpdf.text.Image.getInstance(totalNumberOfPages));
                    totalNumberOfPagesCell.setBorder(0);
                    totalNumberOfPagesCell.setBorderWidthBottom(1);
                    headerTable.addCell(totalNumberOfPagesCell);

                    // format and add table to page
                    headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                    headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.top() + headerTable.getTotalHeight(), writer.getDirectContent());
                } catch (DocumentException e) {
                    System.err.println("unable to create pdf header: " + e);
                }
            }

            /**
             * sets the total number of pages to all pages by setting the template value of {@link #totalNumberOfPages}
             * @param writer the pdf writer
             * @param document the pdf document
             */
            @Override
            public void onCloseDocument(PdfWriter writer, Document document) {
                ColumnText.showTextAligned(totalNumberOfPages, Element.ALIGN_LEFT, new Phrase(String.valueOf(writer.getPageNumber()), FONT_NORMAL), 1, 0, 0);
            }
        });
    }

    /**
     * writes the text area contents to the given document
     *
     * @param document the document to write
     * @throws DocumentException when writing fails
     * @throws IOException       when writing fails
     */
    private void writeDocument(Document document) throws DocumentException, IOException {
        document.open();
        // product image
        com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(Objects.requireNonNull(
                getClass().getResource("/de/tichawa/cis/config/" + CIS_DATA.getTiViKey().toLowerCase().split("_")[1] + "/Product.jpg")));
        image.setAbsolutePosition(document.right() - image.getWidth(), document.top() - image.getHeight());
        document.add(image);
        // specs
        Paragraph specs = new Paragraph(prepareForPdfPrint(specsTextArea.getText()), FONT_NORMAL);
        specs.setMultipliedLeading(1.2f);
        document.add(specs);
        document.add(Chunk.NEWLINE);
        // spec warning
        Paragraph specsWarning = new Paragraph(specsWarningTextArea.getText(), FONT_RED);
        specsWarning.setMultipliedLeading(1.2f);
        document.add(specsWarning);
        // profile image
        URL profileImageUrl = getClass().getResource(getProfileImageUrlString());
        if (profileImageUrl != null) {
            com.itextpdf.text.Image profileImage = com.itextpdf.text.Image.getInstance(profileImageUrl);
            profileImage.scaleToFit((PageSize.A4.getWidth() - 2 * document.leftMargin()) / 2, (PageSize.A4.getHeight() - 2 * document.bottomMargin()) / 2);
            profileImage.setAbsolutePosition(document.leftMargin(), document.bottomMargin());
            document.add(profileImage);
        }
        // cl config
        Paragraph clConfig = new Paragraph(prepareForPdfPrint(clConfigTextArea.getText()), FONT_NORMAL);
        clConfig.setMultipliedLeading(1.2f);
        clConfig.setAlignment(Element.ALIGN_RIGHT);
        document.add(clConfig);

        document.close();
    }

    /**
     * returns the url to the profile image of the cis.
     * tries to get the "Profile.jpg" file for the CIS type unless overwritten by subclass
     *
     * @return the Profile.jpg for this CIS
     */
    protected String getProfileImageUrlString() {
        return "/de/tichawa/cis/config/" + CIS_DATA.getTiViKey().toLowerCase().split("_")[1] + "/Profile.jpg";
    }

    /**
     * prepares the given text for pdf print. Replaces quarter spaces and tabs with normal spaces as these do not work within a {@link Paragraph}.
     *
     * @param text the text that contains space like characters
     * @return the text where the space like characters are replaced with spaces
     */
    private static String prepareForPdfPrint(String text) {
        return text.replace('\u200a', ' ').replace('\t', ' ');
    }
}