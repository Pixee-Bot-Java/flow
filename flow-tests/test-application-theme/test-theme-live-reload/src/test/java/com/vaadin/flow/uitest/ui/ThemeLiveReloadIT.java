/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class ThemeLiveReloadIT extends ChromeBrowserTest {

    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";
    private static final String THEME_FOLDER = FrontendUtils.DEFAULT_FRONTEND_DIR
            + "/themes/app-theme/";

    private File baseDir;
    private File testStylesCSSFile;
    private File stylesCSSFile;
    private File fontFile;

    @Before
    public void init() {
        baseDir = new File(System.getProperty("user.dir", "."));
        final File themeFolder = new File(baseDir, THEME_FOLDER);

        File fontsDir = new File(themeFolder, "fonts");
        if (!fontsDir.exists() && !fontsDir.mkdir()) {
            Assert.fail("Unable to create fonts folder");
        }

        stylesCSSFile = new File(themeFolder, "styles.css");
        testStylesCSSFile = new File(themeFolder, "test-styles.css");
        fontFile = new File(themeFolder, "fonts/ostrich-sans-regular.ttf");
    }

    @After
    public void cleanUp() {
        doActionAndWaitUntilLiveReloadComplete(this::removeGeneratedFiles);
    }

    private void removeGeneratedFiles() {
        try {
            deleteTestStyles();
            deleteFile(fontFile);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't cleanup test files", e);
        }
    }

    @Test
    public void webpackLiveReload_newCssAndFontCreatedAndDeleted_stylesUpdatedOnFly() {
        open();
        Assert.assertFalse(
                "Red background is not expected before applying the styles",
                isCustomBackGroundColor());
        Assert.assertTrue(
                "Expected styles.css file to be auto-generated by flow",
                stylesCSSFile.exists());
        Assert.assertFalse("Expected no test-styles.css file exists before "
                + "applying the styles", testStylesCSSFile.exists());
        Assert.assertFalse("Expected no font file in the theme folder before "
                + "applying the font styles", fontFile.exists());

        // Live reload upon adding a new font file and a new styles.css
        copyFontFile();
        createStylesCssWithFontAndRedBackground();
        waitUntilCustomFont();
        waitUntilCustomBackgroundColor();

        // Live reload upon file deletion
        doActionAndWaitUntilLiveReloadComplete(this::deleteTestStyles);
        waitUntilInitialStyles();
    }

    private void waitUntilCustomBackgroundColor() {
        waitUntil(driver -> isCustomBackGroundColor());
    }

    private void waitUntilInitialStyles() {
        waitUntil(driver -> !isCustomBackGroundColor() && !isCustomFont());
    }

    private void waitUntilCustomFont() {
        waitUntil(driver -> isCustomFont());
    }

    private boolean isCustomBackGroundColor() {
        try {
            final WebElement html = findElement(By.tagName("html"));
            return RED_COLOR.equals(html.getCssValue("background-color"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean isCustomFont() {
        try {
            final WebElement body = findElement(By.tagName("html"));
            return "Ostrich".equals(body.getCssValue("font-family"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private void createStylesCssWithFontAndRedBackground() {
        try {
            // @formatter:off
            final String fontStyle =
                    "@font-face {" +
                    "    font-family: \"Ostrich\";" +
                    "    src: url(\"./fonts/" + fontFile.getName() + "\") format(\"TrueType\");" +
                    "}" +
                    "html {" +
                    "    font-family: \"Ostrich\";" +
                    "    background-color: " + RED_COLOR  + ";" +
                    "}";

            final String stylesCssContent = "@import \"./test-styles.css\";";
            // @formatter:on
            FileUtils.write(testStylesCSSFile, fontStyle,
                    StandardCharsets.UTF_8.name());
            FileUtils.write(stylesCSSFile, stylesCssContent,
                    StandardCharsets.UTF_8.name());
            waitUntil(driver -> testStylesCSSFile.exists());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to apply font and background styles", e);
        }
    }

    private void copyFontFile() {
        try {
            File copyFontFrom = new File(baseDir,
                    FrontendUtils.DEFAULT_FRONTEND_DIR
                            + "/fonts/ostrich-sans-regular.ttf");
            FileUtils.copyFile(copyFontFrom, fontFile);
            waitUntil(driver -> fontFile.exists());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy font file", e);
        }
    }

    private void deleteTestStyles() {
        cleanStylesCss();
        deleteFile(testStylesCSSFile);
    }

    private void cleanStylesCss() {
        try {
            FileUtils.write(stylesCSSFile, "", StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException("Failed to clean-up 'styles.css'", e);
        }
    }

    private void deleteFile(File fileToDelete) {
        if (fileToDelete != null && fileToDelete.exists()
                && !fileToDelete.delete()) {
            Assert.fail("Unable to delete " + fileToDelete);
        }
    }

    private void doActionAndWaitUntilLiveReloadComplete(Runnable action) {
        // Add a new active client with 'blocker' key and let the
        // waitForVaadin() to block until new page/document will be loaded as a
        // result of live reload.
        executeScript(
                "window.Vaadin.Flow.clients[\"blocker\"] = {isActive: () => true};");
        action.run();
        getCommandExecutor().waitForVaadin();
    }
}
