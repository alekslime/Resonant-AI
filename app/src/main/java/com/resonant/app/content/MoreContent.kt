package com.resonant.app.content

/**
 * Six more built-in lessons and eight more quiz sets. They are written with two small helpers so each
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

    val all: List<Lesson> = listOf(
        waterCycleLesson, fractionsLesson, sortingLesson, solarSystemLesson, soundLesson, heartLesson
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

    val allQuizzes: List<QuizSet> = listOf(
        binarySearchQuiz, plateTectonicsQuiz, waterCycleQuiz, fractionsQuiz,
        sortingQuiz, solarSystemQuiz, soundQuiz, heartQuiz
    )
}
