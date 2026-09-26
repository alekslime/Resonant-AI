package com.resonant.app.content

object QuizData {

    val photosynthesisBasics = QuizSet(
        id = "quiz_photosynthesis",
        title = "Photosynthesis basics",
        category = "Biology",
        questions = listOf(
            QuizQuestion(
                id = "ph_q1",
                prompt = SemanticUnit("ph_q1_prompt", "Which three things does photosynthesis need?"),
                options = listOf(
                    QuizOption('A', "Sunlight, water, and carbon dioxide"),
                    QuizOption('B', "Soil, wind, and heat"),
                    QuizOption('C', "Only sunlight"),
                    QuizOption('D', "Oxygen, salt, and shade")
                ),
                correctIndex = 0,
                explanation = "A plant combines sunlight, water, and carbon dioxide to make its own sugar."
            ),
            QuizQuestion(
                id = "ph_q2",
                prompt = SemanticUnit("ph_q2_prompt", "Where in the plant cell does photosynthesis take place?"),
                options = listOf(
                    QuizOption('A', "The nucleus"),
                    QuizOption('B', "The mitochondria"),
                    QuizOption('C', "The chloroplast"),
                    QuizOption('D', "The cell wall")
                ),
                correctIndex = 2,
                explanation = "Chloroplasts contain chlorophyll, the pigment that captures light energy."
            ),
            QuizQuestion(
                id = "ph_q3",
                prompt = SemanticUnit("ph_q3_prompt", "What gas do plants release during photosynthesis?"),
                options = listOf(
                    QuizOption('A', "Carbon dioxide"),
                    QuizOption('B', "Nitrogen"),
                    QuizOption('C', "Hydrogen"),
                    QuizOption('D', "Oxygen")
                ),
                correctIndex = 3,
                explanation = "Oxygen is the byproduct of splitting water molecules during the light reactions."
            ),
            QuizQuestion(
                id = "ph_q4",
                prompt = SemanticUnit("ph_q4_prompt", "What is the green pigment in plants called?"),
                options = listOf(
                    QuizOption('A', "Melanin"),
                    QuizOption('B', "Chlorophyll"),
                    QuizOption('C', "Carotene"),
                    QuizOption('D', "Hemoglobin")
                ),
                correctIndex = 1,
                explanation = "Chlorophyll absorbs red and blue light, reflecting green — which is why leaves look green."
            ),
            QuizQuestion(
                id = "ph_q5",
                prompt = SemanticUnit("ph_q5_prompt", "What is the sugar produced by photosynthesis called?"),
                options = listOf(
                    QuizOption('A', "Fructose"),
                    QuizOption('B', "Starch"),
                    QuizOption('C', "Glucose"),
                    QuizOption('D', "Sucrose")
                ),
                correctIndex = 2,
                explanation = "Plants produce glucose, which they use for energy or convert into starch for storage."
            )
        )
    )

    val cellsAndParts = QuizSet(
        id = "quiz_cells",
        title = "Cells and their parts",
        category = "Biology",
        questions = listOf(
            QuizQuestion(
                id = "ce_q1",
                prompt = SemanticUnit("ce_q1_prompt", "What controls what enters and leaves the cell?"),
                options = listOf(
                    QuizOption('A', "The nucleus"),
                    QuizOption('B', "The cell membrane"),
                    QuizOption('C', "The cytoplasm"),
                    QuizOption('D', "The ribosome")
                ),
                correctIndex = 1,
                explanation = "The cell membrane is selectively permeable, letting some substances through while blocking others."
            ),
            QuizQuestion(
                id = "ce_q2",
                prompt = SemanticUnit("ce_q2_prompt", "Which organelle is known as the powerhouse of the cell?"),
                options = listOf(
                    QuizOption('A', "The nucleus"),
                    QuizOption('B', "The vacuole"),
                    QuizOption('C', "The ribosome"),
                    QuizOption('D', "The mitochondria")
                ),
                correctIndex = 3,
                explanation = "Mitochondria produce ATP through cellular respiration, supplying energy the cell needs."
            ),
            QuizQuestion(
                id = "ce_q3",
                prompt = SemanticUnit("ce_q3_prompt", "What does the nucleus contain?"),
                options = listOf(
                    QuizOption('A', "Chlorophyll"),
                    QuizOption('B', "The cell's DNA"),
                    QuizOption('C', "Digestive enzymes"),
                    QuizOption('D', "Water and salts")
                ),
                correctIndex = 1,
                explanation = "The nucleus stores the cell's genetic instructions in the form of DNA."
            ),
            QuizQuestion(
                id = "ce_q4",
                prompt = SemanticUnit("ce_q4_prompt", "Which structure is found in plant cells but NOT animal cells?"),
                options = listOf(
                    QuizOption('A', "The nucleus"),
                    QuizOption('B', "The mitochondria"),
                    QuizOption('C', "The cell wall"),
                    QuizOption('D', "The ribosome")
                ),
                correctIndex = 2,
                explanation = "Plant cells have a rigid cell wall made of cellulose that gives them structure. Animal cells do not."
            ),
            QuizQuestion(
                id = "ce_q5",
                prompt = SemanticUnit("ce_q5_prompt", "What do ribosomes do?"),
                options = listOf(
                    QuizOption('A', "Store water"),
                    QuizOption('B', "Produce energy"),
                    QuizOption('C', "Build proteins"),
                    QuizOption('D', "Break down waste")
                ),
                correctIndex = 2,
                explanation = "Ribosomes read the cell's genetic instructions and assemble proteins from amino acids."
            ),
            QuizQuestion(
                id = "ce_q6",
                prompt = SemanticUnit("ce_q6_prompt", "What fills most of the space inside a cell?"),
                options = listOf(
                    QuizOption('A', "Blood"),
                    QuizOption('B', "Air"),
                    QuizOption('C', "Cytoplasm"),
                    QuizOption('D', "Chlorophyll")
                ),
                correctIndex = 2,
                explanation = "Cytoplasm is the jelly-like fluid that fills the cell and suspends the organelles."
            )
        )
    )

    val energyInEcosystems = QuizSet(
        id = "quiz_ecosystems",
        title = "Energy in ecosystems",
        category = "Biology",
        questions = listOf(
            QuizQuestion(
                id = "ec_q1",
                prompt = SemanticUnit("ec_q1_prompt", "What do we call organisms that make their own food?"),
                options = listOf(
                    QuizOption('A', "Consumers"),
                    QuizOption('B', "Decomposers"),
                    QuizOption('C', "Producers"),
                    QuizOption('D', "Predators")
                ),
                correctIndex = 2,
                explanation = "Producers, like plants and algae, make their own food through photosynthesis."
            ),
            QuizQuestion(
                id = "ec_q2",
                prompt = SemanticUnit("ec_q2_prompt", "How much energy is typically passed from one trophic level to the next?"),
                options = listOf(
                    QuizOption('A', "100 percent"),
                    QuizOption('B', "50 percent"),
                    QuizOption('C', "10 percent"),
                    QuizOption('D', "1 percent")
                ),
                correctIndex = 2,
                explanation = "The 10 percent rule: about 90 percent of energy is lost as heat at each level."
            ),
            QuizQuestion(
                id = "ec_q3",
                prompt = SemanticUnit("ec_q3_prompt", "What role do decomposers play in an ecosystem?"),
                options = listOf(
                    QuizOption('A', "They hunt prey"),
                    QuizOption('B', "They produce oxygen"),
                    QuizOption('C', "They break down dead matter and return nutrients to the soil"),
                    QuizOption('D', "They store energy for other animals")
                ),
                correctIndex = 2,
                explanation = "Decomposers like fungi and bacteria recycle nutrients by breaking down dead organisms."
            ),
            QuizQuestion(
                id = "ec_q4",
                prompt = SemanticUnit("ec_q4_prompt", "What is a food chain?"),
                options = listOf(
                    QuizOption('A', "A list of what animals eat at a zoo"),
                    QuizOption('B', "A sequence showing how energy moves from one organism to another"),
                    QuizOption('C', "A diagram of all species in a habitat"),
                    QuizOption('D', "A map of where animals hunt")
                ),
                correctIndex = 1,
                explanation = "A food chain traces the path of energy from producers through successive consumers."
            ),
            QuizQuestion(
                id = "ec_q5",
                prompt = SemanticUnit("ec_q5_prompt", "Which organism sits at the base of most food chains?"),
                options = listOf(
                    QuizOption('A', "A herbivore"),
                    QuizOption('B', "A top predator"),
                    QuizOption('C', "A plant or other producer"),
                    QuizOption('D', "A decomposer")
                ),
                correctIndex = 2,
                explanation = "Plants and other producers capture energy from the sun, making them the foundation of the food chain."
            )
        )
    )

    val plantAdaptations = QuizSet(
        id = "quiz_plant_adaptations",
        title = "Plant adaptations",
        category = "Biology",
        questions = listOf(
            QuizQuestion(
                id = "pa_q1",
                prompt = SemanticUnit("pa_q1_prompt", "Why do cacti have thick, waxy stems?"),
                options = listOf(
                    QuizOption('A', "To attract insects"),
                    QuizOption('B', "To store water and reduce water loss"),
                    QuizOption('C', "To absorb more sunlight"),
                    QuizOption('D', "To grow taller than other plants")
                ),
                correctIndex = 1,
                explanation = "The thick stem stores water and the waxy coating slows evaporation — critical in dry deserts."
            ),
            QuizQuestion(
                id = "pa_q2",
                prompt = SemanticUnit("pa_q2_prompt", "What do spines on a cactus do?"),
                options = listOf(
                    QuizOption('A', "Absorb water from rain"),
                    QuizOption('B', "Produce food"),
                    QuizOption('C', "Deter animals from eating the plant"),
                    QuizOption('D', "Release oxygen faster")
                ),
                correctIndex = 2,
                explanation = "Spines are modified leaves that protect the plant from being eaten by thirsty animals."
            ),
            QuizQuestion(
                id = "pa_q3",
                prompt = SemanticUnit("pa_q3_prompt", "How do water lilies stay afloat?"),
                options = listOf(
                    QuizOption('A', "Their leaves are filled with air pockets"),
                    QuizOption('B', "They have no roots"),
                    QuizOption('C', "Their stems are made of wood"),
                    QuizOption('D', "They absorb salt from the water")
                ),
                correctIndex = 0,
                explanation = "Air pockets in the leaves reduce density and keep the leaf buoyant on the water surface."
            ),
            QuizQuestion(
                id = "pa_q4",
                prompt = SemanticUnit("pa_q4_prompt", "Why do some plants in cold climates have dark-colored leaves?"),
                options = listOf(
                    QuizOption('A', "To blend in with the soil"),
                    QuizOption('B', "To absorb more heat from sunlight"),
                    QuizOption('C', "To store more water"),
                    QuizOption('D', "To repel insects")
                ),
                correctIndex = 1,
                explanation = "Darker pigments absorb more solar radiation, helping the plant stay warm in cold environments."
            )
        )
    )

    /** All quiz sets in display order. */
    val allQuizSets = listOf(
        photosynthesisBasics,
        cellsAndParts,
        energyInEcosystems,
        plantAdaptations
    )

    // Kept for backward compatibility with call sites that still reference a
    // single default quiz. Points to the first set.
    val sampleQuiz = photosynthesisBasics
}
