package org.tools4j.tabular.service;

import javafx.stage.Stage;
import org.junit.After;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.tools4j.tabular.javafx.ExecutionService;
import org.tools4j.tabular.javafx.Main;
import org.tools4j.tabular.util.PropertiesRepo;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 * User: ben
 * Date: 29/11/17
 * Time: 6:00 PM
 */
public class AbstractLauncherTest extends ApplicationTest {
    public static final String WORKING_DIR_CONTAINING_SEARCHABLE_COMMANDS = "src/test/resources/table_with_multiple_commands";
    public static final String WORKING_DIR_CONTAINING_JUST_ONE_COMMAND = "src/test/resources/table_with_single_command";
    public static final String WORKING_DIR_CONTAINING_ZERO_COMMANDS = "src/test/resources/table_no_commands";
    protected final AtomicBoolean destroyCalled = new AtomicBoolean(false);
    protected ExecutionService executionService;

    @Override
    public void init() throws TimeoutException {
        System.setProperty("workingDir", getWorkingDir());
    }

    @Override
    public void start(Stage stage) throws TimeoutException {
        destroyCalled.set(false);
        executionService = getExecutionService();
        final Main main = new Main(new PropertiesRepo(), executionService);
        try {
            main.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        FxToolkit.showStage();
    }

    @After
    public void tearDown() throws Exception {
        executionService.destroy();
    }

    public ExecutionService getExecutionService(){
        return getExecutionServiceWithSucessfullyFinished();
    }

    public String getWorkingDir() {
        return WORKING_DIR_CONTAINING_SEARCHABLE_COMMANDS;
    }

    public final ExecutionService getExecutionServiceWithBusyProcess(final int hangForSeconds){
        return new MockExecutionService(MockExecutionService.getBusyProcess(hangForSeconds, aVoid -> {
            destroyCalled.set(true);
            return null;
        }));
    }

    public final ExecutionService getExecutionServiceWithSucessfullyFinished(){
        return new MockExecutionService(MockExecutionService.getFinishedProcess());
    }

    public final ExecutionService getExecutionServiceWithFinishedWithErrors(){
        return new MockExecutionService(MockExecutionService.getFinishedWithErrorProcess());
    }
}
