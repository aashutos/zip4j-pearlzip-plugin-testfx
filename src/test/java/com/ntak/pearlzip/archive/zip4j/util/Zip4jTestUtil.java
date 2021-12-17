/*
 * Copyright © 2021 92AK
 */

package com.ntak.pearlzip.archive.zip4j.util;

import com.ntak.pearlzip.ui.util.PearlZipFXUtil;
import com.ntak.testfx.FormUtil;
import com.ntak.testfx.TypeUtil;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxRobot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ntak.pearlzip.ui.constants.ResourceConstants.DSV;
import static com.ntak.testfx.NativeFileChooserUtil.chooseFile;
import static com.ntak.testfx.TestFXConstants.PLATFORM;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Zip4jTestUtil {
    public static void simOpenEncryptedArchive(FxRobot robot, Path archive, boolean init, boolean inNewWindow,
            String password) {
        PearlZipFXUtil.simOpenArchive(robot, archive, init, inNewWindow);
        PasswordField pwField = robot.lookup("#textPassword").queryAs(PasswordField.class);
        robot.clickOn(pwField).sleep(250, TimeUnit.MILLISECONDS);
        TypeUtil.typeString(robot, password);
        Button btnContinue = robot.lookup("#btnContinue").queryAs(Button.class);
        robot.clickOn(btnContinue);
    }

    public static void simNewEncryptedArchive(FxRobot robot, Path archive, boolean init, Map<String,Object> comboOptions,
            String password) throws IOException {
        if (init) {
            robot.clickOn("#btnNew", MouseButton.PRIMARY);
            robot.sleep(50, MILLISECONDS);
            robot.clickOn("#mnuNewArchive", MouseButton.PRIMARY);

            robot.clickOn((t) -> Optional.ofNullable(t.getId()).orElse("")
                                      .equals("com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveWriteService.new-options"));

            if (Objects.nonNull(password)) {
                robot.clickOn("#checkEnableEncryption").sleep(250, MILLISECONDS);
            }

            for (String k : comboOptions.keySet()) {
                ComboBox field = robot.lookup(k)
                                      .queryAs(ComboBox.class);

                if (Objects.nonNull(field)) {
                    FormUtil.selectComboBoxEntry(robot, field, comboOptions.get(k));
                }
            }

            robot.clickOn("#textEncryptionPassword").sleep(250, MILLISECONDS);
            TypeUtil.typeString(robot, password);

            // Click on first tab
            robot.clickOn((Node)robot.lookup(".tab-pane > .tab-header-area > .headers-region > .tab").query());
        }

        final String[] nameSplit = DSV.split(archive.getFileName()
                                                    .toString());
        final String archiveFormat = nameSplit[nameSplit.length-1];
        ComboBox<String> cmbArchiveFormat = FormUtil.lookupNode(s -> s.isShowing() && s.getTitle().equals("Create new archive..."), "#comboArchiveFormat");
        FormUtil.selectComboBoxEntry(robot, cmbArchiveFormat, archiveFormat);

        robot.clickOn("#btnCreate", MouseButton.PRIMARY);
        robot.sleep(50, MILLISECONDS);

        Files.deleteIfExists(archive);
        chooseFile(PLATFORM, robot, archive);
        robot.sleep(50, MILLISECONDS);

        Assertions.assertTrue(Files.exists(archive), "Archive was not created");
    }
}
