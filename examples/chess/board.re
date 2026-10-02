using collections.list

using piece in self
using square in self
using move in self
using MoveGen in self

type Pieces = arr -> arr -> Piece

// Everything makeMove overwrites that can't be recomputed from the move itself.
// Board::saveState creates one before a move, Board::unmakeMove consumes it.
struct Undo inherits Writeable:
    move: Move
    whiteKingSide: bool
    whiteQueenSide: bool
    blackKingSide: bool
    blackQueenSide: bool
    enPassantSquare: Square

impl Undo:
    func toString() -> str:
        return "undo of " + this.move.toString()

struct Board inherits Writeable:
    pieces: Pieces
    sideToMove: mut Side
    enPassantSquare: mut Square
    whiteKingSide: mut bool
    whiteQueenSide: mut bool
    blackKingSide: mut bool
    blackQueenSide: mut bool
    
impl Board:
    init():
        this.pieces = Board::loadDefaultPieces()
        this.sideToMove = Side.WHITE
        this.enPassantSquare = -1
        this.whiteKingSide = true
        this.whiteQueenSide = true
        this.blackKingSide = true
        this.blackQueenSide = true
    
    func toString() -> str:
        s: mut = ""
        for a in range(8):
            for i in range(8):
                piece = this.pieces[a][i]
                s += piece.toString()
            if a != 7:
                s += "\n"
        return s

    func hasEnPassantSquare() -> bool:
        return this.enPassantSquare >= 0

    func getEnPassantSquare() -> Square:
        return this.enPassantSquare

    func canCastleKingSide(side: Side) -> bool:
        if side == Side.WHITE:
            return this.whiteKingSide
        return this.blackKingSide

    func canCastleQueenSide(side: Side) -> bool:
        if side == Side.WHITE:
            return this.whiteQueenSide
        return this.blackQueenSide

    // A castling right is lost for good once the king or that rook has moved,
    // or the rook has been captured.
    func updateCastlingRights(move: Move, isKing: bool, side: Side) -> none:
        if isKing:
            if side == Side.WHITE:
                this.whiteKingSide = false
                this.whiteQueenSide = false
            else:
                this.blackKingSide = false
                this.blackQueenSide = false

        // A move from or onto a rook's corner means the rook moved or was captured.
        this.clearRightForCorner(move.from)
        this.clearRightForCorner(move.to)

    func clearRightForCorner(square: Square) -> none:
        if square == Square::at(0, 0):
            this.whiteQueenSide = false
        if square == Square::at(0, 7):
            this.whiteKingSide = false
        if square == Square::at(7, 0):
            this.blackQueenSide = false
        if square == Square::at(7, 7):
            this.blackKingSide = false

    func getPiece(square: Square) -> Piece:
        col = Square::col(square)
        row = Square::row(square)
        return this.pieces[7 - row][col]

    func isEmpty(square: Square) -> bool:
        return this.getPiece(square).getType() == PieceType.NONE
    
    func isOccupiedBy(square: Square, side: Side) -> bool:
        return this.getPiece(square).getSide() == side
    
    func makeMove(move: Move) -> none:
        fromCol = Square::col(move.from)
        fromRow = 7 - Square::row(move.from)

        toCol = Square::col(move.to)
        toRow = 7 - Square::row(move.to)

        movedPiece = this.pieces[fromRow][fromCol]

        if movedPiece.pieceSide != this.sideToMove:
            raise "Unexpected move: " + move.toString() + ", side to move: " + this.sideToMove

        movedSide = movedPiece.getSide()
        isPawn = movedPiece.getType() == PieceType.PAWN
        isKing = movedPiece.getType() == PieceType.KING

        if isPawn and fromCol != toCol and this.hasEnPassantSquare() and move.to == this.enPassantSquare:
            this.pieces[fromRow][toCol] = Piece::empty()

        this.pieces[toRow][toCol] = movedPiece
        this.pieces[fromRow][fromCol] = Piece::empty()

        // Promotion: the pawn is replaced by the chosen piece.
        if move.promotion != PieceType.NONE:
            this.pieces[toRow][toCol] = init Piece(move.promotion, movedSide)

        // Castling: the king moves two columns and the rook hops over it.
        // Kingside the rook goes h -> f, queenside a -> d.
        if isKing and toCol - fromCol == 2:
            this.pieces[fromRow][5] = this.pieces[fromRow][7]
            this.pieces[fromRow][7] = Piece::empty()

        if isKing and toCol - fromCol == -2:
            this.pieces[fromRow][3] = this.pieces[fromRow][0]
            this.pieces[fromRow][0] = Piece::empty()

        this.updateCastlingRights(move, isKing, movedSide)

        // The en passant square only lasts one move.
        this.enPassantSquare = -1

        // A pawn moving anything other than one row made a double push.
        if isPawn and fromRow - toRow != 1 and fromRow - toRow != -1:
            row = Square::row(move.from)
            if movedSide == Side.WHITE:
                this.enPassantSquare = Square::at(row + 1, fromCol)
            else:
                this.enPassantSquare = Square::at(row - 1, fromCol)

        this.sideToMove = Side::opposite(this.sideToMove)

    func legalMoves() -> List<Move>:
        return MoveGenerator::generateLegal(this, this.sideToMove, false)

    // Call this BEFORE makeMove(move) to be able to take the move back afterwards.
    func saveState(move: Move) -> Undo:
        return init Undo(move, this.whiteKingSide, this.whiteQueenSide, this.blackKingSide, this.blackQueenSide, this.enPassantSquare)

    // Exactly reverses the makeMove() that followed saveState.
    func unmakeMove(undo: Undo) -> none:
        move = undo.move

        fromCol = Square::col(move.from)
        fromRow = 7 - Square::row(move.from)

        toCol = Square::col(move.to)
        toRow = 7 - Square::row(move.to)

        // makeMove() handed the turn over, so hand it back.
        this.sideToMove = Side::opposite(this.sideToMove)

        isPawn = move.movedPiece.getType() == PieceType.PAWN
        isKing = move.movedPiece.getType() == PieceType.KING

        // The original piece goes home (after a promotion that's the pawn again).
        this.pieces[fromRow][fromCol] = move.movedPiece

        if isPawn and fromCol != toCol and undo.enPassantSquare >= 0 and move.to == undo.enPassantSquare:
            // Destination was empty, and the captured pawn
            // stood next to the origin square.
            this.pieces[toRow][toCol] = Piece::empty()
            this.pieces[fromRow][toCol] = move.capturedPiece
        else:
            this.pieces[toRow][toCol] = move.capturedPiece

        // Castling, rook hops back.
        if isKing and toCol - fromCol == 2:
            this.pieces[fromRow][7] = this.pieces[fromRow][5]
            this.pieces[fromRow][5] = Piece::empty()

        if isKing and toCol - fromCol == -2:
            this.pieces[fromRow][0] = this.pieces[fromRow][3]
            this.pieces[fromRow][3] = Piece::empty()

        this.whiteKingSide = undo.whiteKingSide
        this.whiteQueenSide = undo.whiteQueenSide
        this.blackKingSide = undo.blackKingSide
        this.blackQueenSide = undo.blackQueenSide
        this.enPassantSquare = undo.enPassantSquare

    // Sets up a position from a FEN string.
    func loadFen(fen: str) -> none:
        parts = strSplit(fen, " ")
        rows = strSplit(parts.values[0], "/")

        for r in range(8):
            for c in range(8):
                this.pieces[r][c] = Piece::empty()

        for r in range(8):
            rowText = rows.values[r]
            col: mut = 0

            for i in range(len(rowText)):
                ch = rowText[i]

                if ch >= '1' and ch <= '8':
                    col = col + (ch - '0')
                    continue

                pieceType: mut = PieceType.PAWN
                if ch == 'n' or ch == 'N':
                    pieceType = PieceType.KNIGHT
                else if ch == 'b' or ch == 'B':
                    pieceType = PieceType.BISHOP
                else if ch == 'r' or ch == 'R':
                    pieceType = PieceType.ROOK
                else if ch == 'q' or ch == 'Q':
                    pieceType = PieceType.QUEEN
                else if ch == 'k' or ch == 'K':
                    pieceType = PieceType.KING

                pieceSide: mut = Side.BLACK
                if ch >= 'A' and ch <= 'Z':
                    pieceSide = Side.WHITE

                this.pieces[r][col] = init Piece(pieceType, pieceSide)
                col = col + 1

        if parts.values[1] == "w":
            this.sideToMove = Side.WHITE
        else:
            this.sideToMove = Side.BLACK

        this.whiteKingSide = false
        this.whiteQueenSide = false
        this.blackKingSide = false
        this.blackQueenSide = false

        castleText = parts.values[2]
        for i in range(len(castleText)):
            ch = castleText[i]

            if ch == 'K':
                this.whiteKingSide = true
            else if ch == 'Q':
                this.whiteQueenSide = true
            else if ch == 'k':
                this.blackKingSide = true
            else if ch == 'q':
                this.blackQueenSide = true

        if parts.values[3] == "-":
            this.enPassantSquare = -1
        else:
            this.enPassantSquare = Square::fromName(parts.values[3])

namespace Board:

    func loadDefaultPieces() -> Pieces:
        pieces = init arr -> arr -> Piece(8)

        // Initialize all rows
        for i in range(8):
            pieces[i] = init arr -> Piece(8)

            // Initialize all squares as empty
            for j in range(8):
                pieces[i][j] = Piece::empty()

        // Black back rank
        pieces[0][0] = init Piece(PieceType.ROOK, Side.BLACK)
        pieces[0][1] = init Piece(PieceType.KNIGHT, Side.BLACK)
        pieces[0][2] = init Piece(PieceType.BISHOP, Side.BLACK)
        pieces[0][3] = init Piece(PieceType.QUEEN, Side.BLACK)
        pieces[0][4] = init Piece(PieceType.KING, Side.BLACK)
        pieces[0][5] = init Piece(PieceType.BISHOP, Side.BLACK)
        pieces[0][6] = init Piece(PieceType.KNIGHT, Side.BLACK)
        pieces[0][7] = init Piece(PieceType.ROOK, Side.BLACK)

        // Black pawns
        for i in range(8):
            pieces[1][i] = init Piece(PieceType.PAWN, Side.BLACK)

        // White pawns
        for i in range(8):
            pieces[6][i] = init Piece(PieceType.PAWN, Side.WHITE)

        // White back rank
        pieces[7][0] = init Piece(PieceType.ROOK, Side.WHITE)
        pieces[7][1] = init Piece(PieceType.KNIGHT, Side.WHITE)
        pieces[7][2] = init Piece(PieceType.BISHOP, Side.WHITE)
        pieces[7][3] = init Piece(PieceType.QUEEN, Side.WHITE)
        pieces[7][4] = init Piece(PieceType.KING, Side.WHITE)
        pieces[7][5] = init Piece(PieceType.BISHOP, Side.WHITE)
        pieces[7][6] = init Piece(PieceType.KNIGHT, Side.WHITE)
        pieces[7][7] = init Piece(PieceType.ROOK, Side.WHITE)

        return pieces