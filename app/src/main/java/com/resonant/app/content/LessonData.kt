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

    val allLessons = listOf(binarySearchLesson, photosynthesisLesson, plateTectonicsLesson)
}
