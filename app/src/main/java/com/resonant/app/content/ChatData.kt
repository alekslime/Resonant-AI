package com.resonant.app.content

object ChatData {

    private fun unit(id: String, text: String) = SemanticUnit(id, text)

    val sampleScript = ChatScript(
        title = "Ask Resonant",
        exchanges = listOf(
            ChatExchange(
                userText = "Explain recursion.",
                assistantChunks = listOf(
                    unit("c1u1", "Recursion is a technique where a function calls itself to solve a smaller version of the same problem."),
                    unit("c1u2", "Think of it as solving a problem by reducing it until you reach a simple base case."),
                    unit("c1u3", "The key idea is that every recursive function needs a stopping condition.")
                )
            ),
            ChatExchange(
                userText = "Give me an example.",
                assistantChunks = listOf(
                    unit("c2u1", "A classic example is calculating a factorial."),
                    unit("c2u2", "To find five factorial, you multiply five by four factorial, which multiplies four by three factorial, and so on."),
                    unit("c2u3", "The base case is one factorial, which is simply one, and that's where the calls stop unwinding.")
                )
            ),
            ChatExchange(
                userText = "What happens without a base case?",
                assistantChunks = listOf(
                    unit("c3u1", "Without a base case, the function keeps calling itself indefinitely."),
                    unit("c3u2", "Eventually this exhausts the call stack and the program crashes with a stack overflow error.")
                )
            )
        )
    )
}
