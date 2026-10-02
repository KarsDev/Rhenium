using square in self
using piece in self

struct Move inherits Writeable:
    from: Square
    to: Square

    movedPiece: Piece
    capturedPiece: Piece

    // PieceType.NONE unless this move promotes a pawn
    promotion: PieceType

impl Move:
    func toString() -> str:
        s: mut = "from " + Square::name(this.from) + " to " + Square::name(this.to)

        if this.promotion != PieceType.NONE:
            s = s + " promoting to " + strToUpper(this.promotion)

        return s