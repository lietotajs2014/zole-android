package lv.zole.engine

import kotlinx.serialization.Serializable

@Serializable
enum class Suit(val symbol: String, val isRed: Boolean) {
    CLUBS("♣", false),
    SPADES("♠", false),
    HEARTS("♥", true),
    DIAMONDS("♦", true),
}

/** Ranks with their card points ("acis"). */
@Serializable
enum class Rank(val points: Int, val label: String) {
    SEVEN(0, "7"),
    EIGHT(0, "8"),
    NINE(0, "9"),
    TEN(10, "10"),
    JACK(2, "J"),
    QUEEN(3, "Q"),
    KING(4, "K"),
    ACE(11, "A"),
}

/** The suit a card follows: all queens, jacks and diamonds are trumps. */
enum class EffectiveSuit { TRUMP, CLUBS, SPADES, HEARTS }

@Serializable
data class Card(val suit: Suit, val rank: Rank) {
    val points: Int get() = rank.points
    val isTrump: Boolean get() = rank == Rank.QUEEN || rank == Rank.JACK || suit == Suit.DIAMONDS
    val effectiveSuit: EffectiveSuit
        get() = if (isTrump) EffectiveSuit.TRUMP else when (suit) {
            Suit.CLUBS -> EffectiveSuit.CLUBS
            Suit.SPADES -> EffectiveSuit.SPADES
            Suit.HEARTS -> EffectiveSuit.HEARTS
            Suit.DIAMONDS -> EffectiveSuit.TRUMP
        }

    /** Higher is stronger within the card's effective suit. Trumps: 0..13, others: 0..3. */
    val strength: Int get() = if (isTrump) TRUMP_ORDER.size - 1 - TRUMP_ORDER.indexOf(this) else NON_TRUMP_ORDER.indexOf(rank)

    override fun toString(): String = rank.label + suit.symbol

    companion object {
        /** Trumps from strongest to weakest. */
        val TRUMP_ORDER: List<Card> = listOf(
            Card(Suit.CLUBS, Rank.QUEEN), Card(Suit.SPADES, Rank.QUEEN), Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.QUEEN),
            Card(Suit.CLUBS, Rank.JACK), Card(Suit.SPADES, Rank.JACK), Card(Suit.HEARTS, Rank.JACK), Card(Suit.DIAMONDS, Rank.JACK),
            Card(Suit.DIAMONDS, Rank.ACE), Card(Suit.DIAMONDS, Rank.TEN), Card(Suit.DIAMONDS, Rank.KING),
            Card(Suit.DIAMONDS, Rank.NINE), Card(Suit.DIAMONDS, Rank.EIGHT), Card(Suit.DIAMONDS, Rank.SEVEN),
        )

        /** Non-trump ranks from weakest to strongest. */
        val NON_TRUMP_ORDER: List<Rank> = listOf(Rank.NINE, Rank.KING, Rank.TEN, Rank.ACE)
    }
}

object Deck {
    /** All 26 Zole cards: diamonds 7..A, other suits 9, 10, J, Q, K, A. */
    val all: List<Card> = buildList {
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                if (suit != Suit.DIAMONDS && (rank == Rank.SEVEN || rank == Rank.EIGHT)) continue
                add(Card(suit, rank))
            }
        }
    }

    const val TOTAL_POINTS = 120
}

/** Seat 0 is the human ("Tu"), then clockwise: 1, 2. */
@Serializable
@JvmInline
value class Seat(val index: Int) {
    init { require(index in 0..2) { "Seat must be 0..2" } }
    val next: Seat get() = Seat((index + 1) % 3)
    val prev: Seat get() = Seat((index + 2) % 3)

    companion object {
        val ALL: List<Seat> = listOf(Seat(0), Seat(1), Seat(2))
    }
}

/** Sort order for showing a hand: trumps strongest first, then clubs, spades, hearts. */
val handComparator: Comparator<Card> = compareBy<Card>({ it.effectiveSuit.ordinal }, { -it.strength })

/** Hand order, left to right in the fan: trumps strongest first, then clubs, spades, hearts (each strongest first). */
fun List<Card>.sortedForHand(): List<Card> = sortedWith(handComparator)
