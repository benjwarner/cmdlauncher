package org.tools4j.launcher.service;

import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.tools4j.launcher.javafx.ExecutionService;
import org.tools4j.launcher.javafx.Main;
import org.tools4j.launcher.util.PropertiesRepo;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.tools4j.launcher.service.LauncherUtils.verifyConsoleMode;
import static org.tools4j.launcher.service.LauncherUtils.verifyDataSearchMode;
import static org.tools4j.launcher.service.Utils.containsText;

/**
 * User: ben
 * Date: 24/11/17
 * Time: 7:02 AM
 */
public class TestLauncherWithOnlyOneCommandBusyProcess extends AbstractLauncherTest {

    @Override
    public ExecutionService getExecutionService() {
        //Just 3 seconds
        return super.getExecutionServiceWithBusyProcess(3);
    }

    @Override
    public String getWorkingDir() {
        return WORKING_DIR_CONTAINING_JUST_ONE_COMMAND;
    }

    @Test
    public void testLauncherWithOnlyOneCommand() throws InterruptedException {
        verifyDataSearchMode(false);
        clickOn(Ids.dataSearchBox).write("Uat").type(KeyCode.ENTER, 2);
        verifyThat(Ids.selectedDataLabel, hasText("hauu0001"));
        verifyConsoleMode();
        verifyThat(Ids.consoleLabel, containsText("Running"));
        Thread.sleep(3000);

        //Should be finished now
        verifyThat(Ids.consoleLabel, containsText("Finished"));

        clickOn(Ids.consoleOutput).type(KeyCode.ESCAPE);
        verifyDataSearchMode(true, "Uat");
        clickOn(Ids.dataSearchBox).type(KeyCode.ESCAPE);
        verifyDataSearchMode(false);
        verifyThat(Ids.dataSearchBox, hasText(""));
        assertFalse(destroyCalled.get());
    }
}
