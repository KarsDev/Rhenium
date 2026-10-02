using collections.list

using board in self
using piece in self
using square in self
using move in self
using MoveGen in self

struct SearchResult inherits Writeable:
    best: Move
    score: int

impl SearchResult:
    func toString() -> str:
        return this.best.toString() + " (score " + intToStr(this.score) + ")"

namespace Engine:
    global SCORE_MATE = 30000
    global SCORE_INF = 1000000

    global BISHOP_PAIR_BONUS = 35

    global ROOK_OPEN_FILE = 24
    global ROOK_SEMI_OPEN_FILE = 12

    global ISOLATED_PAWN = -14
    global DOUBLED_PAWN = -12

    global PASSED_PAWN_RANK_3 = 18
    global PASSED_PAWN_RANK_4 = 30
    global PASSED_PAWN_RANK_5 = 50
    global PASSED_PAWN_RANK_6 = 75
    global PASSED_PAWN_RANK_7 = 110

    global TEMPO = 8

    // Non-pawn material. The king is scored as an endgame king.
    func endgameMaterial() -> int:
        return 2600

    func pieceValue(pieceType: PieceType) -> int:
        if pieceType == PieceType.PAWN:
            return 100
        if pieceType == PieceType.KNIGHT:
            return 320
        if pieceType == PieceType.BISHOP:
            return 330
        if pieceType == PieceType.ROOK:
            return 500
        if pieceType == PieceType.QUEEN:
            return 900
        return 0



    func absInt(x: int) -> int:
        if x < 0:
            return 0 - x
        return x

    // 6 on the four centre squares, 0 on the outer ring.
    func centrality(row: int, col: int) -> int:
        dr = Engine::absInt(2 * row - 7)
        dc = Engine::absInt(2 * col - 7)

        if dc > dr:
            return 7 - dc
        return 7 - dr

    // relRow is the row counted from the piece's own back rank (0..7).
    func pawnBonus(relRow: int, col: int, endgame: bool) -> int:
        if endgame:
            return relRow * 10

        bonus: mut = relRow * 5

        if (col == 3 or col == 4) and relRow >= 2:
            bonus = bonus + 10
        if col == 0 or col == 7:
            bonus = bonus - 5

        return bonus

    func pieceBonus(pieceType: PieceType, relRow: int, col: int) -> int:
        centre = Engine::centrality(relRow, col)

        if pieceType == PieceType.KNIGHT:
            return centre * 7 - 25
        if pieceType == PieceType.BISHOP:
            return centre * 3 - 8
        if pieceType == PieceType.ROOK:
            if relRow == 6:
                return 15
            return 0
        if pieceType == PieceType.QUEEN:
            return centre - 3
        return 0

    func kingBonus(relRow: int, col: int, endgame: bool) -> int:
        if endgame:
            return Engine::centrality(relRow, col) * 6

        // In middlegame it's better to stay in the owned side and prefer side castling.
        bonus: mut = 0 - relRow * 20
        if relRow == 0:
            bonus = 10
            if col <= 2 or col >= 6:
                bonus = 25
        return bonus

    // Counts pawns of `side` on the given file (0=a .. 7=h).
    func pawnFileCount(board: Board, col: int, side: Side) -> int:
        count: mut = 0
        for row in range(8):
            sq = row * 8 + col
            piece = board.getPiece(sq)
            if piece.getType() == PieceType.PAWN and piece.getSide() == side:
                count = count + 1
        return count

    // Open file: no pawns at all. Semi-open: no pawns of the rook's own side.
    func rookFileBonus(board: Board, col: int, side: Side) -> int:
        enemySide: mut = Side.BLACK
        if side == Side.BLACK:
            enemySide = Side.WHITE

        ownPawns = Engine::pawnFileCount(board, col, side)
        enemyPawns = Engine::pawnFileCount(board, col, enemySide)

        if ownPawns == 0 and enemyPawns == 0:
            return Engine::ROOK_OPEN_FILE
        if ownPawns == 0:
            return Engine::ROOK_SEMI_OPEN_FILE
        return 0

    // A pawn is passed if no enemy pawn on its file or an adjacent file can
    // ever block or capture it on its way up the board.
    func isPawnPassed(board: Board, row: int, col: int, side: Side) -> bool:
        enemySide: mut = Side.BLACK
        if side == Side.BLACK:
            enemySide = Side.WHITE

        startCol: mut = col - 1
        if startCol < 0:
            startCol = 0
        endCol: mut = col + 1
        if endCol > 7:
            endCol = 7

        for c in range(startCol, endCol + 1):
            for r in range(8):
                sq = r * 8 + c
                piece = board.getPiece(sq)
                if piece.getType() == PieceType.PAWN and piece.getSide() == enemySide:
                    if side == Side.WHITE and r > row:
                        return false
                    if side == Side.BLACK and r < row:
                        return false

        return true

    // relRow is distance from the pawn's own back rank (see pawnBonus above).
    func passedPawnBonus(relRow: int) -> int:
        if relRow <= 1:
            return 0
        if relRow == 2:
            return Engine::PASSED_PAWN_RANK_3
        if relRow == 3:
            return Engine::PASSED_PAWN_RANK_4
        if relRow == 4:
            return Engine::PASSED_PAWN_RANK_5
        if relRow == 5:
            return Engine::PASSED_PAWN_RANK_6
        return Engine::PASSED_PAWN_RANK_7

    // Static score in centipawns from the point of view of the side to move.
    func evaluate(board: Board) -> int:
        common: mut = 0 // everything except pawns and kings
        pawnsMid: mut = 0
        pawnsEnd: mut = 0
        nonPawn: mut = 0
        whiteKing: mut = -1
        blackKing: mut = -1
        whiteBishops: mut = 0
        blackBishops: mut = 0

        for sq in range(64):
            piece = board.getPiece(sq)
            pieceType = piece.getType()

            if pieceType == PieceType.NONE:
                continue

            row = Square::row(sq)
            col = Square::col(sq)

            relRow: mut = row
            sign: mut = 1
            if piece.getSide() == Side.BLACK:
                relRow = 7 - row
                sign = -1

            if pieceType == PieceType.KING:
                if sign == 1:
                    whiteKing = sq
                else:
                    blackKing = sq
                continue

            value = Engine::pieceValue(pieceType)

            if pieceType == PieceType.PAWN:
                ownFile = Engine::pawnFileCount(board, col, piece.getSide())
                leftFile: mut = 0
                rightFile: mut = 0
                if col > 0:
                    leftFile = Engine::pawnFileCount(board, col - 1, piece.getSide())
                if col < 7:
                    rightFile = Engine::pawnFileCount(board, col + 1, piece.getSide())

                structure: mut = 0
                if leftFile == 0 and rightFile == 0:
                    structure = structure + Engine::ISOLATED_PAWN
                if ownFile > 1:
                    structure = structure + Engine::DOUBLED_PAWN

                passedBonus: mut = 0
                if Engine::isPawnPassed(board, row, col, piece.getSide()):
                    passedBonus = Engine::passedPawnBonus(relRow)

                pawnsMid = pawnsMid + sign * (value + Engine::pawnBonus(relRow, col, false) + structure)
                pawnsEnd = pawnsEnd + sign * (value + Engine::pawnBonus(relRow, col, true) + structure + passedBonus)
            else:
                nonPawn = nonPawn + value

                extra: mut = 0
                if pieceType == PieceType.ROOK:
                    extra = Engine::rookFileBonus(board, col, piece.getSide())
                if pieceType == PieceType.BISHOP:
                    if sign == 1:
                        whiteBishops = whiteBishops + 1
                    else:
                        blackBishops = blackBishops + 1

                common = common + sign * (value + Engine::pieceBonus(pieceType, relRow, col) + extra)

        endgame = nonPawn < Engine::endgameMaterial()

        score: mut = common

        if endgame:
            score = score + pawnsEnd
        else:
            score = score + pawnsMid

        if whiteBishops >= 2:
            score = score + Engine::BISHOP_PAIR_BONUS
        if blackBishops >= 2:
            score = score - Engine::BISHOP_PAIR_BONUS

        if whiteKing >= 0:
            wkr = Square::row(whiteKing)
            wkc = Square::col(whiteKing)
            score = score + Engine::kingBonus(wkr, wkc, endgame)
        if blackKing >= 0:
            bkr = Square::row(blackKing)
            bkc = Square::col(blackKing)
            score = score - Engine::kingBonus(7 - bkr, bkc, endgame)

        if board.sideToMove == Side.WHITE:
            return score + Engine::TEMPO
        return 0 - score + Engine::TEMPO


    func sameMove(a: Move, b: Move) -> bool:
        return a.from == b.from and a.to == b.to and a.promotion == b.promotion

    // Lower buckets are searched first.
    func moveBucket(move: Move) -> int:
        if move.promotion == PieceType.QUEEN:
            return 1

        victim = move.capturedPiece.getType()

        if victim == PieceType.QUEEN:
            return 1
        if victim == PieceType.ROOK:
            return 2
        if victim == PieceType.BISHOP or victim == PieceType.KNIGHT:
            return 3
        if victim == PieceType.PAWN:
            return 4
        return 5

    func orderMoves(moves: List<Move>) -> List<Move>:
        ordered = init List<Move>()

        for bucket in range(1, 6):
            for i in range(len(moves)):
                move = moves.get(i)
                if Engine::moveBucket(move) == bucket:
                    ordered.add(move)

        return ordered

    // Returns a copy of moves with `first` moved to the front.
    func putFirst(moves: List<Move>, first: Move) -> List<Move>:
        ordered = init List<Move>()
        ordered.add(first)

        for i in range(len(moves)):
            move = moves.get(i)
            if not Engine::sameMove(move, first):
                ordered.add(move)

        return ordered

    // Looks only at captures and promotions - except when in check, where
    // standing pat isn't legal, so it falls back to searching every evasion.
    // The static evaluation is never taken in the middle of a capture sequence.
    func quiesce(board: Board, alpha: int, beta: int, ply: int) -> int:
        inCheck = MoveGenerator::isInCheck(board, board.sideToMove)

        if inCheck:
            // Capped separately from the main search's check-extension limit so a
            // run of forced checks can't recurse indefinitely.
            if ply >= 64:
                return Engine::evaluate(board)

            moves = Engine::orderMoves(board.legalMoves())

            if moves.isEmpty():
                return ply - Engine::SCORE_MATE

            a: mut = alpha
            for i in range(len(moves)):
                move = moves.get(i)

                undo = board.saveState(move)
                board.makeMove(move)
                score = 0 - Engine::quiesce(board, 0 - beta, 0 - a, ply + 1)
                board.unmakeMove(undo)

                if score >= beta:
                    return beta
                if score > a:
                    a = score

            return a

        stand = Engine::evaluate(board)

        if stand >= beta:
            return beta

        a: mut = alpha
        if stand > a:
            a = stand

        noisy = MoveGenerator::generateLegal(board, board.sideToMove, true)
        moves = Engine::orderMoves(noisy)

        for i in range(len(moves)):
            move = moves.get(i)

            undo = board.saveState(move)
            board.makeMove(move)
            score = 0 - Engine::quiesce(board, 0 - beta, 0 - a, ply + 1)
            board.unmakeMove(undo)

            if score >= beta:
                return beta
            if score > a:
                a = score

        return a

    // Negamax with alpha-beta pruning (fail-hard).
    // Scores are always from the point of view of the side to move.
    func negamax(board: Board, depth: int, alpha: int, beta: int, ply: int) -> int:
        inCheck = MoveGenerator::isInCheck(board, board.sideToMove)

        // Check extension: never stop searching while in check.
        d: mut = depth
        if inCheck and ply < 32:
            d = d + 1

        if d <= 0:
            return Engine::quiesce(board, alpha, beta, ply)

        moves = Engine::orderMoves(board.legalMoves())

        if moves.isEmpty():
            if inCheck:
                // Checkmated. Prefer the furthest mate when losing and the nearest when winning.
                return ply - Engine::SCORE_MATE
            return 0

        a: mut = alpha

        for i in range(len(moves)):
            move = moves.get(i)

            undo = board.saveState(move)
            board.makeMove(move)
            score = 0 - Engine::negamax(board, d - 1, 0 - beta, 0 - a, ply + 1)
            board.unmakeMove(undo)

            if score >= beta:
                return beta
            if score > a:
                a = score

        return a

    // Finds the best move from depth 1 up to maxDepth deepening.
    // The side to move must have at least one legal move.
    func findBestMove(board: Board, maxDepth: int) -> SearchResult:
        moves = board.legalMoves()

        if moves.isEmpty():
            raise "findBestMove called with no legal moves"

        if len(moves) == 1:
            return init SearchResult(moves.get(0), 0)

        // The best move is carried between depths as an int index into `base`,
        // not as a Move variable reassigned from an inner scope: in this language
        // that kind of assignment silently kept the old value (the engine always
        // "chose" its first move even when the loop had found a better one).
        base = Engine::orderMoves(moves)
        bestIndex: mut = 0
        bestScore: mut = 0

        for depth in range(1, maxDepth + 1):
            // Search the previous iteration's best move first (better pruning).
            ordered = Engine::putFirst(base, base.get(bestIndex))

            alpha: mut = 0 - Engine::SCORE_INF
            iterationBestIndex: mut = 0

            for i in range(len(ordered)):
                move = ordered.get(i)
                undo = board.saveState(move)
                board.makeMove(move)

                score: mut = 0
                if i == 0:
                    // First move: full, open window, so its score is exact.
                    score = 0 - Engine::negamax(board, depth - 1, 0 - Engine::SCORE_INF, Engine::SCORE_INF, 1)
                else:
                    // Cheap scout: is this move at least as good as the current best?
                    score = 0 - Engine::negamax(board, depth - 1, 0 - (alpha + 1), 0 - alpha, 1)
                    if score > alpha:
                        // Fail-hard cutoffs report exactly the bound they were tested
                        // against, never more, so `score` here could be understating
                        // this move's real value. Re-search with an open window to get
                        // the true score before it's allowed to become the new best.
                        score = 0 - Engine::negamax(board, depth - 1, 0 - Engine::SCORE_INF, Engine::SCORE_INF, 1)

                board.unmakeMove(undo)

                if score > alpha:
                    alpha = score
                    iterationBestIndex = i

            // `ordered` is `base` with the previous best moved to the front, so
            // translate the winning position in `ordered` back to a position in `base`.
            newBestIndex: mut = bestIndex
            if iterationBestIndex > 0:
                if iterationBestIndex <= bestIndex:
                    newBestIndex = iterationBestIndex - 1
                else:
                    newBestIndex = iterationBestIndex
            bestIndex = newBestIndex
            bestScore = alpha

            println("depth " + intToStr(depth) + ": " + base.get(bestIndex).toString() + " (score " + intToStr(bestScore) + ")")

            // Force mate found, deeper search can't improve it
            if Engine::absInt(bestScore) > Engine::SCORE_MATE - 200:
                break

        return init SearchResult(base.get(bestIndex), bestScore)


    // Counts leaf nodes of the legal move tree. From the start position
    // this must be 20, 400, 8902, 197281, 4865609 for depths 1 to 5.
    func perft(board: Board, depth: int) -> int:
        if depth == 0:
            return 1

        moves = board.legalMoves()

        if depth == 1:
            return len(moves)

        total: mut = 0

        for i in range(len(moves)):
            move = moves.get(i)

            undo = board.saveState(move)
            board.makeMove(move)
            total = total + Engine::perft(board, depth - 1)
            board.unmakeMove(undo)

        return total
