package nz.terilliom.agent

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle

class AgentAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        if (root.packageName == packageName) return
        val prefs = getSharedPreferences("agent", MODE_PRIVATE)
        val command = prefs.getString("ui_command", "").orEmpty()
        if (command.isBlank()) return
        prefs.edit().remove("ui_command").apply()
        when {
            command.startsWith("tap:") -> tapMatchingText(command.removePrefix("tap:").trim())
            command.startsWith("type:") -> typeIntoFocusedField(command.removePrefix("type:"))
            command == "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            command == "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            command == "scroll down" -> scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            command == "scroll up" -> scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
    }

    private fun tapMatchingText(text: String) {
        val root = rootInActiveWindow ?: return
        val matches = root.findAccessibilityNodeInfosByText(text)
        val target = matches.firstOrNull { it.isClickable } ?: matches.firstOrNull()
        var node = target
        while (node != null) {
            if (node.isClickable) { node.performAction(AccessibilityNodeInfo.ACTION_CLICK); return }
            node = node.parent
        }
    }

    private fun typeIntoFocusedField(text: String) {
        val root = rootInActiveWindow ?: return
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scroll(action: Int) {
        val root = rootInActiveWindow ?: return
        fun visit(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isScrollable && node.performAction(action)) return true
            for (i in 0 until node.childCount) if (visit(node.getChild(i))) return true
            return false
        }
        visit(root)
    }

    override fun onInterrupt() {}
}