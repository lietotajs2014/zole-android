package lv.zole.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Contract
import lv.zole.engine.Difficulty
import lv.zole.engine.Outcome
import lv.zole.engine.Rank
import lv.zole.engine.Rules
import lv.zole.engine.ScoringVariant
import lv.zole.engine.Suit

enum class Language { LV, EN }

/** Signed number with a real minus sign, as in the scoring tables. */
fun signedPoints(v: Int): String = if (v > 0) "+$v" else v.toString().replace("-", "−")

/** Every user-visible text. Latvian is the default; English is chosen in Settings. */
interface Strings {
    // Common
    val back: String
    val menu: String
    fun timer(secondsLeft: Int, total: Int): String
    fun cardName(card: Card): String
    /** Seat 0 is the human player. */
    val playerNames: List<String>
    /** Seats whose names take feminine forms (Anna). */
    val feminineSeats: Set<Int> get() = setOf(1)
    fun scoringName(variant: ScoringVariant): String = when (variant) {
        ScoringVariant.CLASSIC -> classic
        ScoringVariant.RONDO -> "Rondo"
    }
    val classic: String

    // Main menu
    val tagline: String
    val play: String
    val continueMatch: String
    fun continueDeal(n: Int): String
    val stats: String
    val rules: String
    val settings: String

    // New game
    val newMatch: String
    val opponents: String
    val difficulties: List<String>
    val ifAllPass: String
    val pools: String
    val poolsHint: String
    val table: String
    fun tableHint(variant: ScoringVariant): String
    val matchLength: String
    fun deals(n: Int): String
    val noLimit: String
    val scoringTable: String
    val start: String

    // Settings
    val appearance: String
    val theme: String
    val themes: List<String>
    val themeHint: String
    val language: String
    val markTrumps: String
    val markTrumpsHint: String
    val fastAnimations: String
    val vibration: String
    val game: String
    val eyeCounter: String
    val gameRules: String
    fun scoringHint(variant: ScoringVariant): String
    val smallZole: String
    val smallZoleHint: String
    val debug: String
    val debugMode: String
    val debugModeHint: String
    val animSpeed: String
    /** Names of the tunable animations, in AnimKind order. */
    val animNames: List<String>
    val reset: String
    val turnTimer: String
    val timerOff: String
    val showBotHands: String
    val showBotHandsHint: String

    // Rules page
    val cardsTitle: String
    val cardsBody: String
    val trumpsTitle: String
    val trumpsBody: String
    val biddingTitle: String
    val biddingBody: String
    val allPassTitle: String
    fun allPassBody(variant: ScoringVariant): String
    val playTitle: String
    val playBody: String
    fun scoringTableTitle(variant: ScoringVariant): String
    val result: String
    val take: String
    val zole: String
    val scoringRows: List<String>
    fun scoringNote(rules: Rules): String

    // Game
    fun dealPools(n: Int, pools: Int): String
    fun dealBidding(n: Int): String
    fun dealTaking(n: Int, name: String): String
    fun dealTrick(n: Int, trick: Int): String
    fun dealOver(n: Int): String
    val thinkingChip: String
    val yourDecision: String
    fun discarding(name: String): String
    fun thinking(name: String): String
    val discardPoints: String
    val discardAndStart: String
    val yourTurn: String
    val allPassed: String
    val youLost: String
    fun lost(name: String): String
    val youWon: String
    fun won(name: String): String
    val defendersWon: String
    val pool: String
    fun poolAdded(total: Int): String
    val tricksLabel: String
    val eyesLabel: String
    fun tricksOf(name: String, n: Int): String
    val matchOver: String
    val nextDeal: String
    val scoreboard: String
    val defenders: String
    fun bid(choice: BidChoice): String
    /** Status line under an opponent: their role in the deal. */
    fun role(isDeclarer: Boolean, feminine: Boolean): String
    val tableRole: String
    fun cards(n: Int): String
    fun contract(contract: Contract?): String
    fun outcome(outcome: Outcome): String

    // Scoreboard and stats
    val match: String
    fun dealOf(n: Int, limit: Int?): String
    val backToTable: String
    val noDeals: String
    val gameColumn: String
    fun takes(who: String): String
    fun zoleBy(who: String): String
    fun smallZoleBy(who: String): String
    val statDeals: String
    val statDeclarer: String
    fun times(n: Int): String
    val statDeclarerWins: String
    val statZoles: String
    val statAverageEyes: String
    val statStreak: String
    val statMatches: String
    val statMatchesWon: String
    val statTotal: String
    val statsNote: String
}

object LvStrings : Strings {
    override val back = "Atpakaļ"
    override val menu = "Izvēlne"
    override fun timer(secondsLeft: Int, total: Int) = "Atlikušas $secondsLeft sekundes no $total"
    override fun cardName(card: Card): String {
        val r = when (card.rank) {
            Rank.ACE -> "dūzis"; Rank.KING -> "kungs"; Rank.QUEEN -> "dāma"; Rank.JACK -> "kalps"
            else -> card.rank.label
        }
        val s = when (card.suit) { Suit.CLUBS -> "kreicis"; Suit.SPADES -> "pīķis"; Suit.HEARTS -> "ercens"; Suit.DIAMONDS -> "kāravs" }
        return "$s $r"
    }
    override val playerNames = listOf("Tu", "Anna", "Jānis")
    override val classic = "Klasiskā"

    override val tagline = "bet ✨minimalist✨"
    override val play = "Spēlēt"
    override val continueMatch = "Turpināt partiju"
    override fun continueDeal(n: Int) = "· $n. dale"
    override val stats = "Statistika"
    override val rules = "Noteikumi"
    override val settings = "Iestatījumi"

    override val newMatch = "Jauna partija"
    override val opponents = "Pretinieki"
    override val difficulties = listOf("Viegls", "Vidējs", "Grūts")
    override val ifAllPass = "Ja visi saka garām"
    override val pools = "Pules"
    override val poolsHint = "Pieraksta kopējo puli, un dala nākamais"
    override val table = "Galds"
    override fun tableHint(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Katrs par sevi; zaudē tas, kam visvairāk stiķu"
        ScoringVariant.RONDO -> "Katrs par sevi; zaudē tas, kam visvairāk punktu"
    }
    override val matchLength = "Partijas garums"
    override fun deals(n: Int) = "$n dales"
    override val noLimit = "Bez limita"
    override val scoringTable = "Punktu tabula"
    override val start = "Sākt"

    override val appearance = "Izskats"
    override val theme = "Tēma"
    override val themes = listOf("Gaiša", "Melna", "Sistēmas")
    override val themeHint = "Melna (OLED) ir noklusējums. “Sistēmas” seko telefona gaišajam vai tumšajam režīmam."
    override val language = "Valoda"
    override val markTrumps = "Iezīmēt trumpjus"
    override val markTrumpsHint = "Mazs punkts kārts stūrī"
    override val fastAnimations = "Ātrās animācijas"
    override val vibration = "Vibrācija"
    override val game = "Spēle"
    override val eyeCounter = "Punktu skaitītājs uz galda"
    override val gameRules = "Spēles noteikumi"
    override fun scoringHint(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Galds: zaudē tas, kam visvairāk stiķu, un maksā 2 katram."
        ScoringVariant.RONDO -> "Galds: zaudē tas, kam visvairāk punktu. Uzvarētājs saņem 3 bez stiķa, 2 ar līdz 30 punktiem, citādi 1."
    }
    override val smallZole = "Mazā zole"
    override val smallZoleHint = "Nepaņemt nevienu stiķi"
    override val debug = "Atkļūdošana"
    override val debugMode = "Debug režīms"
    override val debugModeHint = "Manuāli iestatījumi testēšanai"
    override val animSpeed = "Animāciju ātrums"
    override val animNames = listOf("Kāršu izdalīšana", "Kārts pacelšana", "Kārts uz galda", "Stiķa rādīšana", "Stiķa savākšana", "Botu domāšana")
    override val reset = "Atiestatīt"
    override val turnTimer = "Gājiena taimeris"
    override val timerOff = "Izsl."
    override val showBotHands = "Rādīt botu kārtis"
    override val showBotHandsHint = "Tu redzi viņu kārtis un talonu; boti tavējās neredz"

    override val cardsTitle = "Kārtis"
    override val cardsBody = "26 kārtis, kopā 120 punkti. Dūzis 11, desmitnieks 10, kungs 4, dāma 3, kalps 2, pārējās 0."
    override val trumpsTitle = "Trumpji"
    override val trumpsBody = "14 trumpji no stiprākā: ♣Q ♠Q ♥Q ♦Q, ♣J ♠J ♥J ♦J, ♦A ♦10 ♦K ♦9 ♦8 ♦7. " +
        "Pārējos mastos (♣ ♠ ♥) ir A, 10, K, 9."
    override val biddingTitle = "Solīšana"
    override val biddingBody = "Katram 8 kārtis, 2 galdā. Sāk pirmā roka. Garām: nodod nākamajam. Ņemu: paņem galda kārtis un noliek 2, " +
        "to punkti skaitās tev. Zole: spēlē bez galda kārtīm, tās skaitās mazajiem. Mazā zole (ja ieslēgta): " +
        "jānepaņem neviens stiķis."
    override val allPassTitle = "Ja visi garām"
    override fun allPassBody(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Galds: katrs par sevi, zaudē tas, kam visvairāk stiķu (vienādi stiķi: vairāk punktu). "
        ScoringVariant.RONDO -> "Galds: katrs par sevi, zaudē tas, kam visvairāk punktu (vienādi punkti: vairāk stiķu). "
    } + "Pules: pieraksta kopējo puli, un dala nākamais."
    override val playTitle = "Izspēle"
    override val playBody = "Jāliek tā paša masta kārts (trumpji ir viens masts). Ja tādas nav, drīkst likt jebko. " +
        "Lielajam vajag vismaz 61 punkts."
    override fun scoringTableTitle(variant: ScoringVariant) = "Punktu tabula · ${scoringName(variant)} · no katra pretinieka"
    override val result = "Rezultāts"
    override val take = "Ņemu"
    override val zole = "Zole"
    override val scoringRows = listOf(
        "Uzvara, 61–90 punkti", "Uzvara, mazie jaņos (91+)", "Uzvara, visi stiķi",
        "Zaudējums, 31–60 punkti", "Zaudējums, jaņos (≤30)", "Zaudējums, bez stiķa",
    )
    override fun scoringNote(rules: Rules): String {
        val table = when (rules.scoring) {
            ScoringVariant.CLASSIC -> "Galds: zaudētājs maksā ${rules.tableLoserPays} katram."
            ScoringVariant.RONDO -> "Galds: katrs uzvarētājs no zaudētāja saņem ${rules.rondoNoTricks} bez stiķa, " +
                "${rules.rondoJani} ar līdz 30 punktiem, citādi ${rules.rondoWin}."
        }
        return "Mazā zole: ${signedPoints(rules.smallZoleWin)} / ${signedPoints(rules.smallZoleLose)}. $table " +
            "Pules: uzvarētājs paņem kopējo puli (+${rules.commonPoolValue} no katra) vai cita personīgo puli (+${rules.personalPoolValue}); " +
            "zaudētājs, kamēr ir pules, saņem personīgo puli."
    }

    override fun dealPools(n: Int, pools: Int) = "Dale $n · " + if (pools == 0) "Pules nav" else "Pules: $pools"
    override fun dealBidding(n: Int) = "Dale $n · Solīšana"
    override fun dealTaking(n: Int, name: String) = "Dale $n · $name ņem"
    override fun dealTrick(n: Int, trick: Int) = "Dale $n · $trick. stiķis no 8"
    override fun dealOver(n: Int) = "Dale $n · beigusies"
    override val thinkingChip = "Domā…"
    override val yourDecision = "Tavs lēmums"
    override fun discarding(name: String) = "$name maina kārtis…"
    override fun thinking(name: String) = "$name domā…"
    override val discardPoints = "Punkti - "
    override val discardAndStart = "Nolikt un sākt"
    override val yourTurn = "Tavs gājiens"
    override val allPassed = "Visi garām"
    override val youLost = "Tu zaudēji"
    override fun lost(name: String) = "$name zaudēja"
    override val youWon = "Tu uzvarēji!"
    override fun won(name: String) = "$name uzvarēja"
    override val defendersWon = "Jūs uzvarējāt!"
    override val pool = "Pule"
    override fun poolAdded(total: Int) = "Pieraksta kopējo puli. Pules: $total"
    override val tricksLabel = "Stiķi: "
    override val eyesLabel = "Punkti: "
    override fun tricksOf(name: String, n: Int) = "$name: $n stiķi"
    override val matchOver = "Partija beigusies"
    override val nextDeal = "Nākamā dale"
    override val scoreboard = "Punktu tabula"
    override val defenders = "Mazie "
    override fun bid(choice: BidChoice) = when (choice) {
        BidChoice.PASS -> "Garām"
        BidChoice.TAKE -> "Ņemu"
        BidChoice.ZOLE -> "Zole"
        BidChoice.SMALL_ZOLE -> "Mazā zole"
    }
    override fun role(isDeclarer: Boolean, feminine: Boolean) = when {
        isDeclarer -> if (feminine) "lielā" else "lielais"
        else -> if (feminine) "mazā" else "mazais"
    }
    override val tableRole = "galds"
    override fun cards(n: Int) = if (n % 10 == 1 && n % 100 != 11) "$n kārts" else "$n kārtis"
    override fun contract(contract: Contract?) = when (contract) {
        Contract.NORMAL -> "Ņemu"
        Contract.ZOLE -> "Zole"
        Contract.SMALL_ZOLE -> "Mazā zole"
        Contract.TABLE -> "Galds"
        null -> "Visi garām"
    }
    override fun outcome(outcome: Outcome) = when (outcome) {
        Outcome.WIN -> "uzvara"
        Outcome.WIN_JANI -> "mazie jaņos"
        Outcome.WIN_ALL_TRICKS -> "visi stiķi"
        Outcome.LOSE -> "zaudējums"
        Outcome.LOSE_JANI -> "jaņos"
        Outcome.LOSE_NO_TRICKS -> "bez stiķa"
        Outcome.SMALL_ZOLE_WIN -> "uzvara"
        Outcome.SMALL_ZOLE_LOSE -> "zaudējums"
        Outcome.TABLE -> "katrs par sevi"
        Outcome.ALL_PASSED -> "pule"
    }

    override val match = "Partija"
    override fun dealOf(n: Int, limit: Int?) = if (limit != null) "$n. dale no $limit" else "$n. dale"
    override val backToTable = "Atpakaļ pie galda"
    override val noDeals = "Vēl nav nevienas nospēlētas dales."
    override val gameColumn = "Spēle"
    override fun takes(who: String) = "$who ņem"
    override fun zoleBy(who: String) = "$who zole"
    override fun smallZoleBy(who: String) = "$who mazā zole"
    override val statDeals = "Nospēlētas dales"
    override val statDeclarer = "Lielais"
    override fun times(n: Int) = "$n reizes"
    override val statDeclarerWins = "Uzvaras kā lielajam"
    override val statZoles = "Zoles"
    override val statAverageEyes = "Vidējie punkti kā lielajam"
    override val statStreak = "Garākā uzvaru sērija"
    override val statMatches = "Pabeigtas partijas"
    override val statMatchesWon = "Uzvarētas partijas"
    override val statTotal = "Punkti kopā"
    override val statsNote = "Statistika skaitās tikai par tevi un krājas no visām partijām."
}

object EnStrings : Strings {
    override val back = "Back"
    override val menu = "Menu"
    override fun timer(secondsLeft: Int, total: Int) = "$secondsLeft of $total seconds left"
    override fun cardName(card: Card): String {
        val r = when (card.rank) {
            Rank.ACE -> "ace"; Rank.KING -> "king"; Rank.QUEEN -> "queen"; Rank.JACK -> "jack"
            else -> card.rank.label
        }
        val s = when (card.suit) { Suit.CLUBS -> "clubs"; Suit.SPADES -> "spades"; Suit.HEARTS -> "hearts"; Suit.DIAMONDS -> "diamonds" }
        return "$r of $s"
    }
    override val playerNames = listOf("You", "Anna", "Jānis")
    override val classic = "Classic"

    override val tagline = "but ✨minimalist✨"
    override val play = "Play"
    override val continueMatch = "Continue match"
    override fun continueDeal(n: Int) = "· deal $n"
    override val stats = "Stats"
    override val rules = "Rules"
    override val settings = "Settings"

    override val newMatch = "New match"
    override val opponents = "Opponents"
    override val difficulties = listOf("Easy", "Medium", "Hard")
    override val ifAllPass = "If everyone passes"
    override val pools = "Pools"
    override val poolsHint = "A common pool is recorded and the next player deals"
    override val table = "Table"
    override fun tableHint(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Every player for themselves; most tricks loses"
        ScoringVariant.RONDO -> "Every player for themselves; most points loses"
    }
    override val matchLength = "Match length"
    override fun deals(n: Int) = "$n deals"
    override val noLimit = "No limit"
    override val scoringTable = "Scoring table"
    override val start = "Start"

    override val appearance = "Appearance"
    override val theme = "Theme"
    override val themes = listOf("Light", "Black", "System")
    override val themeHint = "Black (OLED) is the default. “System” follows the phone's light or dark mode."
    override val language = "Language"
    override val markTrumps = "Mark trumps"
    override val markTrumpsHint = "A small dot in the card corner"
    override val fastAnimations = "Fast animations"
    override val vibration = "Vibration"
    override val game = "Game"
    override val eyeCounter = "Point counter on the table"
    override val gameRules = "Game rules"
    override fun scoringHint(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Table: most tricks loses and pays 2 to each player."
        ScoringVariant.RONDO -> "Table: most points loses. Each winner gets 3 with no tricks, 2 with up to 30 points, otherwise 1."
    }
    override val smallZole = "Small zole"
    override val smallZoleHint = "Take no tricks at all"
    override val debug = "Debugging"
    override val debugMode = "Debug mode"
    override val debugModeHint = "Manual tweaks for testing"
    override val animSpeed = "Animation speed"
    override val animNames = listOf("Dealing cards", "Card lift", "Card to table", "Trick shown", "Trick collected", "Bot thinking")
    override val reset = "Reset"
    override val turnTimer = "Turn timer"
    override val timerOff = "Off"
    override val showBotHands = "Show bot cards"
    override val showBotHandsHint = "You see their hands and the talon; bots still can't see yours"

    override val cardsTitle = "Cards"
    override val cardsBody = "26 cards, 120 points in total. Ace 11, ten 10, king 4, queen 3, jack 2, the rest 0."
    override val trumpsTitle = "Trumps"
    override val trumpsBody = "14 trumps, strongest first: ♣Q ♠Q ♥Q ♦Q, ♣J ♠J ♥J ♦J, ♦A ♦10 ♦K ♦9 ♦8 ♦7. " +
        "The other suits (♣ ♠ ♥) have A, 10, K, 9."
    override val biddingTitle = "Bidding"
    override val biddingBody = "Everyone gets 8 cards, 2 go to the talon. First hand starts. Pass: the next player decides. " +
        "Take: pick up the talon and discard 2; their points count for you. Zole: play without the talon; it counts for the defenders. " +
        "Small zole (if enabled): you must not take a single trick."
    override val allPassTitle = "If everyone passes"
    override fun allPassBody(variant: ScoringVariant) = when (variant) {
        ScoringVariant.CLASSIC -> "Table: every player for themselves; whoever takes the most tricks loses (equal tricks: more points loses). "
        ScoringVariant.RONDO -> "Table: every player for themselves; whoever takes the most points loses (equal points: more tricks loses). "
    } + "Pools: a common pool is recorded and the next player deals."
    override val playTitle = "Play"
    override val playBody = "You must follow suit (all trumps count as one suit). If you can't, play anything. " +
        "The declarer needs at least 61 points. A side with 30 points or fewer is \"schneider\" (jaņos)."
    override fun scoringTableTitle(variant: ScoringVariant) = "Scoring table · ${scoringName(variant)} · per opponent"
    override val result = "Result"
    override val take = "Take"
    override val zole = "Zole"
    override val scoringRows = listOf(
        "Win, 61–90 points", "Win, defenders schneider (91+)", "Win, all tricks",
        "Loss, 31–60 points", "Loss, schneider (≤30)", "Loss, no tricks",
    )
    override fun scoringNote(rules: Rules): String {
        val table = when (rules.scoring) {
            ScoringVariant.CLASSIC -> "Table: the loser pays ${rules.tableLoserPays} to each player."
            ScoringVariant.RONDO -> "Table: each winner gets ${rules.rondoNoTricks} from the loser with no tricks, " +
                "${rules.rondoJani} with up to 30 points, otherwise ${rules.rondoWin}."
        }
        return "Small zole: ${signedPoints(rules.smallZoleWin)} / ${signedPoints(rules.smallZoleLose)}. $table " +
            "Pools: the winner takes a common pool (+${rules.commonPoolValue} from each) or another player's personal pool (+${rules.personalPoolValue}); " +
            "a losing declarer gets a personal pool while pools remain."
    }

    override fun dealPools(n: Int, pools: Int) = "Deal $n · " + if (pools == 0) "No pools" else "Pools: $pools"
    override fun dealBidding(n: Int) = "Deal $n · Bidding"
    override fun dealTaking(n: Int, name: String) = "Deal $n · $name takes"
    override fun dealTrick(n: Int, trick: Int) = "Deal $n · trick $trick of 8"
    override fun dealOver(n: Int) = "Deal $n · finished"
    override val thinkingChip = "Thinking…"
    override val yourDecision = "Your call"
    override fun discarding(name: String) = "$name is discarding…"
    override fun thinking(name: String) = "$name is thinking…"
    override val discardPoints = "Points - "
    override val discardAndStart = "Discard and start"
    override val yourTurn = "Your turn"
    override val allPassed = "Everyone passed"
    override val youLost = "You lost"
    override fun lost(name: String) = "$name lost"
    override val youWon = "You won!"
    override fun won(name: String) = "$name won"
    override val defendersWon = "Your side won!"
    override val pool = "Pool"
    override fun poolAdded(total: Int) = "A common pool is recorded. Pools: $total"
    override val tricksLabel = "Tricks: "
    override val eyesLabel = "Points: "
    override fun tricksOf(name: String, n: Int) = "$name: $n tricks"
    override val matchOver = "Match over"
    override val nextDeal = "Next deal"
    override val scoreboard = "Scoreboard"
    override val defenders = "Defenders "
    override fun bid(choice: BidChoice) = when (choice) {
        BidChoice.PASS -> "Pass"
        BidChoice.TAKE -> "Take"
        BidChoice.ZOLE -> "Zole"
        BidChoice.SMALL_ZOLE -> "Small zole"
    }
    override fun role(isDeclarer: Boolean, feminine: Boolean) = if (isDeclarer) "declarer" else "defender"
    override val tableRole = "table"
    override fun cards(n: Int) = if (n == 1) "1 card" else "$n cards"
    override fun contract(contract: Contract?) = when (contract) {
        Contract.NORMAL -> "Take"
        Contract.ZOLE -> "Zole"
        Contract.SMALL_ZOLE -> "Small zole"
        Contract.TABLE -> "Table"
        null -> "Everyone passed"
    }
    override fun outcome(outcome: Outcome) = when (outcome) {
        Outcome.WIN -> "win"
        Outcome.WIN_JANI -> "defenders schneider"
        Outcome.WIN_ALL_TRICKS -> "all tricks"
        Outcome.LOSE -> "loss"
        Outcome.LOSE_JANI -> "schneider"
        Outcome.LOSE_NO_TRICKS -> "no tricks"
        Outcome.SMALL_ZOLE_WIN -> "win"
        Outcome.SMALL_ZOLE_LOSE -> "loss"
        Outcome.TABLE -> "every player for themselves"
        Outcome.ALL_PASSED -> "pool"
    }

    override val match = "Match"
    override fun dealOf(n: Int, limit: Int?) = if (limit != null) "Deal $n of $limit" else "Deal $n"
    override val backToTable = "Back to the table"
    override val noDeals = "No deals played yet."
    override val gameColumn = "Game"
    override fun takes(who: String) = "$who takes"
    override fun zoleBy(who: String) = "$who zole"
    override fun smallZoleBy(who: String) = "$who small zole"
    override val statDeals = "Deals played"
    override val statDeclarer = "Declarer"
    override fun times(n: Int) = if (n == 1) "1 time" else "$n times"
    override val statDeclarerWins = "Wins as declarer"
    override val statZoles = "Zoles"
    override val statAverageEyes = "Average points as declarer"
    override val statStreak = "Longest win streak"
    override val statMatches = "Matches finished"
    override val statMatchesWon = "Matches won"
    override val statTotal = "Total points"
    override val statsNote = "Statistics count only your results and add up across all matches."
}

fun stringsFor(language: Language): Strings = when (language) {
    Language.LV -> LvStrings
    Language.EN -> EnStrings
}

val LocalStrings = staticCompositionLocalOf<Strings> { LvStrings }

/**
 * Strings for one match: on the hardest difficulty the bots are called "Arvis 1" and "Arvis 2".
 * Wrap a match's screens with this so every name (table, results, score sheet) follows it.
 */
@Composable
fun ProvideMatchStrings(difficulty: Difficulty?, content: @Composable () -> Unit) {
    val base = LocalStrings.current
    val strings = remember(base, difficulty) {
        if (difficulty == Difficulty.HARD) {
            object : Strings by base {
                override val playerNames = listOf(base.playerNames[0], "Arvis 1", "Arvis 2")
                override val feminineSeats = emptySet<Int>()
            }
        } else base
    }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}
