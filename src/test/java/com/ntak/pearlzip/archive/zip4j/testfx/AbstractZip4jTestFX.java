/*
 * Copyright © 2021 92AK
 */

package com.ntak.pearlzip.archive.zip4j.testfx;

import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveReadService;
import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveWriteService;
import com.ntak.pearlzip.ui.constants.ZipConstants;
import com.ntak.pearlzip.ui.util.AbstractPearlZipTestFX;
import com.ntak.pearlzip.ui.util.ErrorAlertConsumer;
import com.ntak.pearlzip.ui.util.PearlZipFXUtil;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.ntak.pearlzip.archive.pub.ArchiveService.DEFAULT_BUS;

public abstract class AbstractZip4jTestFX extends AbstractPearlZipTestFX {

    @Override
    public void start(Stage stage) throws IOException, TimeoutException {
        System.setProperty("configuration.ntak.pearl-zip.no-files-history", "5");
        PearlZipFXUtil.initialise(stage, List.of(new Zip4jArchiveWriteService()),List.of(new Zip4jArchiveReadService()));
        ZipConstants.LOCAL_TEMP = Paths.get(System.getProperty("user.home"), ".pz", "temp");
        ZipConstants.STORE_TEMP = Paths.get(System.getProperty("user.home"), ".pz", "temp");
        DEFAULT_BUS.register(ErrorAlertConsumer.getErrorAlertConsumer());
    }

    @Override
    public void stop() {
        DEFAULT_BUS.unregister(ErrorAlertConsumer.getErrorAlertConsumer());
    }
}
