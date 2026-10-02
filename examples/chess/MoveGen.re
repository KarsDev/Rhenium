using collections.list

using board in self
using piece in self
using square in self
using move in self
using MoveGen in self

namespace MoveGenerator:

    func generate(board: Board, side: Side) -> List<Move>:
        moves = init List<Move>()

        for square in range(64):
            piece = board.getPiece(square)

            if piece.getSide() != side:
                continue

            match (piece.getType()):
                PieceType.PAWN:
                    MoveGenerator::generatePawnMoves(board, square, moves)

                PieceType.KNIGHT:
                    MoveGenerator::generateKnightMoves(board, square, moves)

                PieceType.BISHOP:
                    MoveGenerator::generateBishopMoves(board, square, moves)

                PieceType.ROOK:
                    MoveGenerator::generateRookMoves(board, square, moves)

                PieceType.QUEEN:
                    MoveGenerator::generateQueenMoves(board, square, moves)

                PieceType.KING:
                    MoveGenerator::generateKingMoves(board, square, moves)
                    MoveGenerator::generateCastlingMoves(board, square, moves)

        return moves


    // Every pseudo-legal move that isn't en passant goes through here, so this
    // is also where a pawn reaching the last row turns into four promotion moves.
    // Queen comes first so it is what a "first match" lookup finds.
    // The square of the given side's king, or -1 if there isn't one.
    func findKing(board: Board, side: Side) -> Square:
        for square in range(64):
            piece = board.getPiece(square)

            if piece.getType() == PieceType.KING and piece.getSide() == side:
                return square

        return -1


    func isInCheck(board: Board, side: Side) -> bool:
        kingSquare = MoveGenerator::findKing(board, side)

        if kingSquare < 0:
            return false
            
        opposite = Side::opposite(side)
        return MoveGenerator::isSquareAttacked(board, kingSquare, opposite)


    // generate() plus a legality filter: each pseudo-legal move is played, and
    // dropped if it leaves the mover's king attacked. `side` must be the side to
    // move on the board. With capturesOnly, only captures and promotions are
    // returned (used by the engine's quiescence search).
    func generateLegal(board: Board, side: Side, capturesOnly: bool) -> List<Move>:
        pseudo = MoveGenerator::generate(board, side)
        legal = init List<Move>()

        for i in range(len(pseudo)):
            move = pseudo.get(i)

            if capturesOnly and move.capturedPiece.getType() == PieceType.NONE and move.promotion == PieceType.NONE:
                continue

            undo = board.saveState(move)
            board.makeMove(move)

            // makeMove handed the turn over, so `side` is the side that just moved.
            if not MoveGenerator::isInCheck(board, side):
                legal.add(move)

            board.unmakeMove(undo)

        return legal


    func addMove(board: Board, from: Square, to: Square, moves: List<Move>):
        movedPiece = board.getPiece(from)
        capturedPiece = board.getPiece(to)

        toRow = Square::row(to)

        if movedPiece.getType() == PieceType.PAWN and (toRow == 0 or toRow == 7):
            moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.QUEEN))
            moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.ROOK))
            moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.BISHOP))
            moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.KNIGHT))
            return

        moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.NONE))


    // Adds a sliding ray for bishops, rooks and queens.
    func addRay(board: Board, square: Square, rowDelta: int, colDelta: int, moves: List<Move>):
        startRow = Square::row(square)
        startCol = Square::col(square)

        for step in range(1, 8):
            row = startRow + rowDelta * step
            col = startCol + colDelta * step

            if row < 0 or row >= 8 or col < 0 or col >= 8:
                break

            target = Square::at(row, col)
            piece = board.getPiece(target)

            if piece.getType() == PieceType.NONE:
                MoveGenerator::addMove(board, square, target, moves)
                continue

            if piece.getSide() != board.getPiece(square).getSide():
                MoveGenerator::addMove(board, square, target, moves)

            // Any occupied square stops a sliding piece.
            break


    func addJump(board: Board, square: Square, rowDelta: int, colDelta: int, moves: List<Move>):
        row = Square::row(square) + rowDelta
        col = Square::col(square) + colDelta

        if row < 0 or row >= 8 or col < 0 or col >= 8:
            return

        target = Square::at(row, col)
        targetPiece = board.getPiece(target)
        movingPiece = board.getPiece(square)

        if targetPiece.getSide() == movingPiece.getSide():
            return

        MoveGenerator::addMove(board, square, target, moves)

    func addEnPassantMove(board: Board, from: Square, to: Square, moves: List<Move>):
        movedPiece = board.getPiece(from)

        // The captured pawn is on the origin row, in the destination column.
        fromSquare = Square::row(from)
        toSquare = Square::col(to)
        capturedSquare = Square::at(fromSquare, toSquare)
        capturedPiece = board.getPiece(capturedSquare)

        moves.add(init Move(from, to, movedPiece, capturedPiece, PieceType.NONE))

    func generatePawnMoves(board: Board, square: Square, moves: List<Move>):
        piece = board.getPiece(square)

        row = Square::row(square)
        col = Square::col(square)

        direction: mut = 1
        startRow: mut = 1

        if piece.getSide() == Side.BLACK:
            direction = -1
            startRow = 6

        // One square forward
        oneRow = row + direction

        if oneRow >= 0 and oneRow < 8:
            oneForward = Square::at(oneRow, col)

            if board.isEmpty(oneForward):
                MoveGenerator::addMove(board, square, oneForward, moves)

                // Two squares forward from starting rank
                if row == startRow:
                    twoRow = row + direction * 2
                    twoForward = Square::at(twoRow, col)

                    if board.isEmpty(twoForward):
                        MoveGenerator::addMove(board, square, twoForward, moves)


        // Capture left
        captureCol: mut = col - 1

        if captureCol >= 0 and oneRow >= 0 and oneRow < 8:
            captureLeft = Square::at(oneRow, captureCol)

            target = board.getPiece(captureLeft)

            if target.getType() != PieceType.NONE and target.getSide() != piece.getSide():
                MoveGenerator::addMove(board, square, captureLeft, moves)

        // Capture right
        captureCol = col + 1

        if captureCol < 8 and oneRow >= 0 and oneRow < 8:
            captureRight = Square::at(oneRow, captureCol)

            target = board.getPiece(captureRight)

            if target.getType() != PieceType.NONE and target.getSide() != piece.getSide():
                MoveGenerator::addMove(board, square, captureRight, moves)
            
        // En passant
        if board.hasEnPassantSquare():
            epSquare = board.getEnPassantSquare()
            epRow = Square::row(epSquare)
            epCol = Square::col(epSquare)

            // The target must be diagonally forward from this pawn
            if epRow == oneRow and (epCol == col - 1 or epCol == col + 1):
                victim = board.getPiece(Square::at(row, epCol))

                // Only capture an enemy pawn. This also stops generate() from
                // producing bogus moves if it's called for the side that isn't to move
                if victim.getType() == PieceType.PAWN and victim.getSide() != piece.getSide():
                    MoveGenerator::addEnPassantMove(board, square, epSquare, moves)


    func generateKnightMoves(board: Board, square: Square, moves: List<Move>):
        MoveGenerator::addJump(board, square,  2,  1, moves)
        MoveGenerator::addJump(board, square,  2, -1, moves)
        MoveGenerator::addJump(board, square, -2,  1, moves)
        MoveGenerator::addJump(board, square, -2, -1, moves)

        MoveGenerator::addJump(board, square,  1,  2, moves)
        MoveGenerator::addJump(board, square,  1, -2, moves)
        MoveGenerator::addJump(board, square, -1,  2, moves)
        MoveGenerator::addJump(board, square, -1, -2, moves)


    func generateBishopMoves(board: Board, square: Square, moves: List<Move>):
        MoveGenerator::addRay(board, square,  1,  1, moves)
        MoveGenerator::addRay(board, square,  1, -1, moves)
        MoveGenerator::addRay(board, square, -1,  1, moves)
        MoveGenerator::addRay(board, square, -1, -1, moves)


    func generateRookMoves(board: Board, square: Square, moves: List<Move>):
        MoveGenerator::addRay(board, square,  1,  0, moves)
        MoveGenerator::addRay(board, square, -1,  0, moves)
        MoveGenerator::addRay(board, square,  0,  1, moves)
        MoveGenerator::addRay(board, square,  0, -1, moves)


    func generateQueenMoves(board: Board, square: Square, moves: List<Move>):
        // Rook directions
        MoveGenerator::addRay(board, square,  1,  0, moves)
        MoveGenerator::addRay(board, square, -1,  0, moves)
        MoveGenerator::addRay(board, square,  0,  1, moves)
        MoveGenerator::addRay(board, square,  0, -1, moves)

        // Bishop directions
        MoveGenerator::addRay(board, square,  1,  1, moves)
        MoveGenerator::addRay(board, square,  1, -1, moves)
        MoveGenerator::addRay(board, square, -1,  1, moves)
        MoveGenerator::addRay(board, square, -1, -1, moves)


    func generateKingMoves(board: Board, square: Square, moves: List<Move>):
        MoveGenerator::addJump(board, square,  1,  0, moves)
        MoveGenerator::addJump(board, square, -1,  0, moves)
        MoveGenerator::addJump(board, square,  0,  1, moves)
        MoveGenerator::addJump(board, square,  0, -1, moves)

        MoveGenerator::addJump(board, square,  1,  1, moves)
        MoveGenerator::addJump(board, square,  1, -1, moves)
        MoveGenerator::addJump(board, square, -1,  1, moves)
        MoveGenerator::addJump(board, square, -1, -1, moves)


    // True if a piece of the given type and side stands on (row, col).
    func hasPieceAt(board: Board, row: int, col: int, pieceType: PieceType, side: Side) -> bool:
        if row < 0 or row >= 8 or col < 0 or col >= 8:
            return false

        piece = board.getPiece(Square::at(row, col))
        return piece.getType() == pieceType and piece.getSide() == side


    // Walks a ray from the square and checks whether the first piece it meets
    // is a slider of bySide (the given slider type, or a queen).
    func rayHasAttacker(board: Board, square: Square, rowDelta: int, colDelta: int, bySide: Side, sliderType: PieceType) -> bool:
        startRow = Square::row(square)
        startCol = Square::col(square)

        for step in range(1, 8):
            row = startRow + rowDelta * step
            col = startCol + colDelta * step

            if row < 0 or row >= 8 or col < 0 or col >= 8:
                return false

            piece = board.getPiece(Square::at(row, col))

            if piece.getType() == PieceType.NONE:
                continue

            // The first piece in the way decides it.
            return piece.getSide() == bySide and (piece.getType() == sliderType or piece.getType() == PieceType.QUEEN)

        return false


    // Is the square attacked by any piece of bySide? Works on empty squares too,
    // which move generation can't tell us (pawns only capture onto occupied squares).
    func isSquareAttacked(board: Board, square: Square, bySide: Side) -> bool:
        row = Square::row(square)
        col = Square::col(square)

        // Pawns. White pawns move up the board, so a white pawn attacking this
        // square sits one row below it. Black pawns sit one row above.
        pawnRow: mut = row - 1
        if bySide == Side.BLACK:
            pawnRow = row + 1

        if MoveGenerator::hasPieceAt(board, pawnRow, col - 1, PieceType.PAWN, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, pawnRow, col + 1, PieceType.PAWN, bySide):
            return true

        // Knights
        if MoveGenerator::hasPieceAt(board, row + 2, col + 1, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row + 2, col - 1, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 2, col + 1, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 2, col - 1, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row + 1, col + 2, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row + 1, col - 2, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 1, col + 2, PieceType.KNIGHT, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 1, col - 2, PieceType.KNIGHT, bySide):
             return true

        // Enemy king next to the square
        if MoveGenerator::hasPieceAt(board, row + 1, col, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 1, col, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row, col + 1, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row, col - 1, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row + 1, col + 1, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row + 1, col - 1, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 1, col + 1, PieceType.KING, bySide):
            return true
        if MoveGenerator::hasPieceAt(board, row - 1, col - 1, PieceType.KING, bySide):
             return true

        // Rooks and queens along ranks and files
        if MoveGenerator::rayHasAttacker(board, square,  1,  0, bySide, PieceType.ROOK):
            return true
        if MoveGenerator::rayHasAttacker(board, square, -1,  0, bySide, PieceType.ROOK):
            return true
        if MoveGenerator::rayHasAttacker(board, square,  0,  1, bySide, PieceType.ROOK):
            return true
        if MoveGenerator::rayHasAttacker(board, square,  0, -1, bySide, PieceType.ROOK):
            return true

        // Bishops and queens along diagonals
        if MoveGenerator::rayHasAttacker(board, square,  1,  1, bySide, PieceType.BISHOP):
            return true
        if MoveGenerator::rayHasAttacker(board, square,  1, -1, bySide, PieceType.BISHOP):
            return true
        if MoveGenerator::rayHasAttacker(board, square, -1,  1, bySide, PieceType.BISHOP):
            return true
        if MoveGenerator::rayHasAttacker(board, square, -1, -1, bySide, PieceType.BISHOP):
            return true

        return false


    // Castling is encoded as a two-square king move (e -> g or e -> c).
    // Board::makeMove moves the rook.
    func generateCastlingMoves(board: Board, square: Square, moves: List<Move>):
        side = board.getPiece(square).getSide()
        enemy = Side::opposite(side)

        homeRow: mut = 0
        if side == Side.BLACK:
            homeRow = 7

        // The king must still be on its starting square.
        if square != Square::at(homeRow, 4):
            return

        // Can't castle out of check.
        if MoveGenerator::isSquareAttacked(board, square, enemy):
            return

        // Kingside: f and g must be empty, and the king can't cross or land on an attacked square.
        if board.canCastleKingSide(side):
            f = Square::at(homeRow, 5)
            g = Square::at(homeRow, 6)

            pathClear = board.isEmpty(f) and board.isEmpty(g)
            pathSafe = not MoveGenerator::isSquareAttacked(board, f, enemy) and not MoveGenerator::isSquareAttacked(board, g, enemy)

            if pathClear and pathSafe:
                MoveGenerator::addMove(board, square, g, moves)

        // Queenside: b, c and d must be empty, but only c and d must be safe (the king doesn't cross b).
        if board.canCastleQueenSide(side):
            b = Square::at(homeRow, 1)
            c = Square::at(homeRow, 2)
            d = Square::at(homeRow, 3)

            pathClear = board.isEmpty(b) and board.isEmpty(c) and board.isEmpty(d)
            pathSafe = not MoveGenerator::isSquareAttacked(board, c, enemy) and not MoveGenerator::isSquareAttacked(board, d, enemy)

            if pathClear and pathSafe:
                MoveGenerator::addMove(board, square, c, moves)