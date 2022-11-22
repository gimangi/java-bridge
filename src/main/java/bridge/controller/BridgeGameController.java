package bridge.controller;

import bridge.BridgeMaker;
import bridge.config.LoopActivity;
import bridge.config.ErrorMessageConstant;
import bridge.domain.Bridge;
import bridge.domain.BridgeGame;
import bridge.domain.BridgeTranslator;
import bridge.domain.model.Direction;
import bridge.domain.model.GameStatus;
import bridge.view.BridgeConsoleTranslator;
import bridge.view.CommandType;
import bridge.view.InputView;
import bridge.view.OutputView;

import java.util.List;

public class BridgeGameController extends LoopActivity {

    private final BridgeTranslator bridgeTranslator = new BridgeConsoleTranslator();
    private final BridgeMaker bridgeMaker;
    private final InputView inputView;
    private final OutputView outputView;
    private BridgeGame bridgeGame;


    public BridgeGameController(BridgeMaker bridgeMaker, InputView inputView, OutputView outputView) {
        this.bridgeMaker = bridgeMaker;
        this.inputView = inputView;
        this.outputView = outputView;
        validationConstructorParams();
    }


    @Override
    protected final void onStart() {
        outputView.printGameStart();
        executeUntilNoException(() -> createBridgeGame(readBridgeSize()));
    }

    @Override
    protected final void onLoop() {
        while (bridgeGame.getStatus() == GameStatus.RUNNING) {
            executeUntilNoException(this::proceedTurn);
        }
        if (bridgeGame.getStatus() == GameStatus.LOSE) {
            if (executeUntilNoException(this::enterRetry)) {
                bridgeGame.retry();
                return;
            }
        }
        stop();
    }

    @Override
    protected final void onStop() {
        outputView.printResult(bridgeGame, bridgeTranslator);
    }

    @Override
    protected void onError(Exception e) {
        if (e instanceof IllegalArgumentException) {
            outputView.printExpectedErrorMessage((IllegalArgumentException) e);
            return;
        }
        outputView.printUnexpectedErrorMessage(e);
    }

    private void validationConstructorParams() throws IllegalArgumentException {
        if (this.bridgeMaker == null || inputView == null || outputView == null) {
            throw new IllegalArgumentException(ErrorMessageConstant.PARAMS_HAVE_NULL_VALUE);
        }
    }

    private int readBridgeSize() throws IllegalArgumentException {
        outputView.printEnterBridgeLength();
        return inputView.readBridgeSize();
    }

    private void createBridgeGame(int bridgeSize) throws IllegalArgumentException {
        List<String> bridgeInfo = bridgeMaker.makeBridge(bridgeSize);
        Bridge bridge = new Bridge(bridgeInfo);
        bridgeGame = new BridgeGame(bridge);
    }

    private boolean enterRetry() throws IllegalArgumentException {
        outputView.printEnterGameRetry();
        CommandType command = inputView.readGameCommand();
        if (command == CommandType.GAME_QUIT) {
            return false;
        }
        bridgeGame.retry();
        return true;
    }

    private void proceedTurn() throws IllegalArgumentException {
        outputView.printEnterMoveDirection();
        Direction direction = mapToDirection(inputView.readMoving());
        moveToDirection(direction);
        outputView.printMap(bridgeGame, bridgeTranslator);
    }

    private void moveToDirection(Direction direction) throws IllegalArgumentException {
        try {
            bridgeGame.move(direction);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(ErrorMessageConstant.GAME_STATUS_NOT_ALLOW_MOVE);
        }
    }

    private static Direction mapToDirection(CommandType commandType) {
        return Direction.of(commandType.getCommand());
    }

}
