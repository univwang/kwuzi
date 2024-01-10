package com.kob.botrunningsystem.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Bot implements java.util.function.Supplier<Integer> {

    private final int boardSize = 15;
    private final int maxDepth = 1; // 控制搜索深度

    @Override
    public Integer get() {
        File file = new File("input.txt");
        try {
            Scanner sc = new Scanner(file);
            return nextMove(sc.next());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer nextMove(String input) {
        int player = Integer.parseInt(input.substring(0, 1)) + 1;
        char[] board = input.substring(2).toCharArray();

        return findBestMove(board, player);
    }

    private Integer findBestMove(char[] board, int player) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        List<Integer> moves = availableSteps(board);

        for (int move : moves) {
            board[move] = Character.forDigit(player, 10);
            int score = minimax(board, maxDepth, false, player);
            board[move] = '0'; // 回溯
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove == -1 ? -1 : (bestMove / boardSize) * boardSize + (bestMove % boardSize);
    }

    private List<Integer> availableSteps(char[] board) {
        List<Integer> moves = new ArrayList<>();
        boolean hasChess = false;

        // 检查棋盘上是否有棋子
        for (char c : board) {
            if (c != '0') {
                hasChess = true;
                break;
            }
        }

        if (!hasChess) {
            // 如果没有棋子，只在中间位置添加一个步骤
            int center = (boardSize / 2) * boardSize + (boardSize / 2);
            moves.add(center);
        } else {
            // 如果有棋子，添加所有空位且周围有棋子的位置
            for (int i = 0; i < board.length; i++) {
                if (board[i] == '0' && hasNeighborChess(board, i)) {
                    moves.add(i);
                }
            }
        }

        return moves;
    }

    private boolean hasNeighborChess(char[] board, int index) {
        int row = index / boardSize;
        int col = index % boardSize;

        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                if (dRow == 0 && dCol == 0) continue; // 跳过自身
                int neighborRow = row + dRow;
                int neighborCol = col + dCol;
                if (isInsideBoard(neighborRow, neighborCol) && board[neighborRow * boardSize + neighborCol] != '0') {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }

    private int minimax(char[] board, int depth, boolean isMaximizing, int player) {
        // 检查是否已经赢得游戏或达到了递归深度限制
        if (depth == 0) {
            // System.out.println("score: " + (evaluateScore(board, player, isMaximizing)));
            // System.out.println("score: " + evaluateScore(board, 3 - player, !isMaximizing));
            return evaluateScore(board, player, isMaximizing) - evaluateScore(board, 3 - player, !isMaximizing);
        }
        if(isWinning(board, 3 - player)) return Integer.MIN_VALUE / 2;
        if(isWinning(board, player)) return Integer.MAX_VALUE / 2;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int move : availableSteps(board)) {
                board[move] = Character.forDigit(player, 10);
                int score = minimax(board, depth - 1, false, player);
                // System.out.println("Third move: " + move / boardSize + " " + move % boardSize + " score: " + score);
                board[move] = '0'; // 回溯
                bestScore = Math.max(score, bestScore);
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int move : availableSteps(board)) {
                board[move] = Character.forDigit(3 - player, 10);
                int score = minimax(board, depth - 1, true, player);
//                System.out.println("last move: " + move / boardSize + " " + move % boardSize + " score: " + score);
                board[move] = '0'; // 回溯
                bestScore = Math.min(score, bestScore);
            }
            return bestScore;
        }
    }


    private boolean isWinning(char[] board, int player) {
        char playerChar = Character.forDigit(player, 10);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[row * boardSize + col] == playerChar) {
                    // 检查水平方向
                    if (col <= boardSize - 5 && checkLine(board, row, col, 0, 1, playerChar)) return true;
                    // 检查垂直方向
                    if (row <= boardSize - 5 && checkLine(board, row, col, 1, 0, playerChar)) return true;
                    // 检查对角线方向
                    if (row <= boardSize - 5 && col <= boardSize - 5 && checkLine(board, row, col, 1, 1, playerChar)) return true;
                    // 检查反对角线方向
                    if (row >= 4 && col <= boardSize - 5 && checkLine(board, row, col, -1, 1, playerChar)) return true;
                }
            }
        }
        return false;
    }

    private boolean checkLine(char[] board, int row, int col, int dRow, int dCol, char playerChar) {
        for (int i = 0; i < 5; i++) {
            if (board[(row + i * dRow) * boardSize + (col + i * dCol)] != playerChar) return false;
        }
        return true;
    }


    private int evaluateScore(char[] board, int player, boolean isMaximizing) {
        //如果是false，表示是自己的回合，如果是true，表示是对手的回合
        int score = 0;

        // 评分标准
        int liveTwo = 50;
        int liveThree = 500;
        int liveFour = 10000;
        int fiveInRow = 100000; // 连五的得分
        int twoInRow = 10;
        int threeInRow = 100;
        int fourInRow = 1000;
        int centerBonus = 10; // 中央区域的加分

        if(isMaximizing) {
            //扩大三、四、五的权重
            liveThree = liveThree * 2;
            liveFour = liveFour * 2;
        }

        char playerChar = Character.forDigit(player, 10);

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[row * boardSize + col] == playerChar) {
                    // 检查水平、垂直和对角线方向
                    for (int dRow = -1; dRow <= 1; dRow++) {
                        for (int dCol = -1; dCol <= 1; dCol++) {
                            if (dRow == 0 && dCol == 0) continue;
                            int consecutive = 0;
                            int openEnds = 0;
                            int r = row, c = col;

                            // 向前检查
                            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board[r * boardSize + c] == playerChar) {
                                consecutive++;
                                r -= dRow;
                                c -= dCol;
                            }
                            if (isCellEmpty(board, r, c)) openEnds++;

                            r = row + dRow;
                            c = col + dCol;

                            // 向后检查
                            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board[r * boardSize + c] == playerChar) {
                                consecutive++;
                                r += dRow;
                                c += dCol;
                            }
                            if (isCellEmpty(board, r, c)) openEnds++;
                            // 评分
                            if (consecutive == 5) {
                                score += fiveInRow; // 连五
                            } else if (consecutive == 4) {
                                score += (openEnds == 2) ? liveFour : fourInRow;
                            } else if (consecutive == 3) {
                                score += (openEnds == 2) ? liveThree : threeInRow;
                            } else if (consecutive == 2) {
                                score += (openEnds == 2) ? liveTwo : twoInRow;
                            }
                        }
                    }
                }
            }
        }

        // 给中央区域的棋子加分
        for (int row = boardSize / 3; row < 2 * boardSize / 3; row++) {
            for (int col = boardSize / 3; col < 2 * boardSize / 3; col++) {
                if (board[row * boardSize + col] == playerChar) {
                    score += centerBonus;
                }
            }
        }

        // 检测特殊模式
        score += detectSpecialPatterns(board, player) * 5000;

        return score;
    }

    private boolean isCellEmpty(char[] board, int row, int col) {
        return isInsideBoard(row, col) && board[row * boardSize + col] == '0';
    }

    private int detectSpecialPatterns(char[] board, int player) {
        // ... detectSpecialPatterns 的实现
        return 0;
    }
}
