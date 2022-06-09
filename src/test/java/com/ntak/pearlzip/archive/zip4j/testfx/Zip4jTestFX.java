/*
 * Copyright © 2021 92AK
 */

package com.ntak.pearlzip.archive.zip4j.testfx;

import com.ntak.pearlzip.archive.constants.LoggingConstants;
import com.ntak.pearlzip.archive.pub.FileInfo;
import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveReadService;
import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveWriteService;
import com.ntak.pearlzip.archive.zip4j.util.Zip4jTestUtil;
import com.ntak.pearlzip.ui.model.FXArchiveInfo;
import com.ntak.pearlzip.ui.util.JFXUtil;
import com.ntak.pearlzip.ui.util.PearlZipFXUtil;
import com.ntak.testfx.FormUtil;
import com.ntak.testfx.NativeFileChooserUtil;
import com.ntak.testfx.TestFXConstants;
import com.ntak.testfx.TypeUtil;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ntak.pearlzip.archive.constants.ArchiveConstants.CURRENT_SETTINGS;
import static com.ntak.pearlzip.archive.pub.ArchiveService.CUSTOM_MENUS;
import static com.ntak.pearlzip.archive.zip4j.constants.Zip4jConstants.KEY_ENCRYPTION_ENABLE;
import static com.ntak.pearlzip.archive.zip4j.constants.Zip4jConstants.KEY_SPLIT_ARCHIVE_SIZE;
import static com.ntak.pearlzip.ui.UITestSuite.clearDirectory;
import static com.ntak.pearlzip.ui.constants.ResourceConstants.DSV;
import static com.ntak.pearlzip.ui.constants.ZipConstants.SETTINGS_FILE;
import static com.ntak.pearlzip.ui.constants.ZipConstants.STORE_TEMP;
import static com.ntak.pearlzip.ui.pub.PearlZipApplication.genFrmAbout;
import static com.ntak.pearlzip.ui.util.PearlZipFXUtil.*;
import static com.ntak.testfx.FormUtil.resetComboBox;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Zip4jTestFX extends AbstractZip4jTestFX {

    /*
     *  Test cases:
     *  + Open encrypted Zip archive - success
     *  + Open encrypted Zip archive - failure
     *  + Add file to encrypted archive - success
     *  + Delete file from encrypted archive - success
     *  + Create encrypted zip archive - AES 256-bit
     *  + Create encrypted zip archive - AES 128-bit
     *  + Zip4j options persistence (Apply,Cancel)
     *  + Zip4j options persistence (Ok)
     *  + Zip4j options no persistence (Cancel)
     *  + New archive - length validation check
     *  + Open archive - length validation check
     *  + Encrypt an unencrypted zip archive - success
     *  + Encrypt a temporary zip archive - failure
     *  + Split an unencrypted zip archive - success
     *  + Split an encrypted zip archive - failure
     */
    @BeforeAll
    public static void setUpOnce() {
        LoggingConstants.LOG_BUNDLE = ResourceBundle.getBundle("pearlzip",
                                                               Locale.getDefault());
        LoggingConstants.CUSTOM_BUNDLE = ResourceBundle.getBundle("custom",
                                                                  Locale.getDefault());
    }


    @Test
    @DisplayName("Test: Open encrypted archive successfully")
    public void testFX_OpenEncryptedArchive_Success() {
        Path archive = Paths.get("src", "test", "resources", "ea.zip").toAbsolutePath();
        Zip4jTestUtil.simOpenEncryptedArchive(this, archive, true, false, "password");
        sleep(250, MILLISECONDS);

        FXArchiveInfo fxArchiveInfo = JFXUtil.lookupArchiveInfo(archive.toAbsolutePath().toString())
                                                    .get();
        Assertions.assertEquals(3, fxArchiveInfo.getFiles().size(), "The expected number of files were not as anticipated");
        Assertions.assertTrue(fxArchiveInfo.getFiles().stream().anyMatch(f-> f.getFileName().equals("folder")), "folder was not found in archive");
        Assertions.assertTrue(fxArchiveInfo.getFiles().stream().anyMatch(f-> f.getFileName().equals("folder/enc_file_1")), "enc_file_1 was not found in archive");
        Assertions.assertTrue(fxArchiveInfo.getFiles().stream().anyMatch(f-> f.getFileName().equals("folder/enc_file_2")), "enc_file_2 was not found in archive");

        clickOn(Point2D.ZERO.add(110, 10)).clickOn(Point2D.ZERO.add(110, 160));
        sleep(250, MILLISECONDS);
    }

    @Test
    @DisplayName("Test: Open encrypted archive with wrong password yields error message")
    public void testFX_OpenEncryptedArchiveWrongPassword_Fails() {
        Path archive = Paths.get("src", "test", "resources", "ea.zip").toAbsolutePath();
        Zip4jTestUtil.simOpenEncryptedArchive(this, archive, true, false, "p");
        sleep(1000, MILLISECONDS);

        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        Assertions.assertTrue(dialogPane.getHeaderText()
                                        .matches(String.format(".*issue-extracting-file.*%s.*", archive)));
        Assertions.assertTrue(JFXUtil.getMainStageInstances().stream().noneMatch(s->s.getTitle().contains(archive.toString())), "The archive was open unexpectedly");
    }

    @Test
    @DisplayName("Test: Add file to encrypted archive successfully")
    public void testFX_AddFileEncryptedArchive_Success() throws IOException {
        // Set up
        Path archive = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "ea.zip");
        Path srcArchive = Path.of("src", "test", "resources", "ea.zip").toAbsolutePath();
        Files.createDirectories(archive.getParent());
        Files.copy(srcArchive, archive, StandardCopyOption.REPLACE_EXISTING);
        Path file = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "additional_file");
        Files.createFile(file);

        try {
            // Open encrypted archive
            Zip4jTestUtil.simOpenEncryptedArchive(this, archive, true, false, "password");
            FXArchiveInfo fxArchiveInfo = JFXUtil.lookupArchiveInfo(archive.toAbsolutePath()
                                                                           .toString())
                                                 .get();
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .noneMatch((f) -> f.getFileName()
                                                                  .matches(
                                                                          String.format(".*%s",
                                                                                        file.getFileName()
                                                                                            .toString())
                                                                  )
                                               ), "File already exists in archive unexpectedly");

            // Add file
            simAddFile(this, file);

            // Checks
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .anyMatch((f) -> f.getFileName()
                                                                 .equals(file.getFileName()
                                                                             .toString())
                                               ), "File does not exist in archive");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(archive);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    @DisplayName("Test: Delete file from encrypted archive successfully")
    public void testFX_DeleteFileEncryptedArchive_Success() throws IOException {
        // Set up
        Path archive = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "ea.zip");
        Path srcArchive = Path.of("src", "test", "resources", "ea.zip").toAbsolutePath();
        Files.createDirectories(archive.getParent());
        Files.copy(srcArchive, archive, StandardCopyOption.REPLACE_EXISTING);

        try {
            // Open encrypted archive
            Zip4jTestUtil.simOpenEncryptedArchive(this, archive, true, false, "password");
            FXArchiveInfo fxArchiveInfo = JFXUtil.lookupArchiveInfo(archive.toAbsolutePath()
                                                                           .toString())
                                                 .get();
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .anyMatch((f) -> f.getFileName()
                                                                  .equals("folder/enc_file_2")
                                                                  )
                                               , "File does not exists in archive");
            sleep(250, MILLISECONDS);
            // Add file
            simTraversalArchive(this, archive.toAbsolutePath().toString(), "#fileContentsView", (r)->{}, "folder",
                                "enc_file_2");
            simDelete(this);

            // Checks
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .noneMatch((f) -> f.getFileName()
                                                                 .equals("folder/enc_file_2")
                                               )
                    , "File was not deleted from archive");
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    @DisplayName("Test: Create zip archive with AES-256 bit encryption successfully")
    public void testFX_CreateEncryptedArchiveAES256_Success() throws IOException {
        // Set up
        Path archive = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "ea.zip");
        Files.createDirectories(archive.getParent());
        Path file = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "additional_file");
        Files.createFile(file);

        try {
            Map<String,Object> params = Map.of("#comboEncryptionAlgorithm", "AES", "#comboEncryptionStrength", "256-bit");
            Zip4jTestUtil.simNewEncryptedArchive(this, archive, true, params, "pass");
            simAddFile(this, file);

            FXArchiveInfo fxArchiveInfo = JFXUtil.lookupArchiveInfo(archive.toAbsolutePath()
                                                                           .toString())
                                                 .get();

            // Checks
            Assertions.assertTrue(Files.exists(archive), "Archive does not exist");
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .anyMatch((f) -> f.getFileName()
                                                                 .equals(file.getFileName()
                                                                             .toString())
                                               ), "File does not exist in archive");
           FileInfo fileInfo = fxArchiveInfo.getFiles()
                                            .stream()
                                            .filter((f) -> f.getFileName()
                                                             .equals(file.getFileName()
                                                                         .toString())
                                            ).findFirst().get();

           Assertions.assertTrue(fileInfo.isEncrypted(), "File was not encrypted as expected");
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(file);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    @DisplayName("Test: Create zip archive with AES-128 bit encryption successfully")
    public void testFX_CreateEncryptedArchiveAES128_Success() throws IOException {
        // Set up
        Path archive = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "ea.zip");
        Files.createDirectories(archive.getParent());
        Path file = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "additional_file");
        Files.createFile(file);

        try {
            Map<String,Object> params = Map.of("#comboEncryptionAlgorithm", "AES", "#comboEncryptionStrength", "128-bit");
            Zip4jTestUtil.simNewEncryptedArchive(this, archive, true, params, "pass");
            simAddFile(this, file);

            FXArchiveInfo fxArchiveInfo = JFXUtil.lookupArchiveInfo(archive.toAbsolutePath()
                                                                           .toString())
                                                 .get();

            // Checks
            Assertions.assertTrue(Files.exists(archive), "Archive does not exist");
            Assertions.assertTrue(fxArchiveInfo.getFiles()
                                               .stream()
                                               .anyMatch((f) -> f.getFileName()
                                                                 .equals(file.getFileName()
                                                                             .toString())
                                               ), "File does not exist in archive");
            FileInfo fileInfo = fxArchiveInfo.getFiles()
                                             .stream()
                                             .filter((f) -> f.getFileName()
                                                             .equals(file.getFileName()
                                                                         .toString())
                                             ).findFirst().get();

            Assertions.assertTrue(fileInfo.isEncrypted(), "File was not encrypted as expected");
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(file);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    @DisplayName("Test: Change Zip4j Options while selecting Apply, Cancel buttons will persist configuration changes")
    public void testFX_Zip4jOptionsApplyCancel_MatchExpectations() throws IOException {
        // Back up current configuration, if exists
        Path configFile = SETTINGS_FILE;
        Path backupConfigFile = Path.of(String.format("%s.backup", SETTINGS_FILE.toAbsolutePath()));

        if (Files.exists(configFile)) {
            Files.copy(configFile, backupConfigFile, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            // Navigate to Zip4J Options tab
            this.clickOn(Point2D.ZERO.add(160, 10))
                .clickOn(Point2D.ZERO.add(160, 30))
                .clickOn(Point2D.ZERO.add(925, 200))
                .doubleClickOn(Point2D.ZERO.add(925, 375));

            this.clickOn("#comboDefaultCompressionLevel");

            // Make changes
            ComboBox comboDefaultCompressionLevel =
                    this.lookup("#comboDefaultCompressionLevel").queryAs(ComboBox.class);
            ComboBox comboDefaultCompressionMethod =
                    this.lookup("#comboDefaultCompressionMethod").queryAs(ComboBox.class);

            resetComboBox(this, comboDefaultCompressionLevel);
            resetComboBox(this, comboDefaultCompressionMethod);

            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionMethod, "STORE");
            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionLevel, 4);

            // Click Apply, Cancel
            this.clickOn("#btnApply")
                .sleep(250, MILLISECONDS)
                .clickOn("#btnCancel");

            // Check options have been persisted to settings file
            List<String> configs = Files.readAllLines(configFile);
            Assertions.assertTrue(
                    configs.stream().anyMatch(s-> s.equals("configuration.zip4j.default-compression-level=4")),
                    "Default compression level was not set as expected");
            Assertions.assertTrue(
                    configs.stream().anyMatch(s-> s.equals("configuration.zip4j.default-compression-method=STORE")),
                    "Default compression level was not set as expected");
        } finally {
            // Restore configuration file, if exists
            if (Files.exists(backupConfigFile)) {
                Files.copy(backupConfigFile, configFile, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backupConfigFile);
            }
        }
    }

    @Test
    @DisplayName("Test: Change Zip4j Options while selecting ok button will persist configuration changes")
    public void testFX_Zip4jOptionsOk_MatchExpectations() throws IOException {
        // Back up current configuration, if exists
        Path configFile = SETTINGS_FILE;
        Path backupConfigFile = Path.of(String.format("%s.backup", SETTINGS_FILE.toAbsolutePath()));

        if (Files.exists(configFile)) {
            Files.copy(configFile, backupConfigFile, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            // Navigate to Zip4J Options tab
            this.clickOn(Point2D.ZERO.add(160, 10))
                .clickOn(Point2D.ZERO.add(160, 30))
                .clickOn(Point2D.ZERO.add(925, 200))
                .doubleClickOn(Point2D.ZERO.add(925, 375));

            this.clickOn("#comboDefaultCompressionLevel");

            // Make changes
            ComboBox comboDefaultCompressionLevel =
                    this.lookup("#comboDefaultCompressionLevel").queryAs(ComboBox.class);
            ComboBox comboDefaultCompressionMethod =
                    this.lookup("#comboDefaultCompressionMethod").queryAs(ComboBox.class);

            resetComboBox(this, comboDefaultCompressionLevel);
            resetComboBox(this, comboDefaultCompressionMethod);

            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionMethod, "STORE");
            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionLevel, 4);

            // Click Ok
            this.clickOn("#btnOk");

            // Check options have been persisted to settings file
            List<String> configs = Files.readAllLines(configFile);
            Assertions.assertTrue(
                    configs.stream().anyMatch(s-> s.equals("configuration.zip4j.default-compression-level=4")),
                    "Default compression level was not set as expected");
            Assertions.assertTrue(
                    configs.stream().anyMatch(s-> s.equals("configuration.zip4j.default-compression-method=STORE")),
                    "Default compression level was not set as expected");
        } finally {
            // Restore configuration file, if exists
            if (Files.exists(backupConfigFile)) {
                Files.copy(backupConfigFile, configFile, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backupConfigFile);
            }
        }
    }

    @Test
    @DisplayName("Test: Change Zip4j Options while selecting cancel button will not persist configuration changes")
    public void testFX_Zip4jOptionsCancel_MatchExpectations() throws IOException {
        // Back up current configuration, if exists
        Path configFile = SETTINGS_FILE;
        Path backupConfigFile = Path.of(String.format("%s.backup", SETTINGS_FILE.toAbsolutePath()));

        if (Files.exists(configFile)) {
            Files.copy(configFile, backupConfigFile, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            // Navigate to Zip4J Options tab
            this.clickOn(Point2D.ZERO.add(160, 10))
                .clickOn(Point2D.ZERO.add(160, 30))
                .clickOn(Point2D.ZERO.add(925, 200))
                .doubleClickOn(Point2D.ZERO.add(925, 375));

            this.clickOn("#comboDefaultCompressionLevel");

            // Make changes
            ComboBox comboDefaultCompressionLevel =
                    this.lookup("#comboDefaultCompressionLevel").queryAs(ComboBox.class);
            ComboBox comboDefaultCompressionMethod =
                    this.lookup("#comboDefaultCompressionMethod").queryAs(ComboBox.class);

            resetComboBox(this, comboDefaultCompressionLevel);
            resetComboBox(this, comboDefaultCompressionMethod);

            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionMethod, "STORE");
            FormUtil.selectComboBoxEntry(this, comboDefaultCompressionLevel, 4);

            // Click Cancel
            this.clickOn("#btnCancel");

            // Check options have been persisted to settings file
            List<String> configs = Files.readAllLines(configFile);
            List<String> backupConfigs = Files.readAllLines(backupConfigFile);
            for (int i = 0; i < backupConfigs.size(); i++) {
                Assertions.assertEquals(configs.get(i), backupConfigs.get(i), "Line does not match from settings files. No change expected");
            }
        } finally {
            // Restore configuration file, if exists
            if (Files.exists(backupConfigFile)) {
                Files.copy(backupConfigFile, configFile, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backupConfigFile);
            }
        }
    }


    @Test
    @DisplayName("Test: Create zip archive with AES-256 bit encryption with no password fails")
    public void testFX_CreateEncryptedArchiveAES256NoPassword_Fails() throws IOException {
        // Set up
        Path archive = Path.of(STORE_TEMP.toAbsolutePath().toString(), "pz1234567890", "ea.zip");
        Files.createDirectories(archive.getParent());

        try {
            Map<String,Object> params = Map.of("#comboEncryptionAlgorithm", "AES", "#comboEncryptionStrength", "256-bit");

            // Open New dialog...
            clickOn("#btnNew", MouseButton.PRIMARY);
            sleep(50, MILLISECONDS);
            clickOn("#mnuNewArchive", MouseButton.PRIMARY);

            clickOn((t) -> Optional.ofNullable(t.getId()).orElse("")
                                         .equals("com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveWriteService.new-options"));

            // Set options on Zip4j tab
            clickOn("#checkEnableEncryption").sleep(250, MILLISECONDS);

            for (String k : params.keySet()) {
                ComboBox field = lookup(k)
                                      .queryAs(ComboBox.class);

                if (Objects.nonNull(field)) {
                    FormUtil.selectComboBoxEntry(this, field, params.get(k));
                }
            }

            // Click on first tab
            clickOn((Node)lookup(".tab-pane > .tab-header-area > .headers-region > .tab").query());

            final String[] nameSplit = DSV.split(archive.getFileName()
                                                        .toString());
            final String archiveFormat = nameSplit[nameSplit.length-1];
            ComboBox<String> cmbArchiveFormat = FormUtil.lookupNode(s -> s.isShowing() && s.getTitle().equals("Create new archive..."), "#comboArchiveFormat");
            FormUtil.selectComboBoxEntry(this, cmbArchiveFormat, archiveFormat);

            clickOn("#btnCreate", MouseButton.PRIMARY);
            sleep(50, MILLISECONDS);

            DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
            Assertions.assertTrue(dialogPane.getContentText()
                                            .contains("validation-issue"));
            Assertions.assertTrue(JFXUtil.getMainStageInstances().stream().noneMatch(s->s.getTitle().contains(archive.toString())), "The archive was open unexpectedly");
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    @DisplayName("Test: Open encrypted archive with no password yields error message")
    public void testFX_OpenEncryptedArchiveNoPassword_Fails() {
        Path archive = Paths.get("src", "test", "resources", "ea.zip").toAbsolutePath();
        Zip4jTestUtil.simOpenEncryptedArchive(this, archive, true, false, "");
        sleep(1000, MILLISECONDS);

        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        Assertions.assertTrue(dialogPane.getHeaderText()
                                        .matches(String.format(".*issue-extracting-file.*%s.*", archive)));
        Assertions.assertTrue(JFXUtil.getMainStageInstances().stream().noneMatch(s->s.getTitle().contains(archive.toString())), "The archive was open unexpectedly");
    }

    @Test
    @DisplayName("Test: Encrypt an unencrypted zip archive successfully")
    public void testFX_EncryptUnencryptedZipArchive_Success() throws IOException, InterruptedException {
        Path srcArchive = Paths.get("src", "test", "resources", "unencryptedArchive.zip").toAbsolutePath();
        Path archive = Paths.get("tempArchive.zip").toAbsolutePath();
        try {
            Files.copy(srcArchive, archive, StandardCopyOption.REPLACE_EXISTING);
            // Hard coded movement to open MenuItem
            clickOn(Point2D.ZERO.add(110, 10)).clickOn(Point2D.ZERO.add(110, 80));
            PearlZipFXUtil.simOpenArchive(this, archive, false, false);

            initialiseSystemMenu();

            this.clickOn(325, 20)
                .clickOn(325,60)
                .sleep(1000);

            PasswordField pwField = this.lookup("#textPassword").queryAs(PasswordField.class);
            this.clickOn(pwField).sleep(250, TimeUnit.MILLISECONDS);
            TypeUtil.typeString(this, "password");
            Button btnContinue = this.lookup("#btnContinue").queryAs(Button.class);
            this.clickOn(btnContinue)
                .sleep(1000);

            Assertions.assertTrue((Boolean) new Zip4jArchiveReadService().generateArchiveMetaData(archive.toString()).getProperty(KEY_ENCRYPTION_ENABLE).orElse(false), "Generated file was not encrypted");
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    @Test
    @DisplayName("Test: Encrypt an temporary zip archive is not possible")
    public void testFX_EncryptTemporaryZipArchive_Failure() throws IOException,InterruptedException {
        try {
            Path archive = Paths.get(STORE_TEMP.toAbsolutePath().toString(), "temp.zip");
            simNewArchive(this, archive, true);

            initialiseSystemMenu();

            this.sleep(1000)
                .clickOn(325, 20)
                .clickOn(325,60)
                .sleep(1000);

            DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
            Assertions.assertTrue(dialogPane.getContentText()
                                            .matches(String.format(".*incompatible-encrypt.*")));
        } finally {
        }
    }

    @Test
    @DisplayName("Test: Split an unencrypted zip archive successfully")
    public void testFX_SplitUnencryptedZipArchive_Success() throws IOException, InterruptedException {
        // Set split size to minimum value...
        String prevCfgValue = CURRENT_SETTINGS.getProperty(KEY_SPLIT_ARCHIVE_SIZE);
        CURRENT_SETTINGS.setProperty(KEY_SPLIT_ARCHIVE_SIZE, "65536");

        Path srcArchive = Paths.get("src", "test", "resources", "unencryptedArchive.zip").toAbsolutePath();
        Path archive = Paths.get("tempArchive.zip").toAbsolutePath();

        Path filePath = Paths.get("src", "test", "resources", "megFile.bin").toAbsolutePath();
        Path tgtArchive = Files.createTempDirectory("pz");
        try {
            Files.copy(srcArchive, archive, StandardCopyOption.REPLACE_EXISTING);
            // Hard coded movement to open MenuItem
            clickOn(Point2D.ZERO.add(110, 10)).clickOn(Point2D.ZERO.add(110, 80));
            PearlZipFXUtil.simOpenArchive(this, archive, false, false);
            simAddFile(this, filePath);

            FXArchiveInfo info = JFXUtil.lookupArchiveInfo(archive.toString()).get();
            while (info.getFiles().size() < 4) {
                sleep(300);
            }
            sleep(300);
            initialiseSystemMenu();
            sleep(300);
            this.clickOn(325, 10)
                .clickOn(325,40)
                .sleep(300);

            NativeFileChooserUtil.chooseFile(TestFXConstants.PLATFORM, this, tgtArchive);
            sleep(5000);
            Assertions.assertTrue(Files.list(tgtArchive).count() > 1, String.format("Archive was not split as " +
                                                                                            "expected. Number of " +
                                                                                            "files detected: %s",
                                                                                    Files.list(tgtArchive).count()));
        } finally {
            Files.deleteIfExists(archive);
            clearDirectory(tgtArchive);
            if (Objects.nonNull(prevCfgValue)) {
                CURRENT_SETTINGS.setProperty(KEY_SPLIT_ARCHIVE_SIZE, prevCfgValue);
            }
            System.out.println(tgtArchive);
        }
    }

    @Test
    @DisplayName("Test: Split an encrypted zip archive fails")
    public void testFX_SplitEncryptedZipArchive_Fails() throws IOException, InterruptedException {
        // Set split size to minimum value...
        String prevCfgValue = CURRENT_SETTINGS.getProperty(KEY_SPLIT_ARCHIVE_SIZE);
        CURRENT_SETTINGS.setProperty(KEY_SPLIT_ARCHIVE_SIZE, "65536");

        Path srcArchive = Paths.get("src", "test", "resources", "ea.zip").toAbsolutePath();
        Path archive = Paths.get("tempencryptedarchive.zip").toAbsolutePath();

        Path tgtArchive = Files.createTempDirectory("pz");
        try {
            Files.copy(srcArchive, archive, StandardCopyOption.REPLACE_EXISTING);
            // Hard coded movement to open MenuItem
            clickOn(Point2D.ZERO.add(110, 10)).clickOn(Point2D.ZERO.add(110, 80));
            Zip4jTestUtil.simOpenEncryptedArchive(this, archive, false, false,"password");

            FXArchiveInfo info = JFXUtil.lookupArchiveInfo(archive.toString()).get();
            sleep(300);
            initialiseSystemMenu();
            sleep(300);
            this.clickOn(325, 10)
                .clickOn(325,40)
                .sleep(300);

            DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
            Assertions.assertTrue(dialogPane.getContentText()
                                            .matches(String.format(".*incompatible-split.*")));
        } finally {
            Files.deleteIfExists(archive);
            clearDirectory(tgtArchive);
            if (Objects.nonNull(prevCfgValue)) {
                CURRENT_SETTINGS.setProperty(KEY_SPLIT_ARCHIVE_SIZE, prevCfgValue);
            }
            System.out.println(tgtArchive);
        }
    }

    public static void initialiseSystemMenu() throws InterruptedException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        JFXUtil.runLater(() -> {
            try {
                Stage aboutStage = genFrmAbout();
                Menu menu = new Menu();
                final MenuBar menuBar = (MenuBar) new Zip4jArchiveWriteService().getFXFormByIdentifier(CUSTOM_MENUS)
                                                                                .get()
                                                                                .getContent();
                menu.getItems()
                    .addAll(menuBar.getMenus()
                                   .get(0)
                                   .getItems());
                menu.setText(menuBar.getMenus()
                                    .get(0)
                                    .getText());
                createSystemMenu(aboutStage,
                                 Collections.singletonList(menu));
            } catch(IOException e) {

            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
