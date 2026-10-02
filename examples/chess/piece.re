enum PieceType:
    PAWN = "p"
    KNIGHT = "n"
    BISHOP = "b"
    ROOK = "r"
    QUEEN = "q"
    KING = "k"
    NONE = "."

enum Side:
    WHITE
    BLACK
    NONE

namespace Side:
    func opposite(side: Side) -> Side:
        if side == Side.WHITE:
            return Side.BLACK
        else if side == Side.BLACK:
            return Side.WHITE
        
        return side // NONE

struct Piece inherits Writeable:
    pieceType: PieceType
    pieceSide: Side

impl Piece:
    func getType() -> PieceType:
        return this.pieceType
    
    func getSide() -> Side:
        return this.pieceSide
    
    func toString() -> str:
        if this.getSide() == Side.WHITE:
            return strToUpper(this.getType())
        
        return this.getType()

namespace Piece:
    func empty() -> Piece:
        return init Piece(PieceType.NONE, Side.NONE)
    