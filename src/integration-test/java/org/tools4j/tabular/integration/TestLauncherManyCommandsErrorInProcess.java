package org.tools4j.tabular.integration;

import javafx.scene.input.KeyCode;
import org.junit.Test;
import org.tools4j.tabular.javafx.ExecutionService;

import static org.junit.Assert.assertFalse;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.tools4j.tabular.integration.LauncherUtils.verifyCommandSearchMode;
import static org.tools4j.tabular.integration.LauncherUtils.verifyConsoleMode;
import static org.tools4j.tabular.integration.LauncherUtils.verifyDataSearchMode;
import static org.tools4j.tabular.integration.Utils.containsText;

/**
 * User: ben
 * Date: 24/11/17
 * Time: 7:02 AM
 */
public class TestLauncherManyCommandsErrorInProcess extends AbstractLauncherTest {

    @Override
    public ExecutionService getExecutionService() {
        return super.getExecutionServiceWithFinishedWithErrors();
    }

    @Override
    public String getWorkingDir() {
        return WORKING_DIR_CONTAINING_SEARCHABLE_COMMANDS;
    }

    @Test
    public void testLauncher() throws InterruptedException {
        verifyDataSearchMode(false);
        clickOn(Ids.dataSearchBox).write("Uat").type(KeyCode.ENTER, 2);
        verifyCommandSearchMode("hauu0001");
        clickOn(Ids.commandSearchBox).type(KeyCode.ENTER, 2);
        verifyConsoleMode();
        Thread.sleep(500);
        verifyThat(Ids.consoleLabel, containsText("Finished with error"));
        clickOn(Ids.consoleOutput).type(KeyCode.ESCAPE);
        verifyCommandSearchMode("hauu0001");
        clickOn(Ids.commandSearchBox).type(KeyCode.ESCAPE);
        verifyDataSearchMode(true, "Uat");
        clickOn(Ids.dataSearchBox).type(KeyCode.ESCAPE);
        verifyDataSearchMode(false);
        verifyThat(Ids.dataSearchBox, hasText(""));
        assertFalse(destroyCalled.get());
    }
}
