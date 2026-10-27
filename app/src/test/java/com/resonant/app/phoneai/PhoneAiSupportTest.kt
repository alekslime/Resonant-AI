package com.resonant.app.phoneai

import com.resonant.app.network.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAiSupportTest {

    private val g99Cpu = """
        processor	: 0
        BogoMIPS	: 26.00
        Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp
        CPU implementer	: 0x41
    """.trimIndent()

    private val oldCpu = """
        processor	: 0
        Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32
    """.trimIndent()

    private val abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi")

    @Test fun `a 4 GB phone with a dot product CPU is allowed`() {
        assertEquals(PhoneAiSupport.Verdict.Ok, PhoneAiSupport.check(abis, g99Cpu, 3_700_000_000L))
    }

    @Test fun `a 32-bit only phone is refused`() {
        val v = PhoneAiSupport.check(listOf("armeabi-v7a"), g99Cpu, 6_000_000_000L)
        assertTrue(v is PhoneAiSupport.Verdict.No)
    }

    @Test fun `a 3 GB phone is refused`() {
        val v = PhoneAiSupport.check(abis, g99Cpu, 2_700_000_000L)
        assertTrue(v is PhoneAiSupport.Verdict.No)
    }

    @Test fun `a CPU without dot product is refused`() {
        val v = PhoneAiSupport.check(abis, oldCpu, 6_000_000_000L)
        assertTrue(v is PhoneAiSupport.Verdict.No)
    }

    @Test fun `an unreadable cpuinfo or unknown memory does not refuse the phone`() {
        assertEquals(PhoneAiSupport.Verdict.Ok, PhoneAiSupport.check(abis, "", 0L))
    }

    @Test fun `dot product flag must be a whole word`() {
        assertFalse(PhoneAiSupport.hasDotProduct("Features : fp asimd asimddpx"))
        assertTrue(PhoneAiSupport.hasDotProduct("Features\t: fp asimd asimddp"))
    }

    @Test fun `only the emulator address counts as no server`() {
        assertTrue(PhoneAiSupport.noServerConfigured("http://10.0.2.2:11434"))
        assertFalse(PhoneAiSupport.noServerConfigured("http://192.168.1.50:11434"))
    }

    @Test fun `a fresh download starts from zero`() {
        val plan = PhoneAiSupport.resumePlan(200, 0, 1_000)
        assertEquals(PhoneAiSupport.ResumePlan(append = false, totalBytes = 1_000), plan)
    }

    @Test fun `a server that ignores the range restarts the file`() {
        val plan = PhoneAiSupport.resumePlan(200, 400, 1_000)!!
        assertFalse(plan.append)
        assertEquals(1_000L, plan.totalBytes)
    }

    @Test fun `a partial answer is appended and the total adds up`() {
        val plan = PhoneAiSupport.resumePlan(206, 400, 600)!!
        assertTrue(plan.append)
        assertEquals(1_000L, plan.totalBytes)
    }

    @Test fun `other statuses give up`() {
        assertNull(PhoneAiSupport.resumePlan(404, 0, 0))
        assertNull(PhoneAiSupport.resumePlan(416, 400, 0))
        assertNotNull(PhoneAiSupport.resumePlan(206, 0, 10))
    }

    // ---- prompt fitting ----

    private fun msg(role: String, text: String) = ChatMessage(role, text)

    @Test fun `a short chat passes through unchanged`() {
        val h = listOf(msg("system", "Be brief."), msg("user", "Hi"), msg("assistant", "Hello."), msg("user", "Why?"))
        assertEquals(h, PhoneAiPrompt.fit(h))
    }

    @Test fun `a long system prompt is cut at a whole sentence and keeps its start`() {
        val sentence = "The heart pumps blood around the body. "
        val system = "You are a voice assistant. " + sentence.repeat(200)
        val fitted = PhoneAiPrompt.fit(listOf(msg("system", system), msg("user", "Hi")))
        val out = fitted.first().content
        assertTrue(out.length <= PhoneAiPrompt.SYSTEM_CHARS)
        assertTrue(out.startsWith("You are a voice assistant."))
        assertTrue(out.endsWith("."))
    }

    @Test fun `the oldest turns go first and the newest question stays`() {
        val h = buildList {
            add(msg("system", "S"))
            repeat(30) { add(msg("user", "question number $it " + "x".repeat(300))); add(msg("assistant", "answer $it " + "y".repeat(300))) }
            add(msg("user", "the newest question"))
        }
        val fitted = PhoneAiPrompt.fit(h)
        assertEquals("the newest question", fitted.last().content)
        assertEquals("system", fitted.first().role)
        assertEquals("user", fitted[1].role)
        assertTrue(fitted.sumOf { it.content.length } < PhoneAiPrompt.SYSTEM_CHARS + PhoneAiPrompt.HISTORY_CHARS + PhoneAiPrompt.QUESTION_CHARS)
        assertTrue(fitted.any { it.content.startsWith("answer 29") })
        assertFalse(fitted.any { it.content.startsWith("question number 0") })
    }

    @Test fun `history never starts with a reply`() {
        val h = listOf(
            msg("system", "S"),
            msg("user", "a".repeat(350)), msg("assistant", "b".repeat(350)),
            msg("user", "c".repeat(350)), msg("assistant", "d".repeat(350)),
            msg("user", "now")
        )
        val roles = PhoneAiPrompt.fit(h, scale = 0.5).map { it.role }
        assertEquals("system", roles.first())
        assertEquals("user", roles[1])
        assertEquals("user", roles.last())
    }

    @Test fun `a smaller scale gives a shorter prompt`() {
        val h = listOf(msg("system", "Intro. " + "Fact one is true. ".repeat(400)), msg("user", "q"))
        val full = PhoneAiPrompt.fit(h, 1.0).sumOf { it.content.length }
        val half = PhoneAiPrompt.fit(h, 0.5).sumOf { it.content.length }
        assertTrue(half < full)
    }

    @Test fun `fitting is the same every time so the model can reuse what it read`() {
        val h = listOf(msg("system", "Intro. " + "Fact. ".repeat(900)), msg("user", "q1"), msg("assistant", "a1"), msg("user", "q2"))
        assertEquals(PhoneAiPrompt.fit(h), PhoneAiPrompt.fit(h))
        // Same system text, one more turn: the system message must not change.
        val h2 = h + listOf(msg("assistant", "a2"), msg("user", "q3"))
        assertEquals(PhoneAiPrompt.fit(h).first(), PhoneAiPrompt.fit(h2).first())
    }

    @Test fun `cutAtSentence falls back to a space then to a hard cut`() {
        assertEquals("one two", PhoneAiPrompt.cutAtSentence("one two three four", 9))
        assertEquals("abcde", PhoneAiPrompt.cutAtSentence("abcdefghij", 5))
        assertEquals("Hello there.", PhoneAiPrompt.cutAtSentence("Hello there. And more words", 20))
    }
}
