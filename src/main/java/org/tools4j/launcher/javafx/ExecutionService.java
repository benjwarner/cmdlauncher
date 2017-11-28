package org.tools4j.launcher.javafx;

import javafx.scene.control.TextArea;
import org.tools4j.launcher.service.Command;
import org.tools4j.launcher.service.PostExecutionBehaviour;

/**
 * User: ben
 * Date: 21/11/17
 * Time: 5:52 PM
 */
public interface ExecutionService {
    ExecutingCommand exec(final Command command, final TextArea outputConsole, final PostExecutionBehaviour postExecutionBehaviour);
}
