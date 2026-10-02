using board in self
using MoveGen in self
using engine in self

func main() -> int:
    board = init Board()

    searchDepth = 6

    moves: mut = board.legalMoves()

    while not moves.isEmpty():
        moves = board.legalMoves()

        moveInput = input("Next move: ")
        if strToLower(moveInput) == "exit":
            println("Exiting...")
            break

        // debug: perft
        if strToLower(moveInput) == "perft":
            for d in range(1, 6):
                println("perft " + intToStr(d) + ": " + intToStr(Engine::perft(board, d)))
            continue

        moveInputSplit = strSplit(moveInput, " ")

        // "fen <fen string>" sets up any position
        if moveInputSplit.size > 1 and strToLower(moveInputSplit.values[0]) == "fen":
            fen: mut = ""
            for i in range(1, moveInputSplit.size):
                if i > 1:
                    fen = fen + " "
                fen = fen + moveInputSplit.values[i]

            board.loadFen(fen)
            println(board)
            continue

        if moveInputSplit.size != 2 and moveInputSplit.size != 3:
            println("Expected move with format: <from> <to> [promotion piece: q, r, b or n]")
            continue

        from = Square::fromName(moveInputSplit.values[0])
        to = Square::fromName(moveInputSplit.values[1])

        // Promotes to a queen unless a third word says otherwise.
        wanted: mut = PieceType.QUEEN
        if moveInputSplit.size == 3:
            promotionInput = strToLower(moveInputSplit.values[2])

            if promotionInput == "r":
                wanted = PieceType.ROOK
            else if promotionInput == "b":
                wanted = PieceType.BISHOP
            else if promotionInput == "n":
                wanted = PieceType.KNIGHT

        move: mut Move = zero
        
        found: mut = false

        for i in range(len(moves)):
            tmp = moves.get(i)

            found = tmp.from == from and tmp.to == to and (tmp.promotion == PieceType.NONE or tmp.promotion == wanted)
            if found:
                move = tmp
                break

        if not found:
            println("Invalid move")
            continue

        board.makeMove(move)

        println(board)

        // The engine answers, unless the game just ended.
        moves = board.legalMoves()
        if moves.isEmpty():
            continue

        println("Engine is thinking...")
        result = Engine::findBestMove(board, searchDepth)

        println("Engine plays: " + result.best.toString())
        board.makeMove(result.best)

        println(board)

        moves = board.legalMoves()

    if moves.isEmpty():
        if MoveGenerator::isInCheck(board, board.sideToMove):
            println("Checkmate")
        else:
            println("Stalemate")

    return 0