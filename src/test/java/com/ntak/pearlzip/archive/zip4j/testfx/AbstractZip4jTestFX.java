/*
 * Copyright © 2021 92AK
 */

package com.ntak.pearlzip.archive.zip4j.testfx;

import com.ntak.pearlzip.archive.util.LoggingUtil;
import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveReadService;
import com.ntak.pearlzip.archive.zip4j.pub.Zip4jArchiveWriteService;
import com.ntak.pearlzip.ui.constants.ZipConstants;
import com.ntak.pearlzip.ui.constants.internal.InternalContextCache;
import com.ntak.pearlzip.ui.mac.MacPearlZipApplication;
import com.ntak.pearlzip.ui.pub.SysMenuController;
import com.ntak.pearlzip.ui.util.AbstractPearlZipTestFX;
import com.ntak.pearlzip.ui.util.ErrorAlertConsumer;
import com.ntak.pearlzip.ui.util.PearlZipFXUtil;
import de.jangassen.MenuToolkit;
import de.jangassen.model.AppearanceMode;
import de.jangassen.platform.NativeAdapter;
import de.jangassen.platform.mac.MacNativeAdapter;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import static com.ntak.pearlzip.archive.constants.LoggingConstants.LOG_BUNDLE;
import static com.ntak.pearlzip.archive.pub.ArchiveService.DEFAULT_BUS;
import static com.ntak.pearlzip.ui.constants.ZipConstants.CNS_NTAK_PEARL_ZIP_APP_NAME;
import static com.ntak.pearlzip.ui.constants.ZipConstants.CNS_SYSMENU_WINDOW_TEXT;
import static com.ntak.pearlzip.ui.mac.MacZipConstants.*;

public abstract class AbstractZip4jTestFX extends AbstractPearlZipTestFX {

    @Override
    public void start(Stage stage) throws IOException, TimeoutException {
        System.setProperty("configuration.ntak.pearl-zip.no-files-history", "5");
        if (!InternalContextCache.INTERNAL_CONFIGURATION_CACHE.getAdditionalConfig(CK_MENU_TOOLKIT).isPresent()) {
            InternalContextCache.INTERNAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_MENU_TOOLKIT, MenuToolkit.toolkit(Locale.getDefault()));
        }

        PearlZipFXUtil.initialise(stage, List.of(new Zip4jArchiveWriteService()),List.of(new Zip4jArchiveReadService()));

        InternalContextCache.GLOBAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_LOCAL_TEMP, Paths.get(System.getProperty("user.home"),
                                                                                  ".pz", "temp"));
        InternalContextCache.GLOBAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_STORE_TEMP,
                                                                                    Paths.get(System.getProperty(
                                                                                            "user.home"), ".pz", "temp"));
        Path SETTINGS_FILE = Paths.get(System.getProperty(CNS_SETTINGS_FILE, Paths.get(System.getProperty("user.home"),
                                                                                       ".pz",
                                                                                       "settings.properties").toString()));
        InternalContextCache.INTERNAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_SETTINGS_FILE, SETTINGS_FILE);
        DEFAULT_BUS.register(ErrorAlertConsumer.getErrorAlertConsumer());
    }

    @Override
    public void stop() {
        DEFAULT_BUS.unregister(ErrorAlertConsumer.getErrorAlertConsumer());
    }

    public static void createSystemMenu(Stage aboutStage, List<javafx.scene.control.Menu> customMenus) throws IOException {
        ////////////////////////////////////////////
        ///// Create System Menu //////////////////
        //////////////////////////////////////////

        if (!InternalContextCache.INTERNAL_CONFIGURATION_CACHE.getAdditionalConfig(CK_MENU_TOOLKIT).isPresent()) {
            InternalContextCache.INTERNAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_MENU_TOOLKIT, MenuToolkit.toolkit(Locale.getDefault()));
        }

        // Create a new System Menu
        String appName = System.getProperty(CNS_NTAK_PEARL_ZIP_APP_NAME, "PearlZip");
        MenuBar sysMenu;
        final MenuToolkit menuToolkit = InternalContextCache.INTERNAL_CONFIGURATION_CACHE.<MenuToolkit>getAdditionalConfig(CK_MENU_TOOLKIT)
                                                    .get();

        if (!InternalContextCache.INTERNAL_CONFIGURATION_CACHE.getAdditionalConfig(CK_SYS_MENU).isPresent()) {

            sysMenu = new MenuBar();
            sysMenu.setUseSystemMenuBar(true);
            sysMenu.getMenus()
                   .add(menuToolkit.createDefaultApplicationMenu(appName,
                                                                 aboutStage));

            // Add some more Menus...
            FXMLLoader menuLoader = new FXMLLoader();
            menuLoader.setLocation(MacPearlZipApplication.class.getClassLoader()
                                                               .getResource("sysmenu.fxml"));
            menuLoader.setResources(LOG_BUNDLE);
            MenuBar additionalMenu = menuLoader.load();
            SysMenuController menuController = menuLoader.getController();
            menuController.initData();
            sysMenu.getMenus()
                   .addAll(additionalMenu.getMenus());
            InternalContextCache.INTERNAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_CORE_MENU_SIZE, sysMenu.getMenus().size());
        } else {
            sysMenu = InternalContextCache.INTERNAL_CONFIGURATION_CACHE.<MenuBar>getAdditionalConfig(CK_SYS_MENU)
                                  .get();;
        }
        InternalContextCache.INTERNAL_CONFIGURATION_CACHE.setAdditionalConfig(CK_SYS_MENU, sysMenu);
        int coreMenuSize = InternalContextCache.INTERNAL_CONFIGURATION_CACHE.<Integer>getAdditionalConfig(CK_CORE_MENU_SIZE)
                                       .get();
        sysMenu.getMenus().remove(coreMenuSize,sysMenu.getMenus().size());

        for (javafx.scene.control.Menu menu : customMenus) {
            sysMenu.getMenus().add(menu);
        }
        Menu tempMenu =
                InternalContextCache.INTERNAL_CONFIGURATION_CACHE.<Menu>getAdditionalConfig(CK_WINDOW_MENU).get();
        tempMenu.getItems().add(0,sysMenu.getMenus()
               .stream()
               .filter(m -> m.getText()
                             .equals(LoggingUtil.resolveTextKey(CNS_SYSMENU_WINDOW_TEXT)))
               .findFirst()
               .get()
        );

        // Use the menu sysMenu for all stages including new ones
        if (menuToolkit.systemUsesDarkMode()) {
            menuToolkit.setAppearanceMode(AppearanceMode.DARK);
        } else {
            menuToolkit.setAppearanceMode(AppearanceMode.LIGHT);
        }
        try {
            final NativeAdapter nativeAdapter = MacNativeAdapter.getInstance();
            nativeAdapter.setMenuBar(sysMenu.getMenus());
        } catch (Exception e) {
        }
    }
}
