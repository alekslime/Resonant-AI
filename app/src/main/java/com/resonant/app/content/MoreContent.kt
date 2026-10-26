package com.resonant.app.content

/**
 * Twelve more built-in lessons and fourteen more quiz sets, in two batches. They are written with two small helpers so each
 * section is one line of metadata plus its sentences. Everything is read aloud, so: short sentences,
 * no symbols, no digits that read badly.
 *
 * Section keywords follow the rule in [LessonSection]: extra words a person might use when asking,
 * and never question words (what, how, why, when, where, who, which).
 */
object MoreContent {

    private fun section(id: String, title: String, keywords: List<String>, vararg sentences: String) =
        LessonSection(
            id = id,
            title = title,
            units = sentences.mapIndexed { i, text -> SemanticUnit("${id}u${i + 1}", text) },
            keywords = keywords
        )

    private val takeawayWords =
        listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")

    private val introWords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner")

    /** One question. The correct answer is at [correct] (0 for A, 1 for B...). */
    private fun q(id: String, prompt: String, correct: Int, explanation: String, vararg options: String) = QuizQuestion(
        id = id,
        prompt = SemanticUnit("${id}_prompt", prompt),
        options = options.mapIndexed { i, text -> QuizOption('A' + i, text) },
        correctIndex = correct,
        explanation = explanation
    )

    // ------------------------------------------------------------------ lessons

    val waterCycleLesson = Lesson(
        id = "lesson_water_cycle",
        title = "Introduction to the Water Cycle",
        sections = listOf(
            section("s1", "What is the water cycle?", introWords,
                "The water cycle is the endless journey of water around the Earth.",
                "Water moves between the oceans, the air, and the land, changing between liquid, gas, and ice along the way.",
                "It is powered mostly by the heat of the sun."),
            section("s2", "Evaporation",
                listOf("evaporate", "evaporates", "heat", "vapor", "vapour", "rises", "sun", "transpiration", "ocean"),
                "Heat from the sun warms water in oceans, lakes, and rivers.",
                "Some of the water turns into an invisible gas called water vapor and rises into the air.",
                "Plants add to this too, by releasing water vapor from their leaves. That is called transpiration."),
            section("s3", "Condensation and clouds",
                listOf("condense", "condenses", "cloud", "clouds", "cool", "cooling", "cold", "droplet", "droplets", "fog"),
                "As water vapor rises, the air gets colder.",
                "Cold air cannot hold as much vapor, so the vapor turns back into tiny drops of liquid water.",
                "Billions of these tiny drops gathered together make a cloud."),
            section("s4", "Rain, snow, and collection",
                listOf("rain", "snow", "hail", "sleet", "precipitation", "fall", "falls", "runoff", "groundwater", "river", "rivers", "collection"),
                "When cloud droplets grow heavy enough, they fall as rain, snow, sleet, or hail. This is called precipitation.",
                "Some of the water runs over the ground into streams and rivers, and flows back to the sea.",
                "Some soaks into the soil and becomes groundwater, which can stay underground for a very long time."),
            section("s5", "Key takeaway", takeawayWords,
                "Water evaporates, condenses into clouds, falls as precipitation, and collects again.",
                "Then the cycle starts over, powered by the sun.",
                "No water is made or lost. The same water is used again and again.")
        )
    )

    val fractionsLesson = Lesson(
        id = "lesson_fractions",
        title = "Introduction to Fractions",
        sections = listOf(
            section("s1", "What is a fraction?", introWords,
                "A fraction describes a part of a whole.",
                "It is written with two numbers. The top number is the numerator, and the bottom number is the denominator.",
                "The denominator says how many equal parts the whole is cut into. The numerator says how many of those parts you have."),
            section("s2", "Equivalent fractions",
                listOf("equivalent", "equal", "same", "simplify", "simplifying", "simplest", "reduce", "reducing"),
                "Different fractions can describe the same amount. One half, two quarters, and four eighths are all equal.",
                "You make an equivalent fraction by multiplying or dividing the top and bottom by the same number.",
                "Dividing both by the largest number that fits is called simplifying the fraction."),
            section("s3", "Comparing fractions",
                listOf("compare", "comparing", "bigger", "larger", "smaller", "greater", "less", "order", "biggest"),
                "When the denominators are the same, the fraction with the larger numerator is larger.",
                "When the numerators are the same, the fraction with the smaller denominator is larger, because its parts are bigger.",
                "Otherwise, change the fractions so they share a denominator, then compare the numerators."),
            section("s4", "Adding fractions",
                listOf("add", "adding", "addition", "plus", "sum", "total", "combine", "common"),
                "To add fractions with the same denominator, add the numerators and keep the denominator.",
                "One quarter plus two quarters is three quarters.",
                "If the denominators differ, first rewrite both fractions with a common denominator, then add."),
            section("s5", "Key takeaway", takeawayWords,
                "A fraction is a part of a whole, written as a numerator over a denominator.",
                "Equivalent fractions name the same amount in different ways.",
                "Match the denominators before you compare or add.")
        )
    )

    val sortingLesson = Lesson(
        id = "lesson_sorting",
        title = "Introduction to Sorting",
        sections = listOf(
            section("s1", "What is sorting?", introWords,
                "Sorting means putting items in order, such as numbers from smallest to largest, or names from A to Z.",
                "Sorted data is easier to search and easier to read.",
                "A sorting algorithm is a set of steps that can put any list in order."),
            section("s2", "Bubble sort",
                listOf("bubble", "swap", "swaps", "swapping", "neighbor", "neighbors", "neighbour", "adjacent", "pass", "passes"),
                "Bubble sort walks along the list and compares each pair of neighbors.",
                "If a pair is in the wrong order, it swaps them.",
                "After each full pass, the largest unsorted item has bubbled to its place at the end."),
            section("s3", "Selection sort",
                listOf("selection", "select", "selects", "minimum", "smallest", "pick", "picks", "find"),
                "Selection sort looks through the unsorted part of the list for the smallest item.",
                "It moves that item to the front of the unsorted part.",
                "It repeats this until every item has been placed."),
            section("s4", "Speed",
                listOf("fast", "faster", "slow", "slower", "speed", "time", "complexity", "efficient", "efficiency", "large", "long", "merge", "quicksort"),
                "Bubble sort and selection sort are simple, but they are slow on long lists, because the work grows with the square of the length.",
                "Faster methods, like merge sort and quicksort, split the list into smaller pieces and sort each piece.",
                "For short lists, the simple methods are fine."),
            section("s5", "Key takeaway", takeawayWords,
                "Sorting puts items in order.",
                "Bubble sort swaps neighbors, and selection sort picks the smallest item each time.",
                "Both are easy to understand, but faster methods are used for large lists.")
        )
    )

    val solarSystemLesson = Lesson(
        id = "lesson_solar_system",
        title = "Introduction to the Solar System",
        sections = listOf(
            section("s1", "What is the solar system?", introWords,
                "The solar system is the sun and everything that travels around it.",
                "That includes eight planets, their moons, dwarf planets, asteroids, and comets.",
                "The sun's gravity holds it all in orbit."),
            section("s2", "The inner planets",
                listOf("inner", "mercury", "venus", "earth", "mars", "rocky", "terrestrial", "close", "closest"),
                "The four planets closest to the sun are Mercury, Venus, Earth, and Mars.",
                "They are small and rocky, with solid surfaces.",
                "Earth is the only one known to have liquid water on its surface, and life."),
            section("s3", "The outer planets",
                listOf("outer", "jupiter", "saturn", "uranus", "neptune", "giant", "giants", "gas", "ice", "rings", "far", "farthest"),
                "Jupiter, Saturn, Uranus, and Neptune are much larger, and far from the sun.",
                "Jupiter and Saturn are gas giants, made mostly of hydrogen and helium. Uranus and Neptune are ice giants.",
                "All four have rings, though Saturn's are the biggest and brightest."),
            section("s4", "Days, years, and seasons",
                listOf("day", "days", "night", "year", "years", "season", "seasons", "orbit", "rotate", "rotation", "tilt", "spin", "spins"),
                "Earth spins once about every twenty four hours, which gives us day and night.",
                "It travels once around the sun in about three hundred sixty five days. That is one year.",
                "Earth's axis is tilted, and that tilt is the reason we have seasons."),
            section("s5", "Key takeaway", takeawayWords,
                "The solar system is the sun and the things held in orbit around it.",
                "Four small rocky planets are near the sun, and four giant planets are far away.",
                "Earth's spin makes days, its orbit makes years, and its tilt makes seasons.")
        )
    )

    val soundLesson = Lesson(
        id = "lesson_sound",
        title = "Introduction to Sound",
        sections = listOf(
            section("s1", "What is sound?", introWords,
                "Sound is a vibration that travels through matter as a wave.",
                "When something vibrates, it pushes the air around it back and forth, and that motion spreads outward.",
                "Your ears pick up these waves, and your brain turns them into what you hear."),
            section("s2", "Sound needs something to travel through",
                listOf("medium", "vacuum", "space", "air", "water", "solid", "solids", "travel", "travels", "speed"),
                "Sound needs something to travel through, such as air, water, or a solid.",
                "In a vacuum, like outer space, there is nothing to carry the vibration, so there is no sound.",
                "Sound usually travels faster in water than in air, and faster still in solids."),
            section("s3", "Pitch and volume",
                listOf("pitch", "high", "low", "loud", "quiet", "volume", "loudness", "frequency", "amplitude"),
                "Pitch is how high or low a sound is. It depends on frequency, the number of vibrations each second.",
                "Volume is how loud a sound is. It depends on amplitude, which is how big the vibrations are.",
                "A small whistle makes a high pitch, and a big drum makes a low one."),
            section("s4", "Echoes",
                listOf("echo", "echoes", "reflect", "reflects", "reflection", "bounce", "bounces", "wall", "echolocation", "bat", "bats"),
                "An echo is sound that bounces off a surface and comes back to you.",
                "You hear it as a repeat, a little later than the original sound.",
                "Bats and dolphins use echoes to find objects. This is called echolocation, and some people learn to do something similar by clicking."),
            section("s5", "Key takeaway", takeawayWords,
                "Sound is a vibration that travels as a wave through air, water, or solids.",
                "Frequency sets the pitch, and amplitude sets the volume.",
                "Echoes are sound that bounces back, and they can tell you where things are.")
        )
    )

    val heartLesson = Lesson(
        id = "lesson_heart",
        title = "Introduction to the Heart",
        sections = listOf(
            section("s1", "What does the heart do?", introWords + listOf("job", "function", "pump", "pumps"),
                "The heart is a muscle about the size of your fist that pumps blood around your body.",
                "Blood carries oxygen and nutrients to every part of you, and carries waste away.",
                "At rest, it beats roughly sixty to one hundred times a minute, day and night."),
            section("s2", "The four chambers",
                listOf("chamber", "chambers", "atrium", "atria", "ventricle", "ventricles", "four", "valve", "valves", "parts"),
                "The heart has four chambers: two on top called atria, and two below called ventricles.",
                "The atria receive blood coming in, and the ventricles push blood out.",
                "Valves between the chambers open and close so that blood only flows one way."),
            section("s3", "Blood vessels",
                listOf("artery", "arteries", "vein", "veins", "capillary", "capillaries", "vessel", "vessels", "circulation", "lungs", "loop"),
                "Arteries carry blood away from the heart, and veins carry it back.",
                "Capillaries are tiny vessels where oxygen and nutrients pass into the body's tissues.",
                "Blood goes to the lungs to pick up oxygen, returns to the heart, and is then pumped out to the rest of the body."),
            section("s4", "Heartbeat and pulse",
                listOf("pulse", "beat", "beats", "heartbeat", "rate", "wrist", "neck", "exercise", "fast", "faster", "feel"),
                "Each time the heart pumps, a wave of pressure moves along the arteries. That is your pulse.",
                "You can feel it with two fingers on the inside of your wrist, or on the side of your neck.",
                "Your heart beats faster when you exercise, because your muscles need more oxygen."),
            section("s5", "Key takeaway", takeawayWords,
                "The heart is a muscle that pumps blood through four chambers.",
                "Arteries carry blood out, veins bring it back, and capillaries deliver oxygen to the tissues.",
                "Your pulse is the beat of your heart, and you can feel it at your wrist.")
        )
    )

    // ---- second batch

    val percentagesLesson = Lesson(
        id = "lesson_percentages",
        title = "Introduction to Percentages",
        sections = listOf(
            section("s1", "What is a percentage?", introWords,
                "A percentage is a fraction out of one hundred.",
                "The word percent means for each hundred, and it is written with the percent sign.",
                "Fifty percent means fifty out of one hundred, which is the same as one half."),
            section("s2", "Percentages, fractions, and decimals",
                listOf("fraction", "fractions", "decimal", "decimals", "convert", "converting", "change", "equivalent", "point"),
                "To turn a percentage into a decimal, divide it by one hundred. Twenty five percent is zero point two five.",
                "To turn a percentage into a fraction, put it over one hundred and simplify. Twenty five percent is twenty five over one hundred, which is one quarter.",
                "To go the other way, multiply by one hundred."),
            section("s3", "Finding a percentage of a number",
                listOf("find", "calculate", "calculation", "compute", "amount", "ten", "half", "quarter", "multiply"),
                "To find a percentage of a number, turn the percentage into a decimal and multiply.",
                "Ten percent of eighty is zero point one times eighty, which is eight.",
                "A shortcut: ten percent is the number divided by ten, and fifty percent is the number divided by two."),
            section("s4", "Discounts and increases",
                listOf("discount", "discounts", "sale", "price", "prices", "increase", "decrease", "tax", "tip", "save", "cheaper"),
                "A discount takes a percentage off a price. Twenty percent off forty is forty minus eight, which is thirty two.",
                "An increase adds a percentage on top, such as a tip or a tax.",
                "The percentage always applies to the starting amount, so the change is easy to work out."),
            section("s5", "Key takeaway", takeawayWords,
                "A percentage is a number out of one hundred.",
                "Divide by one hundred for a decimal, and multiply by it to find a share of a number.",
                "Discounts and increases are percentages of the starting price.")
        )
    )

    val ancientEgyptLesson = Lesson(
        id = "lesson_ancient_egypt",
        title = "Introduction to Ancient Egypt",
        sections = listOf(
            section("s1", "Who were the ancient Egyptians?", introWords + listOf("people", "civilization", "civilisation"),
                "Ancient Egypt was a civilization that grew up along the river Nile in northeast Africa, more than five thousand years ago.",
                "It lasted for about three thousand years, which is longer than most countries have existed.",
                "Its people built cities, farmed, wrote, and made great monuments."),
            section("s2", "The Nile",
                listOf("nile", "river", "flood", "floods", "farming", "farm", "farmers", "crops", "soil", "desert", "silt"),
                "Almost no rain falls in Egypt, so life depended on the Nile.",
                "Each year the river flooded and left behind rich, dark soil, which was perfect for growing crops.",
                "The river was also the main road, with boats carrying people and goods up and down it."),
            section("s3", "Pharaohs and pyramids",
                listOf("pharaoh", "pharaohs", "king", "ruler", "pyramid", "pyramids", "giza", "tomb", "tombs", "mummy", "mummies", "afterlife"),
                "The ruler of Egypt was called the pharaoh, and people believed he was both a king and a god.",
                "Pharaohs were buried in huge tombs. The Great Pyramid at Giza was built as a tomb about forty five hundred years ago.",
                "Egyptians believed in an afterlife, so they preserved bodies as mummies and filled tombs with things the dead might need."),
            section("s4", "Writing",
                listOf("write", "writing", "hieroglyph", "hieroglyphs", "hieroglyphics", "papyrus", "scribe", "scribes", "rosetta", "stone", "symbols"),
                "Egyptians wrote using pictures and symbols called hieroglyphs.",
                "Scribes wrote on papyrus, a paper made from a river plant, and carved hieroglyphs into stone.",
                "For centuries no one could read them, until the Rosetta Stone, which had the same text in three scripts, helped scholars decode them."),
            section("s5", "Key takeaway", takeawayWords,
                "Ancient Egypt grew along the Nile, which gave it water, soil, and a way to travel.",
                "Its pharaohs built pyramids as tombs, and its scribes wrote in hieroglyphs.",
                "Many of its buildings and writings survive, so we know a great deal about it today.")
        )
    )

    val grammarLesson = Lesson(
        id = "lesson_parts_of_speech",
        title = "Introduction to Parts of Speech",
        sections = listOf(
            section("s1", "What are parts of speech?", introWords + listOf("categories", "kinds", "types"),
                "Parts of speech are the groups that words belong to, based on the job they do in a sentence.",
                "The main ones are nouns, verbs, adjectives, adverbs, pronouns, prepositions, and conjunctions.",
                "Knowing them helps you build clear sentences and understand tricky ones."),
            section("s2", "Nouns and pronouns",
                listOf("noun", "nouns", "pronoun", "pronouns", "person", "place", "thing", "name", "names", "she", "they", "replace"),
                "A noun names a person, a place, a thing, or an idea. Teacher, Cairo, table, and freedom are all nouns.",
                "A pronoun takes the place of a noun, so you do not have to repeat it. She, they, and it are pronouns.",
                "Instead of saying Maria likes Maria's book, you say Maria likes her book."),
            section("s3", "Verbs",
                listOf("verb", "verbs", "action", "actions", "doing", "tense", "past", "present", "future", "state"),
                "A verb shows an action or a state. Run, think, and be are verbs.",
                "Verbs change form to show time. Walk, walked, and will walk are the present, the past, and the future.",
                "Every complete sentence needs at least one verb."),
            section("s4", "Adjectives and adverbs",
                listOf("adjective", "adjectives", "adverb", "adverbs", "describe", "describes", "describing", "modify", "quickly", "ly"),
                "An adjective describes a noun. In the sentence a tall tree, tall is the adjective.",
                "An adverb describes a verb, an adjective, or another adverb. It often tells how, when, or where. In she ran quickly, quickly is the adverb.",
                "Many adverbs end in l y, but not all of them do."),
            section("s5", "Key takeaway", takeawayWords,
                "Words are grouped by the job they do.",
                "Nouns name things, pronouns replace them, and verbs show action.",
                "Adjectives describe nouns, and adverbs describe how, when, or where something happens.")
        )
    )

    val continentsLesson = Lesson(
        id = "lesson_continents_oceans",
        title = "Introduction to Continents and Oceans",
        sections = listOf(
            section("s1", "What are continents and oceans?", introWords,
                "A continent is one of Earth's seven large areas of land. An ocean is one of its large areas of salt water.",
                "About seventy percent of the planet's surface is covered by water, and about thirty percent is land.",
                "The oceans are all connected, so you could sail from any one to any other."),
            section("s2", "The seven continents",
                listOf("continent", "continents", "africa", "asia", "europe", "antarctica", "australia", "oceania", "north", "south", "america", "seven", "list", "names"),
                "The seven continents are Africa, Antarctica, Asia, Australia, Europe, North America, and South America.",
                "Asia is the largest by area and by population. Australia is the smallest.",
                "Antarctica is almost entirely covered in ice, and no one lives there permanently."),
            section("s3", "The five oceans",
                listOf("ocean", "oceans", "pacific", "atlantic", "indian", "arctic", "southern", "five", "sea", "seas", "salt", "deepest"),
                "The five oceans are the Pacific, the Atlantic, the Indian, the Southern, and the Arctic.",
                "The Pacific is the largest and the deepest. It is bigger than all of the land on Earth put together.",
                "The Arctic is the smallest, and much of it is covered with floating ice."),
            section("s4", "Hemispheres and the equator",
                listOf("equator", "hemisphere", "hemispheres", "northern", "southern", "line", "latitude", "longitude", "prime", "meridian", "hot", "tropics"),
                "The equator is an imaginary line around the middle of the Earth. It divides the planet into the northern and southern hemispheres.",
                "Places near the equator are usually hot all year, and places near the poles are cold.",
                "The prime meridian is another imaginary line, running from the north pole to the south pole, and it divides the east from the west."),
            section("s5", "Key takeaway", takeawayWords,
                "Earth has seven continents and five oceans.",
                "Most of the surface is water, and the Pacific is the biggest ocean.",
                "The equator splits the planet into north and south, and the prime meridian into east and west.")
        )
    )

    val newtonLesson = Lesson(
        id = "lesson_newton",
        title = "Introduction to Newton's Laws of Motion",
        sections = listOf(
            section("s1", "What are Newton's laws?", introWords + listOf("laws", "newton", "isaac", "force", "forces"),
                "Newton's laws of motion are three rules that explain how forces make things move, or stop them from moving.",
                "A force is a push or a pull. It is measured in units called newtons, named after Isaac Newton.",
                "They describe everything from a rolling ball to a rocket."),
            section("s2", "The first law: inertia",
                listOf("first", "inertia", "rest", "stay", "keep", "moving", "balanced", "seatbelt", "lazy", "one"),
                "The first law says that an object at rest stays at rest, and an object moving stays moving in a straight line, unless a force acts on it.",
                "This tendency to keep doing what it is doing is called inertia.",
                "It is why you lurch forward when a car brakes suddenly, and why seatbelts matter."),
            section("s3", "The second law: force and mass",
                listOf("second", "mass", "heavy", "heavier", "acceleration", "accelerate", "faster", "push", "pushing", "two"),
                "The second law says that a bigger force causes a bigger acceleration, and a bigger mass needs more force for the same acceleration.",
                "Written as an equation, force equals mass times acceleration.",
                "That is why it is harder to push a full shopping cart than an empty one."),
            section("s4", "The third law: action and reaction",
                listOf("third", "action", "reaction", "equal", "opposite", "pair", "pairs", "rocket", "recoil", "three"),
                "The third law says that for every action there is an equal and opposite reaction.",
                "When you push on a wall, the wall pushes back on you with the same force.",
                "A rocket works this way. It pushes hot gas down, and the gas pushes the rocket up."),
            section("s5", "Key takeaway", takeawayWords,
                "Things keep doing what they are doing until a force changes it.",
                "A bigger force or a smaller mass means more acceleration.",
                "Forces always come in pairs: every push has an equal push back.")
        )
    )

    val digestionLesson = Lesson(
        id = "lesson_digestion",
        title = "Introduction to Digestion",
        sections = listOf(
            section("s1", "What is digestion?", introWords,
                "Digestion is how your body breaks food down into tiny pieces it can use.",
                "Those pieces pass into your blood, which carries them to the cells that need energy and building materials.",
                "Whatever the body cannot use is removed as waste."),
            section("s2", "The mouth and the stomach",
                listOf("mouth", "teeth", "chew", "chewing", "saliva", "swallow", "stomach", "acid", "churn", "churns", "enzyme", "enzymes"),
                "Digestion starts in the mouth. Teeth chew food into small pieces, and saliva begins to break it down.",
                "When you swallow, food travels down a tube called the esophagus to the stomach.",
                "The stomach churns the food with acid and enzymes until it becomes a thick liquid."),
            section("s3", "The small intestine",
                listOf("small", "intestine", "intestines", "absorb", "absorbs", "absorption", "nutrients", "villi", "long", "bile", "liver"),
                "The small intestine is a long, coiled tube where most digestion finishes.",
                "Its walls are covered in tiny folds called villi, which absorb nutrients into the blood.",
                "The liver and pancreas add juices that help break down fats, proteins, and sugars."),
            section("s4", "The large intestine",
                listOf("large", "intestine", "colon", "water", "waste", "fiber", "fibre", "bacteria", "rectum", "leftover"),
                "What is left goes into the large intestine, which soaks up water.",
                "Helpful bacteria live here and make some vitamins.",
                "The remaining waste is stored, then leaves the body."),
            section("s5", "Key takeaway", takeawayWords,
                "Food is broken down in the mouth, stomach, and small intestine.",
                "Nutrients pass into the blood in the small intestine, and the large intestine takes back water.",
                "The whole journey takes about one to two days.")
        )
    )

    val all: List<Lesson> = listOf(
        waterCycleLesson, fractionsLesson, sortingLesson, solarSystemLesson, soundLesson, heartLesson,
        percentagesLesson, ancientEgyptLesson, grammarLesson, continentsLesson, newtonLesson, digestionLesson
    )

    // ------------------------------------------------------------------ quizzes

    val binarySearchQuiz = QuizSet(
        id = "quiz_binary_search",
        title = "Binary search",
        category = "Computer science",
        questions = listOf(
            q("bs_q1", "What must be true of a list before you can use binary search on it?", 1,
                "Binary search decides which half to keep by comparing with the middle, so the list has to be in order.",
                "It must contain only numbers", "It must be sorted", "It must be short", "It must have an even length"),
            q("bs_q2", "Where does binary search look first?", 2,
                "It starts at the middle item, then discards the half that cannot contain the target.",
                "The first item", "The last item", "The middle item", "A random item"),
            q("bs_q3", "Your target is smaller than the middle item. What do you do next?", 0,
                "A smaller target can only be in the left half, so the right half is thrown away.",
                "Search the left half", "Search the right half", "Start again from the end", "Stop. It is not there"),
            q("bs_q4", "Why is binary search faster than checking every item?", 3,
                "Each step removes half of what is left, so even a huge list takes only a few steps.",
                "It skips the sorted items", "It uses a bigger computer", "It never checks the middle", "It cuts the search area in half each step")
        )
    )

    val plateTectonicsQuiz = QuizSet(
        id = "quiz_plate_tectonics",
        title = "Plate tectonics",
        category = "Earth science",
        questions = listOf(
            q("pt_q1", "What are tectonic plates?", 2,
                "Earth's outer shell is broken into large pieces that carry the continents and the ocean floors.",
                "Layers of the atmosphere", "Pieces of the moon", "Large pieces of Earth's outer shell", "Areas of deep ocean"),
            q("pt_q2", "Roughly how fast do plates move?", 1,
                "A few centimeters a year, about as fast as fingernails grow.",
                "A few meters a day", "A few centimeters a year", "A few kilometers an hour", "They do not move"),
            q("pt_q3", "What do we call a boundary where two plates pull apart?", 0,
                "At a divergent boundary, new crust forms in the gap.",
                "Divergent", "Convergent", "Transform", "Central"),
            q("pt_q4", "What is happening at the Himalayas?", 3,
                "The plate carrying India is pushing into the plate carrying Asia, and the land is still rising.",
                "Two plates are pulling apart", "A volcano is erupting", "Plates are sliding sideways", "Two plates are colliding"),
            q("pt_q5", "Where do most earthquakes and volcanoes happen?", 1,
                "Along the boundaries, where plates pull apart, collide, or slide past one another.",
                "In the middle of plates", "Along plate boundaries", "Only near the equator", "Only under the sea")
        )
    )

    val waterCycleQuiz = QuizSet(
        id = "quiz_water_cycle",
        title = "The water cycle",
        category = "Earth science",
        questions = listOf(
            q("wc_q1", "What mostly powers the water cycle?", 0,
                "The sun's heat evaporates water and keeps it moving.",
                "The heat of the sun", "The wind", "The moon", "Underground rivers"),
            q("wc_q2", "What is it called when liquid water turns into water vapor?", 2,
                "Evaporation. Water vapor is an invisible gas.",
                "Condensation", "Precipitation", "Evaporation", "Collection"),
            q("wc_q3", "How do clouds form?", 1,
                "Rising vapor cools, turns back into tiny drops, and billions of drops together make a cloud.",
                "Dust clumps together", "Water vapor cools into tiny droplets", "Rain rises from the ground", "Wind makes ice"),
            q("wc_q4", "Which of these is precipitation?", 3,
                "Precipitation is any water that falls from clouds: rain, snow, sleet, or hail.",
                "Fog on the ground", "A river flowing", "Water in the soil", "Snow falling"),
            q("wc_q5", "What is transpiration?", 0,
                "Plants release water vapor from their leaves, which adds moisture to the air.",
                "Plants releasing water vapor", "Rain soaking into soil", "Ice melting", "Clouds moving")
        )
    )

    val fractionsQuiz = QuizSet(
        id = "quiz_fractions",
        title = "Fractions",
        category = "Math",
        questions = listOf(
            q("fr_q1", "In a fraction, what does the denominator tell you?", 3,
                "The denominator is the bottom number. It says how many equal parts the whole is cut into.",
                "How many parts you have", "How big the whole is", "Which fraction is larger", "How many equal parts the whole is cut into"),
            q("fr_q2", "Which fraction is equal to one half?", 1,
                "Two quarters is the same amount as one half. Multiply the top and bottom of one half by two.",
                "One third", "Two quarters", "Three quarters", "Two thirds"),
            q("fr_q3", "Which is larger, three eighths or five eighths?", 2,
                "The denominators are the same, so the larger numerator wins.",
                "Three eighths", "They are equal", "Five eighths", "You cannot tell"),
            q("fr_q4", "What is one quarter plus two quarters?", 0,
                "Same denominator, so add the numerators and keep the denominator. One plus two is three quarters.",
                "Three quarters", "Three eighths", "Two quarters", "One whole"),
            q("fr_q5", "What does simplifying a fraction mean?", 1,
                "You divide the top and bottom by the largest number that fits both, so the fraction has smaller numbers.",
                "Making it bigger", "Dividing the top and bottom by the same number", "Adding one to the top", "Turning it into a whole number")
        )
    )

    val sortingQuiz = QuizSet(
        id = "quiz_sorting",
        title = "Sorting",
        category = "Computer science",
        questions = listOf(
            q("so_q1", "What does a sorting algorithm do?", 2,
                "It is a set of steps that puts the items of a list in order.",
                "Finds the largest item only", "Deletes repeated items", "Puts a list in order", "Counts the items"),
            q("so_q2", "What does bubble sort do with a pair of neighbors?", 0,
                "It compares them and swaps them if they are in the wrong order.",
                "Swaps them if they are in the wrong order", "Deletes the smaller one", "Always swaps them", "Skips them"),
            q("so_q3", "What does selection sort pick each time?", 3,
                "It finds the smallest item in the unsorted part and moves it to the front.",
                "The middle item", "The largest item", "A random item", "The smallest unsorted item"),
            q("so_q4", "Which sorting methods are faster on long lists?", 1,
                "Merge sort and quicksort split the list into smaller pieces and sort each piece.",
                "Bubble sort and selection sort", "Merge sort and quicksort", "None. All are the same speed", "Only sorting by hand")
        )
    )

    val solarSystemQuiz = QuizSet(
        id = "quiz_solar_system",
        title = "The solar system",
        category = "Space",
        questions = listOf(
            q("ss_q1", "How many planets are in the solar system?", 2,
                "Eight: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, and Neptune.",
                "Six", "Seven", "Eight", "Nine"),
            q("ss_q2", "Which planets are small and rocky?", 0,
                "Mercury, Venus, Earth, and Mars are the four inner planets, with solid surfaces.",
                "The four closest to the sun", "The four farthest from the sun", "Only Earth", "Only the ones with rings"),
            q("ss_q3", "What are Jupiter and Saturn mostly made of?", 1,
                "They are gas giants, made mostly of hydrogen and helium.",
                "Rock and metal", "Hydrogen and helium", "Water and ice only", "Sand"),
            q("ss_q4", "What gives Earth day and night?", 3,
                "Earth spins about once every twenty four hours, so each place turns toward and away from the sun.",
                "Its orbit around the sun", "The moon", "Its tilt", "Its spin"),
            q("ss_q5", "What causes the seasons?", 1,
                "Earth's axis is tilted, so different parts get more direct sunlight at different times of the year.",
                "Earth getting closer to and farther from the sun", "Earth's tilted axis", "The phases of the moon", "The sun getting hotter and colder")
        )
    )

    val soundQuiz = QuizSet(
        id = "quiz_sound",
        title = "How sound works",
        category = "Physics",
        questions = listOf(
            q("sd_q1", "What is sound?", 1,
                "Sound is a vibration that travels through matter as a wave.",
                "A kind of light", "A vibration that travels as a wave", "A type of heat", "A smell in the air"),
            q("sd_q2", "Why is there no sound in outer space?", 0,
                "A vacuum has nothing to carry the vibration.",
                "There is nothing to carry the vibration", "It is too cold", "It is too dark", "Sound is too slow there"),
            q("sd_q3", "What decides how high or low a sound is?", 2,
                "Pitch depends on frequency, the number of vibrations each second.",
                "Amplitude", "Distance", "Frequency", "Echo"),
            q("sd_q4", "A bigger vibration makes a sound that is what?", 3,
                "Amplitude is how big the vibration is, and it sets the volume.",
                "Higher", "Lower", "Slower", "Louder"),
            q("sd_q5", "What is an echo?", 0,
                "Sound that bounces off a surface and returns, heard as a repeat a moment later.",
                "Sound that bounces back", "A very low sound", "Sound that cannot be heard", "Sound in water only")
        )
    )

    val heartQuiz = QuizSet(
        id = "quiz_heart",
        title = "The heart",
        category = "Biology",
        questions = listOf(
            q("ht_q1", "How many chambers does the heart have?", 1,
                "Four: two atria on top and two ventricles below.",
                "Two", "Four", "Six", "Eight"),
            q("ht_q2", "What do valves do?", 2,
                "They open and close so that blood only flows one way.",
                "Make the heart beat", "Store blood", "Make blood flow one way only", "Clean the blood"),
            q("ht_q3", "Which vessels carry blood away from the heart?", 0,
                "Arteries carry blood away, and veins carry it back.",
                "Arteries", "Veins", "Capillaries only", "Nerves"),
            q("ht_q4", "Where do oxygen and nutrients pass into the body's tissues?", 3,
                "Capillaries are tiny vessels with very thin walls, so things can pass through them.",
                "The atria", "The valves", "The arteries", "The capillaries"),
            q("ht_q5", "What is your pulse?", 1,
                "Each heartbeat sends a wave of pressure along the arteries, which you can feel at your wrist or neck.",
                "The speed of your breathing", "A wave of pressure from each heartbeat", "The sound of the valves", "Blood moving in veins")
        )
    )

    // ---- second batch of quizzes

    val percentagesQuiz = QuizSet(
        id = "quiz_percentages",
        title = "Percentages",
        category = "Math",
        questions = listOf(
            q("pc_q1", "What does percent mean?", 2,
                "Percent means for each hundred. A percentage is a fraction out of one hundred.",
                "Out of ten", "Out of one thousand", "Out of one hundred", "Out of fifty"),
            q("pc_q2", "What is fifty percent as a fraction?", 0,
                "Fifty out of one hundred simplifies to one half.",
                "One half", "One quarter", "One fifth", "One tenth"),
            q("pc_q3", "What is ten percent of eighty?", 1,
                "Ten percent is the number divided by ten, so eighty divided by ten is eight.",
                "Ten", "Eight", "Eighteen", "Eighty"),
            q("pc_q4", "How do you turn twenty five percent into a decimal?", 3,
                "Divide by one hundred. Twenty five divided by one hundred is zero point two five.",
                "Multiply by one hundred", "Add one hundred", "Subtract it from one", "Divide by one hundred"),
            q("pc_q5", "A price of forty is reduced by twenty percent. What is the new price?", 0,
                "Twenty percent of forty is eight, and forty minus eight is thirty two.",
                "Thirty two", "Twenty", "Thirty eight", "Eight")
        )
    )

    val ancientEgyptQuiz = QuizSet(
        id = "quiz_ancient_egypt",
        title = "Ancient Egypt",
        category = "History",
        questions = listOf(
            q("eg_q1", "Which river was the heart of ancient Egypt?", 1,
                "The Nile gave Egypt water, rich soil, and a way to travel.",
                "The Amazon", "The Nile", "The Tigris", "The Danube"),
            q("eg_q2", "Why was the yearly flood of the Nile important?", 3,
                "It left behind rich, dark soil, which was perfect for growing crops.",
                "It stopped enemies", "It made the desert bigger", "It brought rain", "It left rich soil for farming"),
            q("eg_q3", "What was the ruler of Egypt called?", 0,
                "The pharaoh was seen as both a king and a god.",
                "Pharaoh", "Emperor", "Sultan", "Consul"),
            q("eg_q4", "What was the Great Pyramid at Giza built as?", 2,
                "It was built about forty five hundred years ago as a tomb.",
                "A temple for crops", "A fortress", "A tomb", "A library"),
            q("eg_q5", "What helped scholars finally read hieroglyphs?", 1,
                "The Rosetta Stone had the same text in three scripts, one of which scholars could already read.",
                "A pharaoh's diary", "The Rosetta Stone", "A map of the Nile", "The Great Pyramid")
        )
    )

    val partsOfSpeechQuiz = QuizSet(
        id = "quiz_parts_of_speech",
        title = "Parts of speech",
        category = "English",
        questions = listOf(
            q("ps_q1", "What does a noun name?", 3,
                "A noun names a person, a place, a thing, or an idea.",
                "An action", "A quality only", "A time only", "A person, place, thing, or idea"),
            q("ps_q2", "What does a pronoun do?", 0,
                "It takes the place of a noun, so you do not have to repeat the noun.",
                "Takes the place of a noun", "Shows an action", "Describes a verb", "Joins two sentences"),
            q("ps_q3", "Which word is a verb in: the dog runs home?", 1,
                "Runs shows an action, so it is the verb.",
                "Dog", "Runs", "The", "Home"),
            q("ps_q4", "What does an adjective describe?", 2,
                "An adjective describes a noun, as in a tall tree.",
                "A verb", "A sentence", "A noun", "A pronoun only"),
            q("ps_q5", "Which word is the adverb in: she ran quickly?", 0,
                "Quickly tells how she ran, so it describes the verb. That makes it an adverb.",
                "Quickly", "She", "Ran", "There is no adverb")
        )
    )

    val continentsQuiz = QuizSet(
        id = "quiz_continents_oceans",
        title = "Continents and oceans",
        category = "Geography",
        questions = listOf(
            q("co_q1", "How many continents are there?", 2,
                "Africa, Antarctica, Asia, Australia, Europe, North America, and South America make seven.",
                "Five", "Six", "Seven", "Eight"),
            q("co_q2", "Which is the largest continent?", 1,
                "Asia is the largest by area and by population.",
                "Africa", "Asia", "North America", "Europe"),
            q("co_q3", "Which is the largest ocean?", 3,
                "The Pacific is the largest and the deepest, bigger than all the land on Earth put together.",
                "Atlantic", "Indian", "Arctic", "Pacific"),
            q("co_q4", "What does the equator divide?", 0,
                "The equator is an imaginary line that divides the Earth into the northern and southern hemispheres.",
                "North from south", "East from west", "Land from sea", "Day from night"),
            q("co_q5", "About how much of Earth's surface is covered by water?", 1,
                "About seventy percent is water, and about thirty percent is land.",
                "About half", "About seventy percent", "About ten percent", "About ninety percent")
        )
    )

    val newtonQuiz = QuizSet(
        id = "quiz_newton",
        title = "Newton's laws of motion",
        category = "Physics",
        questions = listOf(
            q("nw_q1", "What is a force?", 2,
                "A force is a push or a pull, and it is measured in newtons.",
                "A kind of speed", "A kind of energy", "A push or a pull", "A weight only"),
            q("nw_q2", "What is inertia?", 0,
                "Inertia is the tendency of an object to keep doing what it is doing, unless a force acts on it.",
                "The tendency to keep doing what it is doing", "A strong push", "Friction with the air", "Falling to the ground"),
            q("nw_q3", "Why is a full shopping cart harder to push than an empty one?", 1,
                "A bigger mass needs a bigger force for the same acceleration. That is the second law.",
                "It has less friction", "It has more mass", "It has less inertia", "The floor is steeper"),
            q("nw_q4", "What does the third law say?", 3,
                "For every action there is an equal and opposite reaction. Forces come in pairs.",
                "Heavy things fall faster", "Objects slow down by themselves", "Force equals speed", "Every action has an equal and opposite reaction"),
            q("nw_q5", "How does a rocket lift off?", 2,
                "It pushes hot gas down, and the gas pushes the rocket up.",
                "It pushes against the air", "It pushes against the ground", "It pushes gas down, and the gas pushes it up", "It is pulled up by the sun")
        )
    )

    val digestionQuiz = QuizSet(
        id = "quiz_digestion",
        title = "Digestion",
        category = "Biology",
        questions = listOf(
            q("dg_q1", "Where does digestion start?", 1,
                "Teeth chew the food and saliva begins breaking it down.",
                "The stomach", "The mouth", "The small intestine", "The liver"),
            q("dg_q2", "What does the stomach do to food?", 0,
                "It churns food with acid and enzymes until it becomes a thick liquid.",
                "Churns it with acid and enzymes", "Absorbs the nutrients", "Soaks up the water", "Stores it for a week"),
            q("dg_q3", "Where are most nutrients absorbed into the blood?", 2,
                "The small intestine has tiny folds called villi that absorb nutrients.",
                "The stomach", "The large intestine", "The small intestine", "The esophagus"),
            q("dg_q4", "What does the large intestine mostly do?", 3,
                "It soaks up water from what is left, and helpful bacteria live there.",
                "Breaks down fat", "Makes saliva", "Makes acid", "Soaks up water from the leftovers"),
            q("dg_q5", "What are villi?", 0,
                "Villi are tiny folds in the walls of the small intestine that absorb nutrients.",
                "Tiny folds that absorb nutrients", "Muscles in the stomach", "Glands in the mouth", "Bacteria in the colon")
        )
    )

    val allQuizzes: List<QuizSet> = listOf(
        binarySearchQuiz, plateTectonicsQuiz, waterCycleQuiz, fractionsQuiz,
        sortingQuiz, solarSystemQuiz, soundQuiz, heartQuiz,
        percentagesQuiz, ancientEgyptQuiz, partsOfSpeechQuiz, continentsQuiz, newtonQuiz, digestionQuiz
    )
}
