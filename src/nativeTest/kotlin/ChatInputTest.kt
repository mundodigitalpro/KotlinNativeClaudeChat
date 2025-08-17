import kotlin.test.Test
import kotlin.test.assertEquals

class ChatInputTest {

    @Test
    fun emptyInputReturnsBackToMenu() {
        val result = parseChatInput("   ")
        assertEquals(ChatCommand.BACK_TO_MENU, result.command)
        assertEquals(null, result.message)
    }

    @Test
    fun menuAliasesReturnBackToMenu() {
        assertEquals(ChatCommand.BACK_TO_MENU, parseChatInput("/menu").command)
        assertEquals(ChatCommand.BACK_TO_MENU, parseChatInput("/back").command)
        assertEquals(ChatCommand.BACK_TO_MENU, parseChatInput("/MENU").command)
    }

    @Test
    fun exitAliasesReturnExit() {
        assertEquals(ChatCommand.EXIT_APP, parseChatInput("/exit").command)
        assertEquals(ChatCommand.EXIT_APP, parseChatInput("/quit").command)
        assertEquals(ChatCommand.EXIT_APP, parseChatInput("/EXIT").command)
    }

    @Test
    fun helpAliasesReturnHelp() {
        assertEquals(ChatCommand.HELP, parseChatInput("/help").command)
        assertEquals(ChatCommand.HELP, parseChatInput("?").command)
    }

    @Test
    fun normalMessageReturnsContinueWithTrimmedText() {
        val r1 = parseChatInput("Hello")
        assertEquals(ChatCommand.CONTINUE, r1.command)
        assertEquals("Hello", r1.message)

        val r2 = parseChatInput("  hi  ")
        assertEquals(ChatCommand.CONTINUE, r2.command)
        assertEquals("hi", r2.message)
    }
}

