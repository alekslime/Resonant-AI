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
<<<<<<< HEAD
                // No question words here ("what", "about", "explain"): OfflineAnswers
                // drops them as stopwords, so they'd score nothing and only suggest
                // this section is reachable by a word that can't reach it.
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "introduction", "basics", "beginner")
=======
                keywords = listOf("define", "definition", "meaning", "mean", "overview", "intro", "introduction", "about", "explain", "what")
>>>>>>> d0e7410044c32903b896ded2b2b7293565749e8e
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

    val allLessons = listOf(binarySearchLesson)
}
