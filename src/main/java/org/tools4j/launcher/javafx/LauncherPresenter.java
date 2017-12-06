package org.tools4j.launcher.javafx;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.log4j.Logger;
import org.tools4j.launcher.service.Command;
import org.tools4j.launcher.service.DataSetContext;
import org.tools4j.launcher.service.MutablePartIndex;
import org.tools4j.launcher.service.PartIndex;
import org.tools4j.launcher.service.PostExecutionBehaviour;
import org.tools4j.launcher.service.Result;
import org.tools4j.launcher.service.Results;
import org.tools4j.launcher.service.RowWithCommands;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ben
 * Date: 8/01/15
 * Time: 7:14 AM
 */
public class LauncherPresenter implements Initializable {
    private final static Logger LOG = Logger.getLogger(LauncherPresenter.class);

    @FXML
    public Label textTitle;
    @FXML
    public Label textTags;
    @FXML
    public Label labelLogo;
    @FXML
    public Label separatorLabel;
    @FXML
    public Label selectedDataLabel;
    @FXML
    public BorderPane listViewResultsParentPane;
    @FXML
    public Region resultDetailsScrollPane;
    @FXML
    public VBox mainPane;
    @FXML
    public Pane textSearchPane;
    @FXML
    public Pane commandSearchPane;
    @FXML
    public Pane whiteSpaceCover;
    @FXML
    public Pane dataSearchPane;
    @FXML
    public TextArea consoleOutput;
    @FXML
    public Pane consoleOutputContentPane;
    @FXML
    public Label consoleLabel;
    @FXML
    private TextField dataSearchBox;
    @FXML
    private TextField commandSearchBox;
    @FXML
    private TableView<Result<RowWithCommands>> dataTableView;
    @FXML
    private TableView<Result<Command>> commandTableView;
    @FXML
    private Pane dataTableContentPane;
    @FXML
    private Pane commandTableContentPane;
    @FXML
    private Pane outerTablePane;


    private static double xOffset = 0;
    private static double yOffset = 0;

    @Inject
    private DataSetContext dataSetContext;

    @Inject
    private Stage stage;

    @Inject
    private ExecutionService executionService;

    private ObservableList<Result<RowWithCommands>> dataTableItems;
    private ObservableList<Result<Command>> commandTableItems;

    private ExpandCollapseHelper expandCollapseHelper;
    private boolean skipCommandSearch;
    private boolean zeroCommandsConfigured;

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle){
        try {
            final PropertyPersistenceService tablePropertySaveService = new PropertyPersistenceServiceImpl("tables");
            final AtomicReference<ExecutingCommand> executingCommand = new AtomicReference<>();
            skipCommandSearch = dataSetContext.skipCommandSearch();
            zeroCommandsConfigured = dataSetContext.zeroCommandsConfigured();

            final ExecutionEnvironment executionEnvironment = new ExecutionEnvironment(executionService, consoleOutput,
                new PostExecutionBehaviour(
                    aVoid -> {
                        //Running
                        Platform.runLater(() -> {
                            consoleOutput.getStyleClass().removeAll("error", "finished");
                            consoleOutput.getStyleClass().add("running");
                            consoleLabel.getStyleClass().removeAll("error", "finished");
                            consoleLabel.getStyleClass().add("running");
                            consoleLabel.setText("Running... Press [ESC] to forcibly stop.  Press [ENTER] to allow process to complete in the background.");
                        });
                        return null;
                    }, aVoid -> {
                        //Finished
                        Platform.runLater(() -> {
                            consoleOutput.getStyleClass().removeAll("error", "running");
                            consoleOutput.getStyleClass().add("finished");
                            consoleLabel.getStyleClass().removeAll("error", "running");
                            consoleLabel.getStyleClass().add("finished");
                            consoleLabel.setText("Finished. Press [ENTER] to minimize.  [ESC] to run another command.");
                            if(dataSetContext.getProperties().getAsBoolean("app.close.console.on.command.finish", false)){
                                exitConsoleCollapseAndMinimize();
                            }
                        });
                        return null;
                    }, aVoid -> {
                        //Finished with error
                        Platform.runLater(() -> {
                            consoleOutput.getStyleClass().removeAll("finished", "running");
                            consoleOutput.getStyleClass().add("error");
                            consoleLabel.getStyleClass().removeAll("finished", "running");
                            consoleLabel.getStyleClass().add("error");
                            consoleLabel.setText("Finished with error. Press [ENTER] to minimize.  [ESC] to run another command.");
                        });
                        return null;
                    }
                )
            );

            new ShortcutInstaller( stage, dataSearchBox, dataSetContext.getProperties()).install();

            expandCollapseHelper = new ExpandCollapseHelper(stage, textSearchPane, outerTablePane);
            labelLogo.setText(">");
            separatorLabel.setText(">");
            expandCollapseHelper.setExpandedMode(false);

            //Setup dataTableView
            dataTableItems = FXCollections.observableArrayList();
            dataTableView.setItems(dataTableItems);
            for(final String columnName: dataSetContext.getDataColumnsToDisplay()){
                final TableColumn<Result<RowWithCommands>, String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Result<RowWithCommands>, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(final TableColumn.CellDataFeatures<Result<RowWithCommands>, String> param) {
                        return new SimpleStringProperty(param.getValue().get(param.getTableColumn().getText()));
                    }
                });
                dataTableView.getColumns().add(column);
            }
            final PropertySaveTableHelper<Result<RowWithCommands>> dataTableSaveHelper = new PropertySaveTableHelper<>(dataTableView, "dataTableView", tablePropertySaveService);
            dataTableSaveHelper.init();

            //Setup commandTableView
            commandTableItems = FXCollections.observableArrayList();
            commandTableView.setItems(commandTableItems);
            for(final String columnName: dataSetContext.getCommandColumnsToDisplay()){
                final TableColumn<Result<Command>, String> column = new TableColumn<>(columnName);
                column.setPrefWidth(200.0);


                column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(param.getTableColumn().getText())));
                commandTableView.getColumns().add(column);
            }
            final PropertySaveTableHelper<Result<Command>> commandTableSaveHelper = new PropertySaveTableHelper<>(commandTableView, "commandTableView", tablePropertySaveService);
            commandTableSaveHelper.init();

            //Indexes
            final PartIndex<RowWithCommands> dataIndex = new PartIndex<>(dataSetContext.getDataSet());
            final MutablePartIndex<Command> commandIndex = new MutablePartIndex<>();

            dataSearchBox.setOnKeyReleased((KeyEvent) -> {
                LOG.debug("dataSearchBox.onKeyReleased");
                if (dataSearchBox.getText() != null && dataSearchBox.getText().length() > 0) {
                    final Results<RowWithCommands> results = dataIndex.search(dataSearchBox.getText()).withAllWordsMatching();
                    LOG.debug(results.toPrettyString());
                    if (results.size() > 0) {
                        dataTableItems.clear();
                        if (!expandCollapseHelper.isContentVisible()) {
                            expandCollapseHelper.setExpandedMode(true);
                        }
                        for (final Result<RowWithCommands> result: results) {
                            try {
                                dataTableItems.add(result);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        dataTableView.getSelectionModel().selectFirst();
                    }
                }
            });

            commandSearchBox.setOnKeyReleased((KeyEvent) -> {
                LOG.debug("commandSearchBox.onKeyReleased");

                if(KeyEvent.getCode() == KeyCode.ENTER){
                    return;
                }
                if (commandSearchBox.getText() != null
                        && commandSearchBox.getText().length() > 0
                            && !(commandSearchBox.getSelectedText() != null
                            && commandSearchBox.getSelectedText().length() > 0
                            && commandSearchBox.getSelectedText().equals(commandSearchBox.getText()))) {

                    final Results<Command> results = commandIndex.search(commandSearchBox.getText()).withAllWordsMatching();
                    LOG.debug(results.toPrettyString());
                    if (results.size() > 0) {
                        commandTableItems.clear();
                        for (final Result<Command> result: results) {
                            try {
                                commandTableItems.add(result);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        dataTableView.getSelectionModel().selectFirst();
                    }
                }
            });


            dataSearchBox.setOnKeyPressed((KeyEvent e) -> {
                if (e.getCode() == KeyCode.DOWN) {
                    dataTableView.requestFocus();
                    dataTableView.getSelectionModel().selectNext();

                } else if (e.getCode() == KeyCode.UP) {
                    dataTableView.requestFocus();
                    dataTableView.getSelectionModel().selectNext();

                } else if (e.getCode() == KeyCode.ENTER) {
                    dataTableView.requestFocus();
                    dataTableView.getSelectionModel().selectFirst();

                } else if (e.getCode() == KeyCode.ESCAPE) {
                    LOG.debug("Escape key pressed from dataSearchBox");
                    onDataSearchEscapeKeyPressed();

                } else if (e.isControlDown() && !e.isShiftDown()) {
                    if (e.getCode() == KeyCode.R) {
                        LOG.debug("TODO: rebuild indexes");

                    } else if (e.getCode() == KeyCode.Q) {
                        Platform.exit();
                        System.exit(0);
                    }

                } else if (e.isControlDown() && e.isShiftDown()) {
                    if (e.getCode() == KeyCode.D) {
                        debugGui(mainPane);
                    } else if (e.getCode() == KeyCode.M) {
                        installDebugClickEventHandlerRecursivelyOnNode(mainPane);
                    }
                }
            });

            commandSearchBox.setOnKeyPressed((KeyEvent e) -> {
                if (e.getCode() == KeyCode.DOWN) {
                    commandTableView.requestFocus();
                    commandTableView.getSelectionModel().selectNext();

                } else if (e.getCode() == KeyCode.UP) {
                    commandTableView.requestFocus();
                    commandTableView.getSelectionModel().selectNext();

                } else if (e.getCode() == KeyCode.ENTER) {
                    commandTableView.requestFocus();
                    commandTableView.getSelectionModel().selectFirst();

                } else if (e.getCode() == KeyCode.ESCAPE) {
                    LOG.debug("Escape key pressed from commandSearchBox");
                    returnToDataSearchMode();

                } else if (e.isControlDown() && !e.isShiftDown()) {
                    if (e.getCode() == KeyCode.R) {
                        LOG.debug("TODO: rebuild indexes");

                    } else if (e.getCode() == KeyCode.Q) {
                        Platform.exit();
                        System.exit(0);
                    }

                } else if (e.isControlDown() && e.isShiftDown()) {
                    if (e.getCode() == KeyCode.D) {
                        debugGui(mainPane);
                    } else if (e.getCode() == KeyCode.M) {
                        installDebugClickEventHandlerRecursivelyOnNode(mainPane);
                    }
                }
            });


            dataTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    LOG.debug("New value selected");
                }
            });

            commandTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    final String valueToDisplayInPrompt = dataSetContext.getValueToDisplayWhenCommandRowSelected(newValue, commandSearchBox.getText());
                    commandSearchBox.setText(valueToDisplayInPrompt);
                }
            });


            dataTableView.setOnKeyPressed((KeyEvent e) -> {
                if (e.getCode() == KeyCode.RIGHT) {
                    LOG.debug("Gone right");

                } else if (e.getCode() == KeyCode.ENTER) {
                    LOG.debug("Enter key pressed:" + e);
                    if(!zeroCommandsConfigured) {
                        final RowWithCommands selectedRow = dataTableView.getSelectionModel().getSelectedItem().getRow();
                        selectedDataLabel.setText(dataSetContext.getValueToDisplayWhenDataRowSelected(selectedRow, dataSearchBox.getText()));
                        dataSearchPane.setVisible(false);
                        dataTableContentPane.setVisible(false);
                        commandSearchPane.setVisible(true);

                        if (skipCommandSearch) {
                            final Command command = selectedRow.getCommands().get(0);
                            commandSearchBox.setText(dataSetContext.getValueToDisplayWhenCommandRowSelected(command));
                            commandSearchBox.setEditable(false);
                            executeCommandAndEnterConsoleMode(executingCommand, executionEnvironment, command);

                        } else {
                            commandIndex.update(selectedRow.getCommandsTable());
                            commandTableItems.clear();
                            commandTableItems.setAll(commandIndex.returnAll());
                            commandTableContentPane.setVisible(true);
                            commandSearchBox.clear();
                            commandSearchBox.requestFocus();
                        }
                    }
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    LOG.debug("Escape key pressed from dataTable");
                    onDataSearchEscapeKeyPressed();
                }
            });

            commandTableView.setOnKeyPressed((KeyEvent e) -> {
                if (e.getCode() == KeyCode.RIGHT) {
                    LOG.debug("Gone right");

                } else if (e.getCode() == KeyCode.ENTER) {
                    final Command selectedRow = commandTableView.getSelectionModel().getSelectedItem().getRow();
                    executeCommandAndEnterConsoleMode(executingCommand, executionEnvironment, selectedRow);

                } else if (e.getCode() == KeyCode.ESCAPE) {
                    commandSearchBox.requestFocus();
                    commandSearchBox.selectAll();
                }
            });

            consoleOutput.setOnKeyPressed((KeyEvent e) -> {
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                    final ExecutingCommand executingCommandInstance = executingCommand.get();
                    if(executingCommandInstance != null && !executingCommandInstance.isFinished()){
                        if (e.getCode() == KeyCode.ESCAPE) {
                            LOG.warn("Forcibly stopping command...");
                            executingCommandInstance.stop();
                            executingCommand.set(null);
                        } else if (e.getCode() == KeyCode.ENTER) {
                            LOG.info("Letting process complete...");
                            executingCommand.set(null);
                            consoleLabel.setText("Letting process complete in the background... Press [ENTER] to minimize. [ESC] to run another command.");
                        }
                        return;
                    }
                }
                if (e.getCode() == KeyCode.ENTER) {
                    exitConsoleCollapseAndMinimize();

                } else if (e.getCode() == KeyCode.ESCAPE) {
                    exitConsoleMode();
                }
            });

            labelLogo.setOnMousePressed(event -> {
                xOffset = stage.getX() - event.getScreenX();
                yOffset = stage.getY() - event.getScreenY();
            });

            labelLogo.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() + xOffset);
                stage.setY(event.getScreenY() + yOffset);
            });

            //whiteSpaceCover.toFront();


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void executeCommandAndEnterConsoleMode(final AtomicReference<ExecutingCommand> executingCommand, final ExecutionEnvironment executionEnvironment, final Command selectedRow) {
        consoleOutput.clear();
        final ExecutingCommand executingCommandInstance = executionEnvironment.exec(selectedRow);
        executingCommand.set(executingCommandInstance);
        enterConsoleMode();
    }

    private void enterConsoleMode() {
        consoleOutputContentPane.setVisible(true);
        commandTableContentPane.setVisible(false);
        consoleOutput.requestFocus();
        commandSearchBox.setEditable(false);
    }

    private void exitConsoleCollapseAndMinimize() {
        exitConsoleMode();
        returnToDataSearchModeCollapseAndMinimize();
    }

    private void exitConsoleMode() {
        consoleOutputContentPane.setVisible(false);
        if(skipCommandSearch){
            returnToDataSearchMode();
        } else {
            returnToCommandSearchMode();
        }
    }

    private void returnToCommandSearchMode() {
        commandTableContentPane.setVisible(true);
        commandSearchBox.setEditable(true);
        commandSearchBox.requestFocus();
        commandSearchBox.selectAll();
    }

    private void returnToDataSearchMode() {
        commandTableContentPane.setVisible(false);
        dataTableContentPane.setVisible(true);
        dataSearchPane.setVisible(true);
        commandSearchPane.setVisible(false);
        dataSearchBox.requestFocus();
        dataSearchBox.selectAll();
    }

    private void returnToDataSearchModeCollapseAndMinimize() {
        returnToDataSearchMode();
        clearResultsAndCollapseExpansion();
        minimize();
    }

    private void installDebugClickEventHandlerRecursivelyOnNode(final Node node) {
        LOG.debug("Installing mouseClicked event handler on node: " + node);
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> LOG.debug(node.getClass().getSimpleName()
                + " id:" + node.getId()
                + " styleClass:" + node.getStyleClass()
                + " style:" + node.getStyle()));
        if (node instanceof Parent) {
            final Parent parent = (Parent) node;
            parent.getChildrenUnmodifiable().forEach(this::installDebugClickEventHandlerRecursivelyOnNode);
        }
    }

    private void debugGui(final Node node) {
        debugGui(node, "");
    }

    private void debugGui(final Node node, String currentIndentLevel) {
        LOG.debug(currentIndentLevel
                + node.getClass().getSimpleName()
                + " id:" + node.getId()
                + " styleClass:" + node.getStyleClass()
                + " style:" + node.getStyle());
        if (node instanceof Parent) {
            final Parent parent = (Parent) node;
            parent.getChildrenUnmodifiable().forEach((child) -> debugGui(child, currentIndentLevel + "    "));
        }
    }

    private void onDataSearchEscapeKeyPressed() {
        if (expandCollapseHelper.isContentVisible()) {
            clearResultsAndCollapseExpansion();
        } else {
            minimize();
        }
    }

    private void clearResultsAndCollapseExpansion() {
        clearAndReset();
        dataSearchBox.clear();
        dataSearchBox.requestFocus();
    }

    private void minimize() {
        stage.setIconified(true);
    }


    private void clearAndReset() {
        dataTableItems.clear();
        expandCollapseHelper.setExpandedMode(false);
    }
}
