package com.resonant.app.content

object LessonData {

    private fun unit(id: String, text: String) = SemanticUnit(id, text)

    val binarySearchLesson = Lesson(
        id = "lesson_binary_search",
        title = "Introduction to Binary Search",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is binary search?",
                units = listOf(
                    unit("s1u1", "Binary search is a method for finding an item in a sorted list very quickly."),
                    unit("s1u2", "Instead of checking every item one by one, it repeatedly cuts the search area in half."),
                    unit("s1u3", "This makes it dramatically faster than a simple linear scan for large lists.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner")
            ),
            LessonSection(
                id = "s2",
                title = "Why sorting matters",
                units = listOf(
                    unit("s2u1", "Binary search only works if the list is already sorted."),
                    unit("s2u2", "Because the algorithm decides which half to search based on order, an unsorted list breaks that logic entirely."),
                    unit("s2u3", "Sorting is the price you pay once, so that every future search can be fast.")
                ),
                keywords = listOf("sorted", "sort", "sorting", "unsorted", "order", "ordered", "require", "requirement", "precondition", "matter", "matters")
            ),
            LessonSection(
                id = "s3",
                title = "The algorithm",
                units = listOf(
                    unit("s3u1", "Start by looking at the middle item of the list."),
                    unit("s3u2", "If it matches what you're looking for, you're done."),
                    unit("s3u3", "If your target is smaller, repeat the search in the left half."),
                    unit("s3u4", "If your target is larger, repeat the search in the right half."),
                    unit("s3u5", "Keep cutting the remaining range in half until you find the item or run out of items.")
                ),
                keywords = listOf("work", "works", "algorithm", "step", "steps", "process", "procedure", "middle", "half", "halve", "implement")
            ),
            LessonSection(
                id = "s4",
                title = "Example",
                units = listOf(
                    unit("s4u1", "Imagine searching for the number seven in the sorted list: one, three, five, seven, nine, eleven."),
                    unit("s4u2", "The middle item is seven. It matches immediately, so the search ends in a single step."),
                    unit("s4u3", "With a linear scan, you might have checked four items before finding it.")
                ),
                keywords = listOf("example", "instance", "demonstrate", "demo", "sample", "illustrate", "show", "seven")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Binary search requires a sorted array."),
                    unit("s5u2", "Each step eliminates half of the remaining possibilities."),
                    unit("s5u3", "That's what makes it so much faster than checking every item one at a time.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val photosynthesisLesson = Lesson(
        id = "lesson_photosynthesis",
        title = "Introduction to Photosynthesis",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is photosynthesis?",
                units = listOf(
                    unit("s1u1", "Photosynthesis is the process plants use to turn sunlight into energy."),
                    unit("s1u2", "They capture light in their leaves and use it to build the sugars they need to grow."),
                    unit("s1u3", "It's the reason plants are green: the pigment that captures light, chlorophyll, gives them their color.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner")
            ),
            LessonSection(
                id = "s2",
                title = "What plants need",
                units = listOf(
                    unit("s2u1", "Photosynthesis needs three ingredients: sunlight, water, and carbon dioxide."),
                    unit("s2u2", "Roots draw water up from the soil, and leaves pull carbon dioxide in from the air."),
                    unit("s2u3", "Without any one of the three, the process stops.")
                ),
                keywords = listOf("need", "needs", "require", "requirement", "requires", "ingredient", "ingredients", "sunlight", "water", "carbon", "dioxide")
            ),
            LessonSection(
                id = "s3",
                title = "The process",
                units = listOf(
                    unit("s3u1", "Chlorophyll in the leaf absorbs light energy from the sun."),
                    unit("s3u2", "That energy splits water molecules apart and powers a chain of reactions."),
                    unit("s3u3", "The plant uses the released energy to combine carbon dioxide and water into glucose, a sugar."),
                    unit("s3u4", "Oxygen is left over from splitting the water, and the plant releases it into the air.")
                ),
                keywords = listOf("work", "works", "process", "step", "steps", "procedure", "chlorophyll", "glucose", "oxygen", "reaction", "implement")
            ),
            LessonSection(
                id = "s4",
                title = "Example",
                units = listOf(
                    unit("s4u1", "Picture a leaf sitting in bright sunlight for a few hours."),
                    unit("s4u2", "It draws in carbon dioxide and pulls water up from the roots."),
                    unit("s4u3", "By the end of the day it has produced sugar to fuel the plant's growth, and released oxygen into the air around it.")
                ),
                keywords = listOf("example", "instance", "demonstrate", "demo", "sample", "illustrate", "show", "leaf")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Photosynthesis turns sunlight, water, and carbon dioxide into sugar and oxygen."),
                    unit("s5u2", "Plants use the sugar as fuel for growth."),
                    unit("s5u3", "The oxygen they release along the way is the same oxygen most living things breathe.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val plateTectonicsLesson = Lesson(
        id = "lesson_plate_tectonics",
        title = "Introduction to Plate Tectonics",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is plate tectonics?",
                units = listOf(
                    unit("s1u1", "Plate tectonics is the idea that Earth's outer shell is broken into large pieces called plates."),
                    unit("s1u2", "These plates carry the continents and ocean floors, and they are always moving, just very slowly."),
                    unit("s1u3", "Most plates drift only a few centimeters a year, about as fast as fingernails grow.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner", "plate", "plates")
            ),
            LessonSection(
                id = "s2",
                title = "Why the plates move",
                units = listOf(
                    unit("s2u1", "Deep inside the Earth, heat drives slow currents in the hot rock of the mantle."),
                    unit("s2u2", "Those currents drag the plates floating above them along, the way a slow current drags a raft."),
                    unit("s2u3", "Over millions of years, that steady drag reshapes where the continents sit.")
                ),
                keywords = listOf("move", "moves", "moving", "cause", "causes", "mantle", "convection", "current", "currents", "heat", "drive", "driver")
            ),
            LessonSection(
                id = "s3",
                title = "The three types of boundaries",
                units = listOf(
                    unit("s3u1", "Where two plates pull apart, new crust forms in the gap; that's a divergent boundary."),
                    unit("s3u2", "Where two plates crash together, one may be pushed under the other, or both may buckle upward; that's a convergent boundary."),
                    unit("s3u3", "Where two plates slide past each other sideways, that's a transform boundary."),
                    unit("s3u4", "Most earthquakes and volcanoes happen right along these boundaries.")
                ),
                keywords = listOf("boundary", "boundaries", "type", "types", "divergent", "convergent", "transform", "earthquake", "volcano", "step", "steps")
            ),
            LessonSection(
                id = "s4",
                title = "Example",
                units = listOf(
                    unit("s4u1", "The Himalayas are still rising today because the plate carrying India is pushing into the plate carrying Asia."),
                    unit("s4u2", "That collision is a convergent boundary, and it has been crumpling the land upward for millions of years."),
                    unit("s4u3", "California's San Andreas Fault, by contrast, is a transform boundary where plates grind past each other sideways.")
                ),
                keywords = listOf("example", "instance", "demonstrate", "demo", "sample", "illustrate", "show", "himalayas")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Earth's crust is broken into plates that drift on slow currents in the mantle."),
                    unit("s5u2", "Where those plates meet, they pull apart, collide, or slide past one another."),
                    unit("s5u3", "That motion, over millions of years, builds mountains and shapes the map of the continents.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val cellsLesson = Lesson(
        id = "lesson_cells",
        title = "Cells and their parts",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is a cell?",
                units = listOf(
                    unit("s1u1", "A cell is the smallest unit of life. Every living thing is made of cells."),
                    unit("s1u2", "Some organisms, like bacteria, consist of just a single cell. Others, like humans, have trillions."),
                    unit("s1u3", "Despite their tiny size, cells carry out all the processes needed to stay alive.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner", "cell")
            ),
            LessonSection(
                id = "s2",
                title = "The cell membrane",
                units = listOf(
                    unit("s2u1", "Every cell is wrapped in a thin, flexible layer called the cell membrane."),
                    unit("s2u2", "The membrane acts like a gatekeeper, letting useful substances in and pushing waste out."),
                    unit("s2u3", "It is selectively permeable, meaning it allows some things through but not others.")
                ),
                keywords = listOf("membrane", "wall", "boundary", "gatekeeper", "permeable", "selectively")
            ),
            LessonSection(
                id = "s3",
                title = "Key organelles",
                units = listOf(
                    unit("s3u1", "The nucleus is the control centre. It holds the cell's DNA and directs everything the cell does."),
                    unit("s3u2", "Mitochondria are the powerhouses. They convert nutrients into the energy the cell uses."),
                    unit("s3u3", "Ribosomes build proteins by reading the instructions stored in DNA."),
                    unit("s3u4", "Plant cells also have chloroplasts, which carry out photosynthesis, and a rigid cell wall for extra support.")
                ),
                keywords = listOf("nucleus", "mitochondria", "ribosome", "chloroplast", "organelle", "organelles", "part", "parts", "structure", "structures")
            ),
            LessonSection(
                id = "s4",
                title = "Example",
                units = listOf(
                    unit("s4u1", "Think of a cell as a tiny factory."),
                    unit("s4u2", "The nucleus is the manager giving orders, the mitochondria are the generators supplying power, and the ribosomes are the workers on the assembly line building products."),
                    unit("s4u3", "The cell membrane is the factory wall, controlling what comes in and goes out.")
                ),
                keywords = listOf("example", "instance", "demonstrate", "demo", "sample", "illustrate", "show", "factory")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Cells are the building blocks of all life."),
                    unit("s5u2", "Each cell has a membrane, a nucleus, and specialised organelles that keep it running."),
                    unit("s5u3", "Plant and animal cells share most parts, but plant cells also have chloroplasts and a cell wall.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val energyEcosystemsLesson = Lesson(
        id = "lesson_ecosystems",
        title = "Energy in ecosystems",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is an ecosystem?",
                units = listOf(
                    unit("s1u1", "An ecosystem is all the living things in an area, together with the non-living environment they depend on."),
                    unit("s1u2", "A forest, a pond, and a coral reef are all ecosystems — each one a web of organisms exchanging energy and matter."),
                    unit("s1u3", "Energy flows through an ecosystem in one direction: from the sun, through plants, and into animals.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner", "ecosystem")
            ),
            LessonSection(
                id = "s2",
                title = "Producers and consumers",
                units = listOf(
                    unit("s2u1", "Producers are organisms that make their own food, almost always through photosynthesis. Plants and algae are producers."),
                    unit("s2u2", "Consumers eat other organisms to get energy. Herbivores eat plants, carnivores eat animals, and omnivores eat both."),
                    unit("s2u3", "Decomposers, like fungi and bacteria, break down dead matter and return nutrients to the soil.")
                ),
                keywords = listOf("producer", "producers", "consumer", "consumers", "herbivore", "carnivore", "omnivore", "decomposer", "decomposers")
            ),
            LessonSection(
                id = "s3",
                title = "Food chains and the 10 percent rule",
                units = listOf(
                    unit("s3u1", "A food chain shows the order in which energy passes from one organism to the next: grass to rabbit to fox, for example."),
                    unit("s3u2", "At each step, roughly 90 percent of the energy is lost as heat. Only about 10 percent passes to the next level."),
                    unit("s3u3", "That is why food chains rarely have more than four or five links — there simply isn't enough energy left beyond that point.")
                ),
                keywords = listOf("food chain", "chain", "trophic", "level", "10 percent", "ten percent", "energy loss", "transfer", "step", "steps")
            ),
            LessonSection(
                id = "s4",
                title = "Example",
                units = listOf(
                    unit("s4u1", "Imagine a meadow. Grasses capture sunlight and store energy in their leaves."),
                    unit("s4u2", "A rabbit eats the grass, getting about 10 percent of that stored energy."),
                    unit("s4u3", "A fox eats the rabbit, getting about 10 percent of the rabbit's energy — just 1 percent of what the grass originally held."),
                    unit("s4u4", "When the fox dies, decomposers break its body down and return nutrients to the soil so the grass can grow again.")
                ),
                keywords = listOf("example", "instance", "demonstrate", "demo", "sample", "illustrate", "show", "meadow", "rabbit", "fox")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Energy enters ecosystems through producers and flows to consumers at each step of a food chain."),
                    unit("s5u2", "About 90 percent is lost as heat at each transfer, so higher levels have far less energy available."),
                    unit("s5u3", "Decomposers close the loop by returning nutrients to the soil.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val plantAdaptationsLesson = Lesson(
        id = "lesson_plant_adaptations",
        title = "Plant adaptations",
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "What is an adaptation?",
                units = listOf(
                    unit("s1u1", "An adaptation is a feature that helps an organism survive in its environment."),
                    unit("s1u2", "Adaptations develop over many generations through natural selection: individuals with helpful traits are more likely to survive and pass those traits on."),
                    unit("s1u3", "Plants have evolved a wide range of adaptations to cope with heat, cold, drought, and limited light.")
                ),
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "basics", "beginner", "adaptation")
            ),
            LessonSection(
                id = "s2",
                title = "Desert adaptations",
                units = listOf(
                    unit("s2u1", "Cacti store water in their thick, fleshy stems and are coated in wax to slow evaporation."),
                    unit("s2u2", "Their leaves have evolved into spines, which reduces water loss and deters animals from eating the plant."),
                    unit("s2u3", "Some desert plants have very deep roots to reach underground water, while others spread shallow roots wide to catch any brief rainfall.")
                ),
                keywords = listOf("desert", "cactus", "cacti", "drought", "water storage", "spines", "wax", "dry", "arid")
            ),
            LessonSection(
                id = "s3",
                title = "Cold climate adaptations",
                units = listOf(
                    unit("s3u1", "Conifers like pine trees have needle-shaped leaves, which lose very little water in cold, dry air."),
                    unit("s3u2", "Their waxy coating also prevents freezing damage, and their downward-sloping branches shed heavy snow before it can break them."),
                    unit("s3u3", "Some tundra plants grow low to the ground to avoid cold winds, and have darker pigments to absorb more heat from the weak sunlight.")
                ),
                keywords = listOf("cold", "conifer", "pine", "needle", "tundra", "freeze", "frost", "snow", "arctic", "alpine")
            ),
            LessonSection(
                id = "s4",
                title = "Aquatic adaptations",
                units = listOf(
                    unit("s4u1", "Water lilies have flat, buoyant leaves filled with air pockets to stay on the surface where sunlight reaches."),
                    unit("s4u2", "Aquatic plants often have flexible stems that bend with currents rather than break."),
                    unit("s4u3", "Many lack the thick waxy coating of land plants because they don't need to prevent water loss.")
                ),
                keywords = listOf("aquatic", "water", "pond", "lake", "water lily", "floating", "buoyant", "river", "stream")
            ),
            LessonSection(
                id = "s5",
                title = "Key takeaway",
                units = listOf(
                    unit("s5u1", "Plant adaptations are features shaped by natural selection to help a plant survive its specific environment."),
                    unit("s5u2", "Desert plants conserve water, cold-climate plants resist freezing, and aquatic plants are built to float and flex."),
                    unit("s5u3", "The same basic needs, water, light, and nutrients, are met in very different ways depending on where a plant lives.")
                ),
                keywords = listOf("summary", "summarize", "summarise", "recap", "takeaway", "remember", "important", "conclusion", "key", "main")
            )
        )
    )

    val allLessons = listOf(
        photosynthesisLesson,
        cellsLesson,
        energyEcosystemsLesson,
        plantAdaptationsLesson,
        binarySearchLesson,
        plateTectonicsLesson
    )
}
