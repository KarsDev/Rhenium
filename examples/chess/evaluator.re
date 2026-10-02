using board in self

namespace Evaluator:
    global PAWN: int = 100
    global KNIGHT: int = 320
    global BISHOP: int = 330
    global ROOK: int = 500
    global QUEEN: int = 900

    global BISHOP_PAIR_BONUS = 35

    global ROOK_OPEN_FILE = 24
    global ROOK_SEMI_OPEN_FILE = 12
    global ROOK_SEVENTH_RANK = 20

    global ISOLATED_PAWN = -14
    global DOUBLED_PAWN = -12

    global PASSED_PAWN_RANK_2 = 10
    global PASSED_PAWN_RANK_3 = 18
    global PASSED_PAWN_RANK_4 = 30
    global PASSED_PAWN_RANK_5 = 50
    global PASSED_PAWN_RANK_6 = 75
    global PASSED_PAWN_RANK_7 = 110

    global TEMPO = 8

    global FILE_A = 0x0101010101010101L
    global FILE_B = 0x0202020202020202L
    global FILE_C = 0x0404040404040404L
    global FILE_D = 0x0808080808080808L
    global FILE_E = 0x1010101010101010L
    global FILE_F = 0x2020202020202020L
    global FILE_G = 0x4040404040404040L
    global FILE_H = 0x8080808080808080L
    
    global FILE_MASKS = [FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H]

    global RANK_ABOVE = init arr -> long(8)
    global RANK_BELOW = init arr -> long(8)

    global CASTLE_KINGSIDE_BONUS = 20
    global CASTLE_QUEENSIDE_BONUS = 0


    global PAWN_PST = [0, 0, 0, 0, 0, 0, 0, 0, 10, 10, 10, -5, -5, 10, 10, 10, 5, 5, 5, 10, 10, 5, 5, 5, 0, 0, 8, 20, 20, 8, 0, 0, 5, 5, 12, 25, 25, 12, 5, 5, 10, 10, 20, 30, 30, 20, 10, 10, 50, 50, 50, 50, 50, 50, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0]
    global KNIGHT_PST = [-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 5, 5, 0, -20, -40, -30, 5, 10, 15, 15, 10, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 10, 15, 15, 10, 0, -30, -40, -20, 0, 0, 0, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50]
    global BISHOP_PST = [-20, -10, -10, -10, -10, -10, -10, -20, -10, 5, 0, 0, 0, 0, 5, -10, -10, 10, 10, 10, 10, 10, 10, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 5, 5, 10, 0, -10, -10, 0, 0, 0, 0, 0, 0, -10, -20, -10, -10, -10, -10, -10, -10, -20]
    global ROOK_PST = [0, 0, 5, 10, 10, 5, 0, 0, 0, 0, 5, 10, 10, 5, 0, 0, 0, 0, 5, 10, 10, 5, 0, 0, 5, 5, 5, 10, 10, 5, 5, 5, 5, 5, 5, 10, 10, 5, 5, 5, 5, 5, 5, 10, 10, 5, 5, 5, 25, 25, 25, 30, 30, 25, 25, 25, 0, 0, 5, 10, 10, 5, 0, 0]
    global QUEEN_PST = [-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20]
