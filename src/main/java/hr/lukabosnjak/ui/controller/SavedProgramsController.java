package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.service.ProgramExportService;
import hr.lukabosnjak.service.SavedJobAccessException;
import hr.lukabosnjak.service.SavedJobService;
import hr.lukabosnjak.service.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class SavedProgramsController {
    @FXML private ListView<MachiningJob> savedJobsList;
    @FXML private Label currentUserLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea gCodePreview;

    private final SavedJobService savedJobService;
    private final ProgramExportService programExportService;
    private final SessionContext sessionContext;
    private final ApplicationNavigation navigation;

    public SavedProgramsController(SavedJobService savedJobService, ProgramExportService programExportService,
                                   SessionContext sessionContext, ApplicationNavigation navigation) {
        this.savedJobService = savedJobService;
        this.programExportService = programExportService;
        this.sessionContext = sessionContext;
        this.navigation = navigation;
    }

    @FXML
    private void initialize() {
        currentUserLabel.setText(sessionContext.currentUser()
                .map(user -> user.getUsername() + " (" + user.getRole().getName() + ")")
                .orElse("Nema prijavljenog korisnika"));
        savedJobsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MachiningJob job, boolean empty) {
                super.updateItem(job, empty);
                setText(empty || job == null ? null : "#" + job.getMachiningJobId() + " — " + job.getName()
                        + " | " + job.getCreatedAt() + " | "
                        + job.getCreatedBy().getUsername() + " | "
                        + job.getShape().getShapeType() + " | " + job.getCncMachine().getName());
            }
        });
        savedJobsList.getSelectionModel().selectedItemProperty().addListener((obs, old, job) -> {
            gCodePreview.setText(job == null ? "" : savedJobService.gCodeProgramOf(job).text());
        });
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleExport() {
        MachiningJob job = savedJobsList.getSelectionModel().getSelectedItem();
        if (job == null) {
            statusLabel.setText("Odaberite spremljeni program.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Izvoz CNC programa");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CNC program (*.nc)", "*.nc"));
        chooser.setInitialFileName(job.getName() + ".nc");
        File file = chooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file == null) return;
        try {
            programExportService.export(savedJobService.gCodeProgramOf(job), file.toPath());
            statusLabel.setText("Program je izvezen u .nc datoteku.");
        } catch (IOException | IllegalArgumentException exception) {
            statusLabel.setText("Izvoz nije uspio: " + exception.getMessage());
        }
    }

    @FXML
    private void handleOpen() {
        if (savedJobsList.getSelectionModel().getSelectedItem() == null) {
            statusLabel.setText("Odaberite spremljeni program.");
            return;
        }
        MachiningJob selected = savedJobsList.getSelectionModel().getSelectedItem();
        try {
            navigation.showMainWithJob(savedJobService.loadById(selected.getMachiningJobId()));
        } catch (IllegalArgumentException | SavedJobAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        navigation.showMain();
    }

    @FXML
    private void handleLogout() {
        sessionContext.logout();
        navigation.showLogin();
    }

    private void refresh() {
        try {
            savedJobsList.getItems().setAll(savedJobService.loadAll());
            statusLabel.setText(savedJobsList.getItems().isEmpty()
                    ? "Nema spremljenih programa." : "Odaberite program za pregled ili izvoz.");
        } catch (SavedJobAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
}
