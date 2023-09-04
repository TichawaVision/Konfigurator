package de.tichawa.cis.config.controller.price_update;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.*;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.util.Pair;
import org.jooq.DSLContext;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for PriceUpdateInactiveItems.fxml
 */
public class PriceUpdateInactiveItemsController implements Initializable {
    @FXML
    private TableView<Pair<Integer, String>> overviewTable;

    /**
     * Replaces the article number for all entries in the equipment, electronic and mechanics table.
     *
     * @param oldArticleNumber the article number to replace
     * @param newArticleNumber the new article number after replacing
     */
    private static void replaceItem(int oldArticleNumber, int newArticleNumber) {
        System.out.println("replacing " + oldArticleNumber + " with " + newArticleNumber);
        try {
            // update in database in all possible locations
            DSLContext database = PriceUpdateController.getDatabaseOrThrowException();

            // replace in equipment table (only ferix old)
            int equipmentNumber = database.update(Equipment.EQUIPMENT)
                    .set(Equipment.EQUIPMENT.ART_NO_FERIX_OLD, newArticleNumber)
                    .where(Equipment.EQUIPMENT.ART_NO_FERIX_OLD.eq(oldArticleNumber)).execute();
            // replace in electronics table
            int electronicNumber = database.update(Electronic.ELECTRONIC)
                    .set(Electronic.ELECTRONIC.ART_NO, newArticleNumber)
                    .where(Electronic.ELECTRONIC.ART_NO.eq(oldArticleNumber)).execute();
            // replace in mechanics table: as art_no
            int mechanicNumber = database.update(Mechanic.MECHANIC)
                    .set(Mechanic.MECHANIC.ART_NO, newArticleNumber)
                    .where(Mechanic.MECHANIC.ART_NO.eq(oldArticleNumber)).execute();
            // replace in mechanics table: as next_size_art_no
            int mechanicNextNumber = database.update(Mechanic.MECHANIC)
                    .set(Mechanic.MECHANIC.NEXT_SIZE_ART_NO, newArticleNumber)
                    .where(Mechanic.MECHANIC.NEXT_SIZE_ART_NO.eq(oldArticleNumber)).execute();

            // show alert to user of updated records
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Replaced article in database:");
            alert.setContentText("Updated\n"
                    + equipmentNumber + " entries in equipment table,\n"
                    + electronicNumber + " entries in electronic table,\n"
                    + mechanicNumber + " entries in mechanic table for art_no,\n"
                    + mechanicNextNumber + " entries in mechanic table for next_size_art_no.");
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to replace article in database: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Adds a new table cell with the given content to the given pdf table
     *
     * @param table   the pdf table to add the cell to
     * @param content the string content of the new cell
     */
    private void addCellToPdfTable(PdfPTable table, String content) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        cell.setPhrase(new Phrase(content));
        table.addCell(cell);
    }

    /**
     * Adds the header line to the pdf pages
     *
     * @param writer the pdf writer of the pdf file
     */
    private void addPdfHeader(PdfWriter writer) {
        // add page event that is automatically called when a new page is written
        writer.setPageEvent(new PdfPageEventHelper() {

            /**
             * Handles all activity when a new pdf page is created. Adds the header line to the pdf page.
             */
            @Override
            public void onStartPage(PdfWriter writer, Document document) {
                // header line consists of a table with invisible borders
                PdfPTable headerTable = new PdfPTable(new float[]{1, 1}); // 2 columns with equal width (1:1)
                headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER); // invisible border

                // left cell
                headerTable.addCell("Configurator - Inactive items");

                // right cell
                PdfPCell cell = new PdfPCell(headerTable.getDefaultCell()); // copy default cell options (like no border)
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT); // align right cell right
                cell.setVerticalAlignment(Element.ALIGN_BOTTOM); // align right cell bottom
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"); // for date/time format
                cell.setPhrase(new Phrase("Generated on " + formatter.format(LocalDateTime.now()), FontFactory.getFont(FontFactory.HELVETICA, 10))); // smaller size for date/time
                headerTable.addCell(cell);

                // format and add table to page
                headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.top() + headerTable.getTotalHeight() + 8, writer.getDirectContent());
            }
        });
    }

    /**
     * Handles the replace article button press.
     * Lets the user input the new article number first.
     * Checks whether the new article is already in the database.
     * If it is, the article is replaced via {@link #replaceItem(int, int)}
     * If not, the user is asked for necessary inputs by launching the PriceUpdateNewItem.fxml for further handling
     *
     * @param article the old article to replace given as a pair of the article number and the ferix key
     */
    private void handleReplaceArticle(Pair<Integer, String> article) {
        // ask for article number first
        TextInputDialog articleNumberInputDialog = new TextInputDialog();
        articleNumberInputDialog.setTitle("New article number");
        articleNumberInputDialog.setHeaderText("Insert the new article number (FERIX alt)");
        articleNumberInputDialog.setContentText("Article number:");
        Optional<String> userInput = articleNumberInputDialog.showAndWait();

        // check if user gave an answer -> if not (cancel or X), just do nothing (return)
        if (!userInput.isPresent())
            return;

        // check if input is valid
        int newArticleNumber;
        try {
            newArticleNumber = Integer.parseInt(userInput.get());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("New Article number must be a number!");
            alert.showAndWait();
            return;
        }

        // check if article number is already in database
        PriceRecord existingRecord;
        try {
            existingRecord = PriceUpdateController.getDatabaseOrThrowException().selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.eq(newArticleNumber)).fetchOne();
        } catch (IOException e) {
            System.err.println("failed to read from database");
            return;
        }

        if (existingRecord != null) {
            // if found: show info and ask if continue
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Already found the new article " + newArticleNumber + " in configurator database with ferix key: " + existingRecord.getFerixKey()
                    + "\nAre you sure you want to replace the old article " + article.getKey() + ", " + article.getValue() + "?");

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK)
                    replaceItem(article.getKey(), newArticleNumber);
            });
            return;
        } // else (did not find the article in the database)

        // launch input mask for new article creation
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("price_update/PriceUpdateNewItem.fxml", "Add new article data");
        PriceUpdateNewItemController newItemController = stageWithLoader.getValue().getController();
        newItemController.passData(article.getKey(), newArticleNumber);
        stageWithLoader.getKey().showAndWait();

        // replace item in other tables if creation was successful
        if (newItemController.wasAddingSuccessful())
            replaceItem(article.getKey(), newArticleNumber);
    }

    /**
     * Handles the save menu button press.
     * Opens a file chooser and writes the inactive items as a table to the chosen pdf location.
     * Shows an alert if an error occurs during writing the pdf file.
     */
    @FXML
    private void handleSave() {
        try {
            // let the user choose the save location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
                    new FileChooser.ExtensionFilter("All files", "*"));
            java.io.File file = fileChooser.showSaveDialog(overviewTable.getScene().getWindow());
            if (file == null)
                return;
            // create pdf document and writer to the chosen location
            Document document = new Document();
            PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(file));

            // write pdf file content
            addPdfHeader(pdfWriter);
            writePdfContent(document);
        } catch (IOException | DocumentException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Saving failed");
            alert.setHeaderText("PDF file could not be written or saved: " + e);
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Handles the initialization on creation. Sets up the overview table columns with a button column to let the user replace an inactive item.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // setup table
        overviewTable.getColumns().clear();
        // - article column
        TableColumn<Pair<Integer, String>, Integer> articleNumberColumn = new TableColumn<>("Article number");
        articleNumberColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getKey()));
        overviewTable.getColumns().add(articleNumberColumn);
        // - ferix column
        TableColumn<Pair<Integer, String>, String> ferixKeyColumn = new TableColumn<>("Ferix key");
        ferixKeyColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getValue()));
        overviewTable.getColumns().add(ferixKeyColumn);
        // - replace button column
        TableColumn<Pair<Integer, String>, Button> replaceArticleColumn = new TableColumn<>();
        replaceArticleColumn.setCellFactory(ActionButtonTableCell.forTableColumn("Replace article", pair -> {
            handleReplaceArticle(pair);
            return pair;
        }));
        overviewTable.getColumns().add(replaceArticleColumn);
    }

    /**
     * Accepts the data for initialization. Initializes the overview table with the given items.
     *
     * @param items A map of inactive items to display that maps the article number to the ferix key.
     */
    public void passData(Map<Integer, String> items) {
        overviewTable.getItems().clear();
        overviewTable.getItems().addAll(items.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    /**
     * Writes the pdf content that is an overview table of the inactive items with columns "Article number" and "FERIX key".
     *
     * @param document the document to write the content to
     * @throws DocumentException if an error occurs during writing
     */
    private void writePdfContent(Document document) throws DocumentException {
        // open document to be able to write
        document.open();

        // overview table of inactive items
        PdfPTable table = new PdfPTable(new float[]{1, 2}); // 1/3 space for article number, 2/3 for ferix key
        // - table header
        table.setHeaderRows(1);
        addCellToPdfTable(table, "Article number");
        addCellToPdfTable(table, "FERIX key");
        // - table content
        overviewTable.getItems().forEach(pair -> {
            addCellToPdfTable(table, String.valueOf(pair.getKey()));
            addCellToPdfTable(table, pair.getValue());
        });
        // write the table to the document
        document.add(table);

        // all content written -> close
        document.close();
    }
}