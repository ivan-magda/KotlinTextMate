package dev.textmate.grammar

import dev.textmate.grammar.rule.RuleId

/**
 * A compiled injection rule: a scope selector matcher paired with a compiled rule and priority.
 * Plain class (not data class) — function-typed [matcher] has identity-based equals/hashCode on JVM.
 */
internal class InjectionRule(
    /** Original selector string, for debug logging. */
    val debugSelector: String,
    /** Returns true when the current scope stack matches this injection's target. */
    val matcher: ScopeMatcher,
    /** Priority: -1 = L: (high, wins ties), 0 = default, 1 = R: (low). */
    val priority: Int,
    /** Rule compiled into the host grammar's rule space. */
    val ruleId: RuleId
)
