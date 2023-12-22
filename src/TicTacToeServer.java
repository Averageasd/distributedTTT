import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TicTacToeServer extends Application implements TicTacToeConstants {

    // number of sessions
    private int sessionNo = 1;

    @Override
    public void start(Stage stage) throws Exception {
        TextArea taLog = new TextArea();

        Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
        stage.setTitle("TicTacToeServer");
        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            try {

                // create a server socket
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> {
                    taLog.appendText(new Date() + " : Server started at socket 8000\n");
                });

                while (true) {
                    Platform.runLater(() -> {
                        taLog.appendText(new Date() + " : Wait for players to join session " + sessionNo + '\n');
                    });

                    // connect to player 1
                    Socket player1 = serverSocket.accept();

                    // announce that player 1 joined
                    Platform.runLater(() -> {
                        taLog.appendText(new Date() + ": Player 1 joined session " + sessionNo + '\n');
                        taLog.appendText("Player 1's IP address " + player1.getInetAddress().getHostAddress() + '\n');
                    });

                    // notify that player just connected is player 1
                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

                    // connect to player 2
                    Socket player2 = serverSocket.accept();

                    // announce player 2 joined
                    Platform.runLater(() -> {
                        taLog.appendText(new Date() + " Player 2 joined session " + sessionNo + '\n');
                        taLog.appendText("Player 2's IP address " + player1.getInetAddress().getHostAddress() + '\n');
                    });

                    // notify to player 2 that they successfully joined
                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER2);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    // handling
    class HandleSession implements Runnable, TicTacToeConstants {

        private Socket player1;
        private Socket player2;

        private char[][] cell = new char[3][3];
        private DataInputStream fromPlayer1;
        private DataOutputStream toPlayer1;
        private DataInputStream fromPlayer2;
        private DataOutputStream toPlayer2;

        private boolean continueToPlay = true;

        public HandleSession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    cell[i][j] = ' ';
                }
            }
        }

        @Override
        public void run() {
            try {
                fromPlayer1 = new DataInputStream(player1.getInputStream());
                toPlayer1 = new DataOutputStream(player1.getOutputStream());
                fromPlayer2 = new DataInputStream(player2.getInputStream());
                toPlayer2 = new DataOutputStream(player2.getOutputStream());

                // tell player 1 to start
                toPlayer1.writeInt(1);

                while (true) {

                    // receive a move from player 1
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    cell[row][column] = 'X';

                    // check if player 1 won
                    if (isWon('X')) {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);

                        // show player1's updated move on player 2's side
                        sendMove(toPlayer2, row, column);
                        break;
                    } else if (isFull()) {
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        // show player1's updated move on player 2's side
                        sendMove(toPlayer2, row, column);
                        break;
                    } else {
                        toPlayer2.writeInt(CONTINUE);
                        // show player1's updated move on player 2's side
                        sendMove(toPlayer2, row, column);
                    }

                    // now we handle player 2's  input
                    row = fromPlayer2.readInt();


                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendMove(DataOutputStream out, int row, int column) throws IOException {

            // send row index
            out.writeInt(row);

            // send column index
            out.write(column);
        }

        private boolean isFull() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (cell[i][j] == ' ') {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isWon(char token) {

            // check all rows
            for (int i = 0; i < 3; i++) {
                if (cell[i][0] == token && cell[i][1] == token && cell[i][2] == token) {
                    return true;
                }
            }

            // check all cols
            for (int i = 0; i < 3; i++) {
                if (cell[0][i] == token && cell[1][i] == token && cell[2][i] == token) {
                    return true;
                }
            }

            // check diagonals
            if (cell[0][0] == token && cell[1][1] == token && cell[2][2] == token) {
                return true;
            }

            if (cell[0][2] == token && cell[1][1] == token && cell[2][0] == token) {
                return true;
            }

            return false;
        }
    }
}
